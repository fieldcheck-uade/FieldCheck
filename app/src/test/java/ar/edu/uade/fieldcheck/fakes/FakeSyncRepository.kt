package ar.edu.uade.fieldcheck.fakes

import ar.edu.uade.fieldcheck.domain.model.SyncResult
import ar.edu.uade.fieldcheck.domain.repository.SyncRepository

/**
 * Repositorio de sincronización falso.
 *
 * Desde el test se elige qué responde el "servidor" para cada inspección con [results];
 * si una inspección no está en el mapa, la subida sale bien.
 */
class FakeSyncRepository(
    private val pendingIds: List<String>,
    private val results: Map<String, SyncResult> = emptyMap(),
) : SyncRepository {

    val uploadedIds = mutableListOf<String>()
    val syncedIds = mutableListOf<String>()
    val failedInspections = mutableMapOf<String, String>()

    override suspend fun getPendingInspectionIds(): List<String> {
        return pendingIds
    }

    override suspend fun uploadInspection(inspectionId: String): SyncResult {
        uploadedIds.add(inspectionId)
        return results[inspectionId] ?: SyncResult.Success
    }

    override suspend fun markSynced(inspectionId: String) {
        syncedIds.add(inspectionId)
    }

    override suspend fun markFailed(inspectionId: String, message: String) {
        failedInspections[inspectionId] = message
    }
}
