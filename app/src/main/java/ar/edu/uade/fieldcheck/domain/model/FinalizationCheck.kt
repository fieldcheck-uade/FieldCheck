package ar.edu.uade.fieldcheck.domain.model

/**
 * Resultado de revisar si una inspección se puede finalizar (reglas R4 y R5).
 *
 * En lugar de devolver solo "sí / no", devuelve QUÉ falta: así P4 puede mostrar
 * la lista de faltantes con su motivo y el usuario sabe exactamente qué corregir.
 *
 * @property itemsWithoutResult ítems "Sin completar".
 * @property notCompliesWithoutEvidence ítems "No cumple" sin nota ni foto.
 */
data class FinalizationCheck(
    val itemsWithoutResult: List<ItemResult>,
    val notCompliesWithoutEvidence: List<ItemResult>,
) {

    fun canFinalize(): Boolean {
        return itemsWithoutResult.isEmpty() && notCompliesWithoutEvidence.isEmpty()
    }

    /** Total de ítems que impiden finalizar ("Faltan 2 ítems para poder finalizar"). */
    fun missingCount(): Int {
        return itemsWithoutResult.size + notCompliesWithoutEvidence.size
    }

    /**
     * Mensaje para el usuario: "Faltan 2 ítems para poder finalizar." o, si es uno solo,
     * "Falta 1 ítem para poder finalizar.". Lo usan el caso de uso y la tarjeta de P4.
     */
    fun missingMessage(): String {
        val count = missingCount()
        if (count == 1) {
            return "Falta 1 ítem para poder finalizar."
        }
        return "Faltan $count ítems para poder finalizar."
    }

    /** Todos los faltantes juntos, ordenados por posición, para la lista filtrada de P4. */
    fun missingItems(): List<ItemResult> {
        val allMissing = mutableListOf<ItemResult>()
        allMissing.addAll(itemsWithoutResult)
        allMissing.addAll(notCompliesWithoutEvidence)
        return allMissing.sortedBy { item -> item.position }
    }
}
