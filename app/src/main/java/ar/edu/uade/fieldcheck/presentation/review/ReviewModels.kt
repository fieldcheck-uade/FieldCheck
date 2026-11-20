package ar.edu.uade.fieldcheck.presentation.review

import ar.edu.uade.fieldcheck.domain.model.ItemResult
import ar.edu.uade.fieldcheck.domain.model.ResultCounts

/**
 * Lo que dibuja P4 cuando la inspección ya se leyó.
 *
 * @property items todos los ítems, en orden, cada uno con su problema (si tiene).
 * @property missingItems solo los ítems que impiden finalizar, para la lista filtrada (Figma 46:52).
 * @property missingMessage "Faltan N ítems para poder finalizar." o `null` si no falta nada.
 */
data class ReviewContent(
    val inspectionId: String,
    val counts: ResultCounts,
    val items: List<ReviewItem>,
    val missingItems: List<ReviewItem>,
    val missingMessage: String?,
)

/**
 * Un ítem de la lista de revisión.
 *
 * @property problem motivo por el que el ítem impide finalizar, o `null` si está bien.
 */
data class ReviewItem(
    val item: ItemResult,
    val problem: ReviewProblem?,
)

/** Motivos por los que un ítem impide finalizar (reglas R4 y R5). */
enum class ReviewProblem {
    /** El ítem todavía no tiene resultado ("Sin completar"). */
    WITHOUT_RESULT,

    /** "No cumple" sin nota ni foto ("No cumple sin evidencia"). */
    NOT_COMPLIES_WITHOUT_EVIDENCE,
}

/**
 * Estado del proceso de finalizar, separado de los datos de la inspección.
 *
 * @property isShowingMissingItems `true` después de tocar "Finalizar" con faltantes:
 *   la pantalla muestra la tarjeta de aviso y filtra la lista.
 * @property isConfirmDialogVisible `true` mientras se pregunta "¿Finalizar la inspección?".
 * @property isFinalizing `true` mientras se guarda; evita finalizar dos veces.
 */
data class FinalizationUiState(
    val isShowingMissingItems: Boolean = false,
    val isConfirmDialogVisible: Boolean = false,
    val isFinalizing: Boolean = false,
)

/** Eventos de una sola vez que la pantalla consume (navegar o mostrar un Snackbar). */
sealed interface ReviewEvent {

    /** La inspección quedó finalizada: hay que ir a su detalle (P5). */
    data class Finalized(val inspectionId: String) : ReviewEvent

    /** Algo salió mal al finalizar: se muestra [message] en un Snackbar. */
    data class ShowMessage(val message: String) : ReviewEvent
}
