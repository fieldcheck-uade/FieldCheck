package ar.edu.uade.fieldcheck.data.remote

/**
 * Resultado de un pedido a la API, ya clasificado (RNF03).
 *
 * Es interno de la capa de Data: los repositorios lo traducen a los tipos del dominio
 * (`SyncResult` o `Resource`).
 */
sealed interface ApiResult<out T> {

    data class Success<T>(val data: T) : ApiResult<T>

    /** Sin red, timeout o 5xx: puede andar si se reintenta más tarde. */
    data class TransientError(val message: String) : ApiResult<Nothing>

    /** El servidor rechazó el pedido (4xx): reintentar igual no lo arregla. */
    data class PermanentError(val message: String) : ApiResult<Nothing>
}
