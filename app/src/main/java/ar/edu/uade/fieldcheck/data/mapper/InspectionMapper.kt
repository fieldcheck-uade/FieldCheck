package ar.edu.uade.fieldcheck.data.mapper

import ar.edu.uade.fieldcheck.data.local.entity.InspectionWithProgress
import ar.edu.uade.fieldcheck.domain.model.Inspection
import ar.edu.uade.fieldcheck.domain.model.InspectionStatus
import ar.edu.uade.fieldcheck.domain.model.SyncStatus

/**
 * Convierte el dato de Room (entidad) al modelo de dominio.
 *
 * Mantener esta traducción en un solo lugar hace que, si cambia la base,
 * cambie solo el mapper y no la UI ni los casos de uso.
 */
object InspectionMapper {

    fun toDomain(row: InspectionWithProgress): Inspection {
        val entity = row.inspection

        return Inspection(
            clientUuid = entity.clientUuid,
            templateName = entity.templateName,
            siteName = entity.siteName,
            status = toInspectionStatus(entity.status),
            syncStatus = toSyncStatus(entity.syncStatus),
            startedAt = entity.startedAt,
            finishedAt = entity.finishedAt,
            totalItems = row.totalItems,
            completedItems = row.completedItems,
        )
    }

    fun toDomainList(rows: List<InspectionWithProgress>): List<Inspection> {
        val inspections = mutableListOf<Inspection>()
        for (row in rows) {
            inspections.add(toDomain(row))
        }
        return inspections
    }

    /** El texto guardado en la base tiene que coincidir con el nombre del enum. */
    private fun toInspectionStatus(value: String): InspectionStatus {
        when (value) {
            "IN_PROGRESS" -> return InspectionStatus.IN_PROGRESS
            "FINISHED" -> return InspectionStatus.FINISHED
            else -> throw IllegalArgumentException("Estado de inspección desconocido: $value")
        }
    }

    private fun toSyncStatus(value: String): SyncStatus {
        when (value) {
            "PENDING" -> return SyncStatus.PENDING
            "SYNCED" -> return SyncStatus.SYNCED
            "FAILED" -> return SyncStatus.FAILED
            else -> throw IllegalArgumentException("Estado de sincronización desconocido: $value")
        }
    }
}
