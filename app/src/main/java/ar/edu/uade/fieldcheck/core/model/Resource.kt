package ar.edu.uade.fieldcheck.core.model

/**
 * Resultado de una operación de las capas de Data y Domain.
 *
 * El repositorio nunca lanza excepciones hacia arriba: las atrapa y devuelve un [Error]
 * con un mensaje entendible. Así el ViewModel solo tiene que preguntar "¿salió bien o mal?".
 */
sealed class Resource<out T> {

    /** La operación terminó bien y trae los datos pedidos. */
    data class Success<out T>(val data: T) : Resource<T>()

    /** La operación falló. [message] es apto para mostrar al usuario; [cause] queda para depurar. */
    data class Error(
        val message: String,
        val cause: Throwable? = null,
    ) : Resource<Nothing>()
}
