package ar.edu.uade.fieldcheck.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import ar.edu.uade.fieldcheck.domain.repository.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación de [NetworkMonitor] con `ConnectivityManager` de Android.
 *
 * Android avisa los cambios de red con callbacks; `callbackFlow` los convierte en un [Flow]
 * para que el resto de la app los consuma como cualquier otro dato.
 */
@Singleton
class AndroidNetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) : NetworkMonitor {

    override val isOnline: Flow<Boolean> = createConnectivityFlow().distinctUntilChanged()

    private fun createConnectivityFlow(): Flow<Boolean> {
        return callbackFlow {
            val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    trySend(true)
                }

                override fun onLost(network: Network) {
                    trySend(false)
                }
            }

            // Primero el estado actual: el callback solo avisa cambios futuros.
            trySend(isCurrentlyOnline(connectivityManager))
            connectivityManager.registerDefaultNetworkCallback(callback)

            // Cuando nadie escucha más (la pantalla se cerró), se deja de escuchar a Android.
            awaitClose {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }
    }

    private fun isCurrentlyOnline(connectivityManager: ConnectivityManager): Boolean {
        val activeNetwork = connectivityManager.activeNetwork
        if (activeNetwork == null) {
            return false
        }
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        if (capabilities == null) {
            return false
        }
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
