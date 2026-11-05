package ar.edu.uade.fieldcheck.domain.model

/** Los tres resultados posibles de un ítem. "Sin completar" se representa con `null`. */
enum class ItemResultValue {
    COMPLIES,
    NOT_COMPLIES,
    NOT_APPLICABLE,
}

/**
 * Resultado de un ítem dentro de una inspección.
 *
 * @property id clave autogenerada por la base; es la que usan las pantallas para guardar cambios.
 * @property position orden del ítem, empezando en 0. En pantalla se muestra [displayNumber].
 * @property itemText copia del texto de la plantilla: si la plantilla cambia, la inspección no cambia.
 * @property result `null` mientras el ítem no se completó.
 * @property note nota opcional del inspector; nunca es un texto en blanco (se guarda `null`).
 */
data class ItemResult(
    val id: Long,
    val position: Int,
    val itemText: String,
    val result: ItemResultValue?,
    val note: String?,
    val evidence: List<Evidence>,
) {

    /** Número que ve el usuario: "Ítem 1" es la posición 0. */
    fun displayNumber(): Int {
        return position + 1
    }

    fun isCompleted(): Boolean {
        return result != null
    }

    /** Hay evidencia si el inspector escribió una nota o sacó al menos una foto. */
    fun hasEvidence(): Boolean {
        val currentNote = note
        val hasNote = currentNote != null && currentNote.isNotBlank()
        val hasPhotos = evidence.isNotEmpty()
        return hasNote || hasPhotos
    }

    /** Regla R4: un "No cumple" sin nota ni foto no alcanza para finalizar. */
    fun isNotCompliesWithoutEvidence(): Boolean {
        val isNotComplies = result == ItemResultValue.NOT_COMPLIES
        return isNotComplies && !hasEvidence()
    }
}

/**
 * Foto tomada como evidencia de un ítem.
 *
 * La foto no se guarda en la base: [filePath] apunta al archivo en el almacenamiento interno.
 * [latitude] y [longitude] son `null` si no se pudo (o no se quiso) guardar la ubicación.
 */
data class Evidence(
    val clientUuid: String,
    val filePath: String,
    val capturedAt: Long,
    val latitude: Double?,
    val longitude: Double?,
) {

    /** La miniatura muestra un ícono de ubicación solo si hay coordenadas. */
    fun hasLocation(): Boolean {
        return latitude != null && longitude != null
    }
}
