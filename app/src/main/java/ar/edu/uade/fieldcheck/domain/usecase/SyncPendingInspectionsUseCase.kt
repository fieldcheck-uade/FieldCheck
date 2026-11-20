package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.domain.model.SyncResult
import ar.edu.uade.fieldcheck.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * Caso de uso: subir todas las inspecciones pendientes (RF03). Lo ejecuta `SyncWorker`.
 *
 * Reglas (RNF03):
 * - Se intenta cada inspección por separado: si una falla, las demás se siguen subiendo.
 * - Subida completa → "Sincronizada".
 * - Error transitorio (sin red, timeout, 5xx) → sigue "Pendiente" y se pide reintentar más tarde.
 * - Error permanente (4xx) → "Error" con el motivo, sin reintento automático.
 */
class SyncPendingInspectionsUseCase @Inject constructor(
    private val repository: SyncRepository,
) {

    /** Lo que tiene que hacer quien llamó cuando termina la pasada. */
    enum class Outcome {
        /** No quedó nada que reintentar solo (todo subió, o falló de forma permanente). */
        DONE,

        /** Alguna inspección tuvo un error transitorio: conviene volver a intentar más tarde. */
        RETRY_LATER,
    }

    suspend operator fun invoke(): Outcome {
        val pendingIds = repository.getPendingInspectionIds()
        var needsRetry = false

        for (inspectionId in pendingIds) {
            val result = repository.uploadInspection(inspectionId)
            when (result) {
                is SyncResult.Success -> {
                    repository.markSynced(inspectionId)
                }

                is SyncResult.TransientError -> {
                    // No se toca el estado: sigue PENDING y el usuario lo ve como "Pendiente".
                    needsRetry = true
                }

                is SyncResult.PermanentError -> {
                    repository.markFailed(inspectionId, result.message)
                }
            }
        }

        if (needsRetry) {
            return Outcome.RETRY_LATER
        }
        return Outcome.DONE
    }
}
