package ar.edu.uade.fieldcheck.domain.model

/**
 * Resultado de intentar subir algo al servidor (RF03, RNF03).
 *
 * Separa los errores en dos tipos porque se tratan distinto:
 * - [TransientError]: falta de red, timeout o error 5xx. Puede andar más tarde, así que la
 *   inspección queda "Pendiente" y WorkManager reintenta solo, con espera exponencial.
 * - [PermanentError]: el servidor rechazó el envío (4xx, por ejemplo API key inválida o datos
 *   inválidos). Reintentar solo no lo arregla: la inspección pasa a "Error" con el motivo y
 *   espera a que el usuario toque "Reintentar".
 */
sealed interface SyncResult {

    data object Success : SyncResult

    data class TransientError(val message: String) : SyncResult

    data class PermanentError(val message: String) : SyncResult
}
