package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.model.Inspection
import ar.edu.uade.fieldcheck.domain.model.SyncStatus
import ar.edu.uade.fieldcheck.domain.repository.InspectionRepository
import javax.inject.Inject

/**
 * Caso de uso: "Reintentar" la sincronización desde el detalle (P5).
 *
 * Regla: solo se vuelve a encolar una inspección cuya sincronización falló. Una pendiente
 * ya está en la cola y una sincronizada no tiene nada que subir.
 *
 * Por ahora no hay backend: reintentar solo la deja PENDING (ver S6).
 */
class RetrySyncUseCase @Inject constructor(
    private val repository: InspectionRepository,
) {

    suspend operator fun invoke(inspection: Inspection): Resource<Unit> {
        if (inspection.syncStatus != SyncStatus.FAILED) {
            return Resource.Error("Solo se puede reintentar una inspección con error de sincronización.")
        }
        return repository.markForSyncRetry(inspection.clientUuid)
    }
}
