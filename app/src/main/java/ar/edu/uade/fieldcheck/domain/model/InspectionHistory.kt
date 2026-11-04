package ar.edu.uade.fieldcheck.domain.model

/**
 * Historial de inspecciones ya separado en las dos secciones de la pantalla P1.
 *
 * @property inProgress inspecciones que el inspector tiene que retomar (van primero).
 * @property finished inspecciones cerradas, de la más reciente a la más antigua.
 */
data class InspectionHistory(
    val inProgress: List<Inspection>,
    val finished: List<Inspection>,
) {

    /** Verdadero cuando no hay ninguna inspección guardada en el teléfono. */
    fun isEmpty(): Boolean {
        return inProgress.isEmpty() && finished.isEmpty()
    }
}
