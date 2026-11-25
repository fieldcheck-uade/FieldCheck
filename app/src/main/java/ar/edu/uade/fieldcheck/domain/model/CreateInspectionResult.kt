package ar.edu.uade.fieldcheck.domain.model

/**
 * Resultado de crear una inspección (regla R1).
 *
 * Son tres casos distintos porque la pantalla los muestra en lugares distintos:
 * - [Created]: se creó; la pantalla navega a la ejecución (P3) con su id.
 * - [SiteRequired]: el sitio quedó vacío; el error va debajo del campo "Sitio".
 * - [Failed]: no se pudo guardar; el mensaje va arriba del botón.
 *
 * Un tipo sellado le permite al ViewModel decidir con un `when` exhaustivo, en lugar de
 * comparar el texto del mensaje (que se rompería si alguien cambia la redacción).
 */
sealed interface CreateInspectionResult {

    data class Created(val inspectionId: String) : CreateInspectionResult

    data object SiteRequired : CreateInspectionResult

    data class Failed(val message: String) : CreateInspectionResult
}
