package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.domain.repository.SettingsRepository
import javax.inject.Inject

/** Caso de uso: activar o desactivar el modo oscuro y recordarlo para la próxima vez. */
class SetDarkModeUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {

    suspend operator fun invoke(enabled: Boolean) {
        repository.setDarkMode(enabled)
    }
}
