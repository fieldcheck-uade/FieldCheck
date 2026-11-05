package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.model.InspectionDetail
import ar.edu.uade.fieldcheck.domain.repository.InspectionRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import javax.inject.Inject

/**
 * Caso de uso: finalizar una inspección (regla R5).
 *
 * Reglas:
 * - Antes de guardar se lee la inspección actual y se corre [CheckFinalizationUseCase].
 *   Si falta algo, NO se finaliza y se devuelve [Resource.Error] con "Faltan N ítems...".
 *   Se vuelve a validar acá (aunque P4 ya lo muestre) porque la regla es del negocio,
 *   no de la pantalla.
 * - Una inspección ya finalizada es de solo lectura (R6): no se puede finalizar de nuevo.
 * - Si todo está bien, el repositorio la pasa a FINISHED con la hora actual y PENDING para subir.
 */
class FinalizeInspectionUseCase @Inject constructor(
    private val repository: InspectionRepository,
    private val checkFinalization: CheckFinalizationUseCase,
    private val clock: Clock,
) {

    suspend operator fun invoke(inspectionId: String): Resource<Unit> {
        val currentDetail = repository.observeInspectionDetail(inspectionId).first()

        when (currentDetail) {
            is Resource.Error -> {
                return currentDetail
            }

            is Resource.Success -> {
                return finalizeIfComplete(currentDetail.data)
            }
        }
    }

    private suspend fun finalizeIfComplete(detail: InspectionDetail): Resource<Unit> {
        if (detail.inspection.isFinished()) {
            return Resource.Error("La inspección ya está finalizada.")
        }

        val check = checkFinalization(detail.items)
        if (!check.canFinalize()) {
            return Resource.Error(check.missingMessage())
        }

        val now = clock.millis()
        return repository.finalizeInspection(
            inspectionId = detail.inspection.clientUuid,
            now = now,
        )
    }
}
