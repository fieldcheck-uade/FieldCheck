package ar.edu.uade.fieldcheck.data.mapper

import ar.edu.uade.fieldcheck.data.local.entity.EvidenceEntity
import ar.edu.uade.fieldcheck.data.local.entity.InspectionEntity
import ar.edu.uade.fieldcheck.data.local.entity.InspectionWithItems
import ar.edu.uade.fieldcheck.data.local.entity.InspectionWithProgress
import ar.edu.uade.fieldcheck.data.local.entity.ItemResultWithEvidence
import ar.edu.uade.fieldcheck.domain.model.Evidence
import ar.edu.uade.fieldcheck.domain.model.Inspection
import ar.edu.uade.fieldcheck.domain.model.InspectionDetail
import ar.edu.uade.fieldcheck.domain.model.InspectionStatus
import ar.edu.uade.fieldcheck.domain.model.ItemResult
import ar.edu.uade.fieldcheck.domain.model.ItemResultValue
import ar.edu.uade.fieldcheck.domain.model.SyncStatus

/**
 * Convierte el dato de Room (entidad) al modelo de dominio, y los valores del dominio
 * al texto que se guarda en la base.
 *
 * Mantener esta traducción en un solo lugar hace que, si cambia la base,
 * cambie solo el mapper y no la UI ni los casos de uso.
 */
object InspectionMapper {

    fun toDomain(row: InspectionWithProgress): Inspection {
        return toInspection(
            entity = row.inspection,
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

    /**
     * Convierte una inspección con sus ítems. Los ítems se ordenan por posición porque
     * Room no garantiza el orden de las listas de `@Relation`.
     */
    fun toDomainDetail(row: InspectionWithItems): InspectionDetail {
        val items = mutableListOf<ItemResult>()
        var completedItems = 0

        for (itemRow in row.items) {
            val item = toItemResult(itemRow)
            items.add(item)
            if (item.isCompleted()) {
                completedItems++
            }
        }

        val sortedItems = items.sortedBy { item -> item.position }
        val inspection = toInspection(
            entity = row.inspection,
            totalItems = sortedItems.size,
            completedItems = completedItems,
        )

        return InspectionDetail(
            inspection = inspection,
            items = sortedItems,
            lastSyncError = row.inspection.lastSyncError,
        )
    }

    /** Texto que se guarda en la columna `item_results.result`; `null` = sin completar. */
    fun toDatabaseValue(result: ItemResultValue?): String? {
        when (result) {
            ItemResultValue.COMPLIES -> return "COMPLIES"
            ItemResultValue.NOT_COMPLIES -> return "NOT_COMPLIES"
            ItemResultValue.NOT_APPLICABLE -> return "NOT_APPLICABLE"
            null -> return null
        }
    }

    private fun toInspection(entity: InspectionEntity, totalItems: Int, completedItems: Int): Inspection {
        return Inspection(
            clientUuid = entity.clientUuid,
            templateName = entity.templateName,
            siteName = entity.siteName,
            status = toInspectionStatus(entity.status),
            syncStatus = toSyncStatus(entity.syncStatus),
            startedAt = entity.startedAt,
            finishedAt = entity.finishedAt,
            totalItems = totalItems,
            completedItems = completedItems,
        )
    }

    private fun toItemResult(row: ItemResultWithEvidence): ItemResult {
        val entity = row.itemResult

        val evidence = mutableListOf<Evidence>()
        for (evidenceEntity in row.evidence) {
            evidence.add(toEvidence(evidenceEntity))
        }
        // Las fotos se muestran en el orden en que se tomaron.
        val sortedEvidence = evidence.sortedBy { photo -> photo.capturedAt }

        return ItemResult(
            id = entity.id,
            position = entity.position,
            itemText = entity.itemText,
            result = toItemResultValue(entity.result),
            note = entity.note,
            evidence = sortedEvidence,
        )
    }

    private fun toEvidence(entity: EvidenceEntity): Evidence {
        return Evidence(
            clientUuid = entity.clientUuid,
            filePath = entity.filePath,
            capturedAt = entity.capturedAt,
            latitude = entity.latitude,
            longitude = entity.longitude,
        )
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

    private fun toItemResultValue(value: String?): ItemResultValue? {
        when (value) {
            null -> return null
            "COMPLIES" -> return ItemResultValue.COMPLIES
            "NOT_COMPLIES" -> return ItemResultValue.NOT_COMPLIES
            "NOT_APPLICABLE" -> return ItemResultValue.NOT_APPLICABLE
            else -> throw IllegalArgumentException("Resultado de ítem desconocido: $value")
        }
    }
}
