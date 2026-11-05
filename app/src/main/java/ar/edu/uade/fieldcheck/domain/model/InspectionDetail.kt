package ar.edu.uade.fieldcheck.domain.model

/**
 * Inspección completa: sus datos generales más todos sus ítems, en orden.
 *
 * Es lo que necesitan las pantallas de ejecución (P3), revisión (P4) y detalle (P5).
 *
 * @property lastSyncError motivo del último error de sincronización, en lenguaje simple, o `null`.
 */
data class InspectionDetail(
    val inspection: Inspection,
    val items: List<ItemResult>,
    val lastSyncError: String?,
) {

    /** Conteo de resultados para los chips de P4 y P5 ("Cumple 16", "No cumple 3"...). */
    fun countResults(): ResultCounts {
        return ResultCounts.fromItems(items)
    }

    /** Busca un ítem por su posición; devuelve `null` si la posición no existe. */
    fun findItemAt(position: Int): ItemResult? {
        for (item in items) {
            if (item.position == position) {
                return item
            }
        }
        return null
    }
}

/**
 * Cantidad de ítems por resultado.
 *
 * @property pending ítems todavía sin completar ("Sin completar").
 */
data class ResultCounts(
    val complies: Int,
    val notComplies: Int,
    val notApplicable: Int,
    val pending: Int,
) {

    companion object {

        /** Recorre los ítems una vez y suma cada resultado en su contador. */
        fun fromItems(items: List<ItemResult>): ResultCounts {
            var complies = 0
            var notComplies = 0
            var notApplicable = 0
            var pending = 0

            for (item in items) {
                when (item.result) {
                    ItemResultValue.COMPLIES -> complies++
                    ItemResultValue.NOT_COMPLIES -> notComplies++
                    ItemResultValue.NOT_APPLICABLE -> notApplicable++
                    null -> pending++
                }
            }

            return ResultCounts(
                complies = complies,
                notComplies = notComplies,
                notApplicable = notApplicable,
                pending = pending,
            )
        }
    }
}
