package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.model.Inspection
import ar.edu.uade.fieldcheck.domain.model.InspectionHistory
import ar.edu.uade.fieldcheck.domain.repository.InspectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Caso de uso: obtener el historial de inspecciones para la pantalla de inicio (P1).
 *
 * Regla de negocio que vive acá y no en la UI:
 * - Las inspecciones "En curso" van primero, porque son las que hay que retomar.
 *   Se ordenan por fecha de inicio, de la más reciente a la más antigua.
 * - Las "Finalizadas" se ordenan por fecha de finalización, de la más reciente a la más antigua.
 */
class GetInspectionHistoryUseCase @Inject constructor(
    private val repository: InspectionRepository,
) {

    /** `operator fun invoke` permite llamar al caso de uso como si fuera una función: `getInspectionHistory()`. */
    operator fun invoke(): Flow<Resource<InspectionHistory>> {
        return repository.observeInspections().map { resource ->
            convertToHistory(resource)
        }
    }

    private fun convertToHistory(resource: Resource<List<Inspection>>): Resource<InspectionHistory> {
        when (resource) {
            is Resource.Success -> {
                val history = buildHistory(resource.data)
                return Resource.Success(history)
            }

            is Resource.Error -> {
                return resource
            }
        }
    }

    private fun buildHistory(inspections: List<Inspection>): InspectionHistory {
        val inProgress = mutableListOf<Inspection>()
        val finished = mutableListOf<Inspection>()

        for (inspection in inspections) {
            if (inspection.isFinished()) {
                finished.add(inspection)
            } else {
                inProgress.add(inspection)
            }
        }

        val inProgressSorted = inProgress.sortedByDescending { inspection -> inspection.startedAt }
        val finishedSorted = finished.sortedByDescending { inspection -> getFinishDate(inspection) }

        return InspectionHistory(
            inProgress = inProgressSorted,
            finished = finishedSorted,
        )
    }

    /** Si por algún motivo una inspección finalizada no tiene fecha de fin, se usa la de inicio. */
    private fun getFinishDate(inspection: Inspection): Long {
        val finishedAt = inspection.finishedAt
        if (finishedAt != null) {
            return finishedAt
        }
        return inspection.startedAt
    }
}
