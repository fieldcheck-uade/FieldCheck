package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.domain.model.FinalizationCheck
import ar.edu.uade.fieldcheck.domain.model.ItemResult
import javax.inject.Inject

/**
 * Caso de uso: revisar qué le falta a una inspección para poder finalizarse (reglas R4 y R5).
 *
 * Reglas:
 * - Todo ítem tiene que tener resultado.
 * - Todo "No cumple" tiene que tener evidencia: una nota con texto o al menos una foto.
 *
 * Es una función pura: P4 la usa para mostrar los faltantes y [FinalizeInspectionUseCase]
 * la vuelve a correr antes de guardar, para que la regla no dependa de la pantalla.
 */
class CheckFinalizationUseCase @Inject constructor() {

    operator fun invoke(items: List<ItemResult>): FinalizationCheck {
        val itemsWithoutResult = mutableListOf<ItemResult>()
        val notCompliesWithoutEvidence = mutableListOf<ItemResult>()

        for (item in items) {
            if (!item.isCompleted()) {
                itemsWithoutResult.add(item)
            } else if (item.isNotCompliesWithoutEvidence()) {
                notCompliesWithoutEvidence.add(item)
            }
        }

        return FinalizationCheck(
            itemsWithoutResult = itemsWithoutResult,
            notCompliesWithoutEvidence = notCompliesWithoutEvidence,
        )
    }
}
