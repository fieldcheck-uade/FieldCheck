package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.domain.model.ItemResult
import javax.inject.Inject

/**
 * Caso de uso: decidir en qué ítem se abre la ejecución (P3) — regla R2.
 *
 * Reglas, en orden:
 * 1. Si la navegación pidió una posición concreta (por ejemplo, al tocar un ítem en P4)
 *    y esa posición existe, se abre ahí.
 * 2. Si no, se abre en el primer ítem sin resultado, que es donde el inspector dejó.
 * 3. Si todos están completos, se abre en el último ítem.
 *
 * Es una función pura (no lee la base): recibe los ítems ya cargados y ordenados.
 */
class GetStartPositionUseCase @Inject constructor() {

    operator fun invoke(
        items: List<ItemResult>,
        requestedPosition: Int = NO_REQUESTED_POSITION,
    ): Int {
        if (items.isEmpty()) {
            return 0
        }

        val wasPositionRequested = requestedPosition != NO_REQUESTED_POSITION
        if (wasPositionRequested && positionExists(items, requestedPosition)) {
            return requestedPosition
        }

        for (item in items) {
            if (!item.isCompleted()) {
                return item.position
            }
        }

        val lastItem = items.maxBy { item -> item.position }
        return lastItem.position
    }

    private fun positionExists(items: List<ItemResult>, position: Int): Boolean {
        for (item in items) {
            if (item.position == position) {
                return true
            }
        }
        return false
    }

    companion object {
        /** Igual al valor por defecto de `Screen.Execution.startPosition`: "no se pidió ninguna posición". */
        const val NO_REQUESTED_POSITION = -1
    }
}
