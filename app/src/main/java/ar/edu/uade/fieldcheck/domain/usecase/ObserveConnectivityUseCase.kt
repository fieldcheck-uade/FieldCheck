package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.domain.repository.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/**
 * Caso de uso: saber si hay conexión, para mostrar u ocultar el `OfflineBanner`.
 *
 * Es uno de los casos de uso simples (ver README, "Decisiones relevantes"): existe para que
 * los ViewModels hablen siempre con casos de uso. `distinctUntilChanged` garantiza que solo
 * se emita cuando el valor cambia, sin depender de cómo esté hecho el monitor de red.
 */
class ObserveConnectivityUseCase @Inject constructor(
    private val networkMonitor: NetworkMonitor,
) {

    operator fun invoke(): Flow<Boolean> {
        return networkMonitor.isOnline.distinctUntilChanged()
    }
}
