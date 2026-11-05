package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.domain.repository.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/**
 * Caso de uso: saber si hay conexión, para mostrar u ocultar el `OfflineBanner`.
 *
 * Regla: solo se emite cuando el valor cambia. Android puede avisar varias veces seguidas
 * que hay red (por ejemplo al pasar de Wi-Fi a datos); sin este filtro la pantalla se
 * redibujaría sin motivo.
 */
class ObserveConnectivityUseCase @Inject constructor(
    private val networkMonitor: NetworkMonitor,
) {

    operator fun invoke(): Flow<Boolean> {
        return networkMonitor.isOnline.distinctUntilChanged()
    }
}
