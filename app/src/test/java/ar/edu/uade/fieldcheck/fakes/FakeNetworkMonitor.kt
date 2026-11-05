package ar.edu.uade.fieldcheck.fakes

import ar.edu.uade.fieldcheck.domain.repository.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Conexión falsa: el test decide si hay red con [setOnline]. */
class FakeNetworkMonitor(
    initiallyOnline: Boolean = true,
) : NetworkMonitor {

    private val online = MutableStateFlow(initiallyOnline)

    override val isOnline: Flow<Boolean> = online

    fun setOnline(value: Boolean) {
        online.value = value
    }
}
