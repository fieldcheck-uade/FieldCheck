package ar.edu.uade.fieldcheck.data.local.database

import ar.edu.uade.fieldcheck.data.local.dao.InspectionDao
import ar.edu.uade.fieldcheck.data.local.entity.InspectionEntity
import ar.edu.uade.fieldcheck.data.local.entity.ItemResultEntity
import javax.inject.Inject

/**
 * Carga inspecciones de ejemplo la primera vez que se abre la app.
 *
 * TEMPORAL: existe solo para poder ver la pantalla de Historial con datos mientras
 * no está implementada la pantalla "Nueva inspección" (P2). Se borra cuando P2 cree
 * inspecciones reales.
 */
class SampleDataSeeder @Inject constructor(
    private val inspectionDao: InspectionDao,
) {

    suspend fun seedIfEmpty() {
        val existingInspections = inspectionDao.countInspections()
        if (existingInspections > 0) {
            return
        }

        val now = System.currentTimeMillis()
        val oneHour = 60L * 60L * 1000L
        val oneDay = 24L * oneHour

        insertInspectionWithItems(
            inspection = createInspection(
                clientUuid = "sample-in-progress-1",
                siteName = "Planta Norte · Sala de bombas",
                status = "IN_PROGRESS",
                startedAt = now - oneHour,
                finishedAt = null,
                syncStatus = "PENDING",
            ),
            completedResults = listOf("COMPLIES", "COMPLIES", "NOT_APPLICABLE", null, null),
        )

        insertInspectionWithItems(
            inspection = createInspection(
                clientUuid = "sample-finished-synced",
                siteName = "Obra Belgrano · Piso 3",
                status = "FINISHED",
                startedAt = now - oneDay - oneHour,
                finishedAt = now - oneDay,
                syncStatus = "SYNCED",
            ),
            completedResults = listOf("COMPLIES", "COMPLIES", "COMPLIES", "NOT_COMPLIES", "COMPLIES"),
        )

        insertInspectionWithItems(
            inspection = createInspection(
                clientUuid = "sample-finished-failed",
                siteName = "Depósito Sur · Tablero principal",
                status = "FINISHED",
                startedAt = now - 2 * oneDay - oneHour,
                finishedAt = now - 2 * oneDay,
                syncStatus = "FAILED",
            ),
            completedResults = listOf("COMPLIES", "NOT_COMPLIES", "COMPLIES", "COMPLIES", "COMPLIES"),
        )
    }

    private fun createInspection(
        clientUuid: String,
        siteName: String,
        status: String,
        startedAt: Long,
        finishedAt: Long?,
        syncStatus: String,
    ): InspectionEntity {
        return InspectionEntity(
            clientUuid = clientUuid,
            templateId = "sample-template-electrical",
            templateName = "Seguridad eléctrica",
            siteName = siteName,
            status = status,
            startedAt = startedAt,
            finishedAt = finishedAt,
            syncStatus = syncStatus,
            lastSyncError = null,
            updatedAt = startedAt,
        )
    }

    /** Cada posición de [completedResults] es un ítem; null significa "todavía sin completar". */
    private suspend fun insertInspectionWithItems(
        inspection: InspectionEntity,
        completedResults: List<String?>,
    ) {
        inspectionDao.insertInspection(inspection)

        val itemResults = mutableListOf<ItemResultEntity>()
        for (index in completedResults.indices) {
            val itemResult = ItemResultEntity(
                inspectionId = inspection.clientUuid,
                itemText = "Ítem de ejemplo ${index + 1}",
                position = index,
                result = completedResults[index],
                note = null,
                updatedAt = inspection.startedAt,
            )
            itemResults.add(itemResult)
        }
        inspectionDao.insertItemResults(itemResults)
    }
}
