package ar.edu.uade.fieldcheck.fakes

import ar.edu.uade.fieldcheck.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Repositorio de preferencias falso para tests: reemplaza a DataStore.
 *
 * Arranca igual que la app real (modo claro) y guarda en memoria lo que se le pide.
 */
class FakeSettingsRepository(
    initialDarkMode: Boolean = false,
) : SettingsRepository {

    private val darkMode = MutableStateFlow(initialDarkMode)

    override fun observeDarkMode(): Flow<Boolean> {
        return darkMode
    }

    override suspend fun setDarkMode(enabled: Boolean) {
        darkMode.value = enabled
    }
}
