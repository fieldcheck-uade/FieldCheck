package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Caso de uso: saber si la app se tiene que ver en modo oscuro.
 *
 * Devuelve un [Flow] para que el tema cambie en el momento en que el usuario toca el botón.
 */
class ObserveDarkModeUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {

    operator fun invoke(): Flow<Boolean> {
        return repository.observeDarkMode()
    }
}
