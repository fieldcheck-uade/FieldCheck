package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.model.InspectionDetail
import ar.edu.uade.fieldcheck.domain.repository.InspectionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Caso de uso: observar una inspección completa (P3, P4 y P5).
 *
 * El repositorio ya entrega los ítems ordenados por posición; este caso de uso existe
 * para que los ViewModels hablen siempre con casos de uso y nunca con repositorios.
 */
class GetInspectionDetailUseCase @Inject constructor(
    private val repository: InspectionRepository,
) {

    operator fun invoke(inspectionId: String): Flow<Resource<InspectionDetail>> {
        return repository.observeInspectionDetail(inspectionId)
    }
}
