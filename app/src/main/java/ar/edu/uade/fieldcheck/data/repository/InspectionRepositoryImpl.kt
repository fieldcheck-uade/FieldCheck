package ar.edu.uade.fieldcheck.data.repository

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.data.local.dao.EvidenceDao
import ar.edu.uade.fieldcheck.data.local.dao.InspectionDao
import ar.edu.uade.fieldcheck.data.local.dao.TemplateDao
import ar.edu.uade.fieldcheck.data.local.entity.EvidenceEntity
import ar.edu.uade.fieldcheck.data.local.entity.InspectionEntity
import ar.edu.uade.fieldcheck.data.local.entity.InspectionWithItems
import ar.edu.uade.fieldcheck.data.local.entity.InspectionWithProgress
import ar.edu.uade.fieldcheck.data.local.entity.ItemResultEntity
import ar.edu.uade.fieldcheck.data.local.entity.TemplateItemEntity
import ar.edu.uade.fieldcheck.data.mapper.InspectionMapper
import ar.edu.uade.fieldcheck.domain.model.Inspection
import ar.edu.uade.fieldcheck.domain.model.InspectionDetail
import ar.edu.uade.fieldcheck.domain.model.ItemResultValue
import ar.edu.uade.fieldcheck.domain.model.Template
import ar.edu.uade.fieldcheck.domain.repository.InspectionRepository
import ar.edu.uade.fieldcheck.domain.repository.PhotoStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación del repositorio de inspecciones usando Room.
 *
 * Cumple el contrato [InspectionRepository] definido en Domain. Hilt sabe que tiene que
 * entregar esta clase cuando alguien pide un `InspectionRepository` (ver `di/RepositoryModule`).
 *
 * Ningún método lanza excepciones hacia arriba: las fallas de la base vuelven como
 * [Resource.Error] con un mensaje entendible para el usuario.
 */
@Singleton
class InspectionRepositoryImpl @Inject constructor(
    private val inspectionDao: InspectionDao,
    private val templateDao: TemplateDao,
    private val evidenceDao: EvidenceDao,
    private val photoStorage: PhotoStorage,
) : InspectionRepository {

    override fun observeInspections(): Flow<Resource<List<Inspection>>> {
        val rowsFlow: Flow<List<InspectionWithProgress>> = inspectionDao.observeAllWithProgress()

        val successFlow: Flow<Resource<List<Inspection>>> = rowsFlow.map { rows ->
            val inspections = InspectionMapper.toDomainList(rows)
            Resource.Success(inspections)
        }

        // Si la base falla, no dejamos que la app se cierre: avisamos con un Resource.Error.
        return successFlow.catch { exception ->
            val error = Resource.Error(
                message = "No pudimos leer las inspecciones guardadas.",
                cause = exception,
            )
            emit(error)
        }
    }

    override fun observeInspectionDetail(inspectionId: String): Flow<Resource<InspectionDetail>> {
        val rowFlow: Flow<InspectionWithItems?> = inspectionDao.observeInspectionWithItems(inspectionId)

        val resultFlow: Flow<Resource<InspectionDetail>> = rowFlow.map { row ->
            convertDetailRow(row)
        }

        return resultFlow.catch { exception ->
            val error = Resource.Error(
                message = "No pudimos leer la inspección guardada.",
                cause = exception,
            )
            emit(error)
        }
    }

    private fun convertDetailRow(row: InspectionWithItems?): Resource<InspectionDetail> {
        if (row == null) {
            return Resource.Error("No encontramos esta inspección.")
        }
        val detail = InspectionMapper.toDomainDetail(row)
        return Resource.Success(detail)
    }

    override suspend fun createInspection(template: Template, siteName: String, now: Long): Resource<String> {
        return runDatabaseWrite(errorMessage = "No pudimos crear la inspección. Intentá de nuevo.") {
            val templateItems = templateDao.getItemsForTemplate(template.id)
            if (templateItems.isEmpty()) {
                Resource.Error("La plantilla no tiene ítems.")
            } else {
                val inspectionId = UUID.randomUUID().toString()
                val inspection = buildNewInspection(inspectionId, template, siteName, now)
                val itemResults = copyTemplateItems(inspectionId, templateItems, now)

                inspectionDao.insertInspectionWithItems(inspection, itemResults)
                Resource.Success(inspectionId)
            }
        }
    }

    private fun buildNewInspection(
        inspectionId: String,
        template: Template,
        siteName: String,
        now: Long,
    ): InspectionEntity {
        return InspectionEntity(
            clientUuid = inspectionId,
            templateId = template.id,
            templateName = template.name,
            siteName = siteName,
            status = "IN_PROGRESS",
            startedAt = now,
            finishedAt = null,
            syncStatus = "PENDING",
            lastSyncError = null,
            updatedAt = now,
        )
    }

    /**
     * Regla R1: la inspección guarda una COPIA del texto de cada ítem, todos sin resultado.
     * Así, si mañana la plantilla cambia, esta inspección sigue mostrando lo que se revisó.
     */
    private fun copyTemplateItems(
        inspectionId: String,
        templateItems: List<TemplateItemEntity>,
        now: Long,
    ): List<ItemResultEntity> {
        val itemResults = mutableListOf<ItemResultEntity>()
        for (templateItem in templateItems) {
            val itemResult = ItemResultEntity(
                inspectionId = inspectionId,
                itemText = templateItem.text,
                position = templateItem.position,
                result = null,
                note = null,
                updatedAt = now,
            )
            itemResults.add(itemResult)
        }
        return itemResults
    }

    override suspend fun saveItemResult(
        itemResultId: Long,
        result: ItemResultValue?,
        note: String?,
        now: Long,
    ): Resource<Unit> {
        return runDatabaseWrite(errorMessage = "No pudimos guardar el cambio. Intentá de nuevo.") {
            val updatedRows = inspectionDao.updateItemResult(
                itemResultId = itemResultId,
                result = InspectionMapper.toDatabaseValue(result),
                note = note,
                updatedAt = now,
            )
            resultOfUpdate(updatedRows, notFoundMessage = "No encontramos el ítem.")
        }
    }

    override suspend fun addEvidence(itemResultId: Long, filePath: String, capturedAt: Long): Resource<Unit> {
        return runDatabaseWrite(errorMessage = "No se pudo guardar la foto. Intentá de nuevo.") {
            val evidence = EvidenceEntity(
                clientUuid = UUID.randomUUID().toString(),
                itemResultId = itemResultId,
                filePath = filePath,
                capturedAt = capturedAt,
                // La ubicación todavía no se captura (fuera de alcance, ver S6).
                latitude = null,
                longitude = null,
                syncStatus = "PENDING",
            )
            evidenceDao.insertEvidence(evidence)
            Resource.Success(Unit)
        }
    }

    override suspend fun deleteEvidence(evidenceId: String): Resource<Unit> {
        return runDatabaseWrite(errorMessage = "No pudimos eliminar la foto. Intentá de nuevo.") {
            val evidence = evidenceDao.getEvidence(evidenceId)
            if (evidence == null) {
                Resource.Error("No encontramos la foto.")
            } else {
                evidenceDao.deleteEvidence(evidenceId)
                // Primero la fila y después el archivo: si falla el borrado del archivo,
                // la foto ya no aparece en la app y solo queda un archivo sin usar.
                photoStorage.deletePhotoFile(evidence.filePath)
                Resource.Success(Unit)
            }
        }
    }

    override suspend fun finalizeInspection(inspectionId: String, now: Long): Resource<Unit> {
        return runDatabaseWrite(errorMessage = "No pudimos finalizar la inspección. Intentá de nuevo.") {
            val updatedRows = inspectionDao.finishInspection(inspectionId = inspectionId, finishedAt = now)
            resultOfUpdate(updatedRows, notFoundMessage = "No encontramos una inspección en curso para finalizar.")
        }
    }

    override suspend fun markForSyncRetry(inspectionId: String): Resource<Unit> {
        return runDatabaseWrite(errorMessage = "No pudimos reintentar la sincronización.") {
            val updatedRows = inspectionDao.markForSyncRetry(inspectionId)
            resultOfUpdate(updatedRows, notFoundMessage = "No encontramos esta inspección.")
        }
    }

    /** Un UPDATE que no cambió ninguna fila significa que lo buscado no existe. */
    private fun resultOfUpdate(updatedRows: Int, notFoundMessage: String): Resource<Unit> {
        if (updatedRows == 0) {
            return Resource.Error(notFoundMessage)
        }
        return Resource.Success(Unit)
    }

    /**
     * Ejecuta una operación sobre la base y convierte cualquier falla en [Resource.Error].
     *
     * La [CancellationException] se vuelve a lanzar: significa que la pantalla se cerró y
     * la corrutina se canceló, no que la base falló. Atraparla rompería la cancelación.
     */
    private suspend fun <T> runDatabaseWrite(
        errorMessage: String,
        operation: suspend () -> Resource<T>,
    ): Resource<T> {
        try {
            return operation()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            return Resource.Error(message = errorMessage, cause = exception)
        }
    }
}
