package ar.edu.uade.fieldcheck.core.model

/**
 * Estado genérico de una pantalla en MVVM.
 *
 * El ViewModel expone un `StateFlow<UiState<T>>` y la pantalla Compose dibuja
 * algo distinto para cada caso:
 * - [Loading]: mientras se leen los datos.
 * - [Success]: los datos ya están disponibles.
 * - [Error]: algo falló; se muestra el mensaje y la opción de reintentar.
 */
sealed interface UiState<out T> {

    data object Loading : UiState<Nothing>

    data class Success<T>(val data: T) : UiState<T>

    data class Error(val message: String) : UiState<Nothing>
}
