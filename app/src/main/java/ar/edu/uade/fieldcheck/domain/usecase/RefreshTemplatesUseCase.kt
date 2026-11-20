package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.repository.TemplateRepository
import javax.inject.Inject

/**
 * Caso de uso: actualizar la caché de plantillas con las del servidor (RF01).
 *
 * Si falla (por ejemplo, sin conexión), la caché anterior queda intacta y la app sigue
 * funcionando con las plantillas que ya tenía: por eso el error no se muestra en pantalla.
 */
class RefreshTemplatesUseCase @Inject constructor(
    private val repository: TemplateRepository,
) {

    suspend operator fun invoke(): Resource<Unit> {
        return repository.refreshTemplates()
    }
}
