package ar.edu.uade.fieldcheck.presentation.execution

import ar.edu.uade.fieldcheck.domain.model.Evidence
import ar.edu.uade.fieldcheck.domain.model.ItemResult

/**
 * Lo que la pantalla P3 necesita para dibujar el ítem actual.
 *
 * Se arma en el ViewModel a partir de la inspección guardada más lo que el usuario
 * está haciendo en ese momento (por ejemplo, la nota que todavía está escribiendo).
 *
 * @property itemNumber número del ítem para "Ítem 7 de 20" (empieza en 1).
 * @property noteDraft texto actual del campo de nota. Puede adelantarse a lo guardado
 *   porque la nota se guarda cuando el usuario deja de escribir.
 * @property showsEvidenceWarning regla R4: "No cumple" sin nota ni foto.
 * @property isReadOnly la inspección ya está finalizada: se puede mirar pero no cambiar (R6).
 * @property evidencePendingDeletion foto que el usuario pidió borrar y espera confirmación;
 *   `null` si no hay diálogo abierto.
 */
data class ExecutionContent(
    val siteName: String,
    val item: ItemResult,
    val itemNumber: Int,
    val totalItems: Int,
    val noteDraft: String,
    val showsEvidenceWarning: Boolean,
    val isReadOnly: Boolean,
    val evidencePendingDeletion: Evidence?,
) {

    fun isFirstItem(): Boolean {
        return itemNumber == 1
    }

    /** En el último ítem el botón "Siguiente" pasa a ser "Revisar y finalizar". */
    fun isLastItem(): Boolean {
        return itemNumber == totalItems
    }

    /** "Guardado ✓" se muestra cuando el ítem ya tiene un resultado registrado. */
    fun showsSavedIndicator(): Boolean {
        return item.isCompleted()
    }

    /** Avance de la barra de progreso, entre 0 y 1. */
    fun progress(): Float {
        if (totalItems == 0) {
            return 0f
        }
        return itemNumber.toFloat() / totalItems.toFloat()
    }
}

/**
 * Eventos de una sola vez que el ViewModel le pide a la pantalla.
 *
 * Van por un `SharedFlow` y no por el estado porque no deben repetirse al rotar
 * el teléfono (por ejemplo, abrir la cámara dos veces).
 */
sealed interface ExecutionEvent {

    /** Ir a P4 · Revisión y finalización. */
    data class NavigateToReview(val inspectionId: String) : ExecutionEvent

    /** Abrir la cámara del sistema para que guarde la foto en [contentUri]. */
    data class OpenCamera(val contentUri: String) : ExecutionEvent

    /** La foto quedó guardada: Snackbar "Foto guardada". */
    data object PhotoSaved : ExecutionEvent

    /** No hay app de cámara en el teléfono. */
    data object CameraUnavailable : ExecutionEvent

    /** Algo falló al guardar; [message] ya está en lenguaje simple. */
    data class ShowError(val message: String) : ExecutionEvent
}
