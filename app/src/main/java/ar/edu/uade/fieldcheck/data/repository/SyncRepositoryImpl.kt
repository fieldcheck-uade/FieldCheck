package ar.edu.uade.fieldcheck.data.repository

import ar.edu.uade.fieldcheck.data.local.dao.EvidenceDao
import ar.edu.uade.fieldcheck.data.local.dao.InspectionDao
import ar.edu.uade.fieldcheck.data.local.entity.InspectionWithItems
import ar.edu.uade.fieldcheck.data.mapper.RemoteMapper
import ar.edu.uade.fieldcheck.data.remote.ApiResult
import ar.edu.uade.fieldcheck.data.remote.FieldCheckRemoteDataSource
import ar.edu.uade.fieldcheck.domain.model.SyncResult
import ar.edu.uade.fieldcheck.domain.repository.SyncRepository
import kotlinx.coroutines.CancellationException
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sube las inspecciones de Room a la API (patrón outbox, ver docs/diagramas.md punto 4).
 *
 * Orden de la subida: primero los datos de la inspección y después las fotos, porque el
 * servidor rechaza una foto de una inspección que todavía no conoce (404).
 * Cada foto subida se marca SYNCED: si la conexión se corta a mitad, el próximo intento
 * sigue desde la foto que faltaba en lugar de mandar todas otra vez.
 */
@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val inspectionDao: InspectionDao,
    private val evidenceDao: EvidenceDao,
    private val remoteDataSource: FieldCheckRemoteDataSource,
) : SyncRepository {

    override suspend fun getPendingInspectionIds(): List<String> {
        return inspectionDao.getPendingFinishedIds()
    }

    override suspend fun uploadInspection(inspectionId: String): SyncResult {
        try {
            val row = inspectionDao.getInspectionWithItems(inspectionId)
                ?: return SyncResult.PermanentError("No encontramos la inspección en el teléfono.")

            val inspectionDto = RemoteMapper.toInspectionDto(row)
                ?: return SyncResult.PermanentError("La inspección no está finalizada o tiene ítems sin completar.")

            val dataResult = remoteDataSource.sendInspection(inspectionDto)
            if (dataResult !is ApiResult.Success) {
                return toSyncResult(dataResult)
            }

            return uploadPendingPhotos(row)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            // Una falla leyendo la base no es culpa del servidor: se reintenta más tarde.
            return SyncResult.TransientError("No pudimos leer la inspección guardada. Se va a reintentar solo.")
        }
    }

    private suspend fun uploadPendingPhotos(row: InspectionWithItems): SyncResult {
        val inspectionId = row.inspection.clientUuid

        for (itemWithEvidence in row.items) {
            val item = itemWithEvidence.itemResult
            for (evidence in itemWithEvidence.evidence) {
                if (evidence.syncStatus == "SYNCED") {
                    continue
                }

                val photo = File(evidence.filePath)
                if (!photo.exists()) {
                    return SyncResult.PermanentError(
                        "Falta el archivo de una foto del ítem ${item.position + 1}. Eliminala y volvé a sacarla.",
                    )
                }

                val photoResult = remoteDataSource.uploadEvidence(
                    inspectionId = inspectionId,
                    evidenceId = evidence.clientUuid,
                    itemPosition = RemoteMapper.toApiPosition(item.position),
                    capturedAt = evidence.capturedAt,
                    latitude = evidence.latitude,
                    longitude = evidence.longitude,
                    photo = photo,
                )
                if (photoResult !is ApiResult.Success) {
                    return toSyncResult(photoResult)
                }
                evidenceDao.markSynced(evidence.clientUuid)
            }
        }

        return SyncResult.Success
    }

    override suspend fun markSynced(inspectionId: String) {
        inspectionDao.markSynced(inspectionId)
    }

    override suspend fun markFailed(inspectionId: String, message: String) {
        inspectionDao.markFailed(inspectionId, message)
    }

    private fun toSyncResult(result: ApiResult<*>): SyncResult {
        return when (result) {
            is ApiResult.Success -> SyncResult.Success
            is ApiResult.TransientError -> SyncResult.TransientError(result.message)
            is ApiResult.PermanentError -> SyncResult.PermanentError(result.message)
        }
    }
}
