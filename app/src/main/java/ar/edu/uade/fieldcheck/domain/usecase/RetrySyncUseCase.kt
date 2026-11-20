package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.model.Inspection
import ar.edu.uade.fieldcheck.domain.model.SyncStatus
import ar.edu.uade.fieldcheck.domain.repository.InspectionRepository
import ar.edu.uade.fieldcheck.domain.repository.SyncScheduler
import javax.inject.Inject

/**
 * Caso de uso: "Reintentar" la sincronización desde el detalle (P5).
 *
 * Regla: solo se vuelve a encolar una inspección cuya sincronización falló. Una pendiente
 * ya está en la cola y una sincronizada no tiene nada que subir.
 *
 * Reintentar la deja PENDING (se borra el error) y pide una sincronización: si hay red sube
 * enseguida, y si no, WorkManager espera a que vuelva la conexión.
 */
class RetrySyncUseCase @Inject constructor(
    private val repository: InspectionRepository,
    private val syncScheduler: SyncScheduler,
) {

    suspend operator fun invoke(inspection: Inspection): Resource<Unit> {
        if (inspection.syncStatus != SyncStatus.FAILED) {
            return Resource.Error("Solo se puede reintentar una inspección con error de sincronización.")
        }
        val result = repository.markForSyncRetry(inspection.clientUuid)
        if (result is Resource.Success) {
            syncScheduler.requestSync()
        }
        return result
    }
}
