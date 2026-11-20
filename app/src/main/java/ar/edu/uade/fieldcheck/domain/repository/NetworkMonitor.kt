package ar.edu.uade.fieldcheck.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Informa si el teléfono tiene conexión.
 *
 * Es una interfaz del dominio para que los ViewModels se puedan testear con una
 * conexión falsa; la implementación real usa `ConnectivityManager` de Android.
 */
interface NetworkMonitor {

    /** Emite `true` con conexión y `false` sin conexión, cada vez que cambia. */
    val isOnline: Flow<Boolean>
}
