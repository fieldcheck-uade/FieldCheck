package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.model.Template
import ar.edu.uade.fieldcheck.domain.repository.InspectionRepository
import java.time.Clock
import javax.inject.Inject

/**
 * Caso de uso: crear una inspección nueva a partir de una plantilla (regla R1).
 *
 * Reglas:
 * - El sitio es obligatorio: se le quitan los espacios de los extremos y, si queda vacío,
 *   se devuelve [Resource.Error] con el mensaje [SITE_REQUIRED_MESSAGE] sin crear nada.
 * - La copia de los ítems de la plantilla la hace el repositorio al guardar.
 *
 * Devuelve el id (`clientUuid`) de la inspección creada, para navegar a la ejecución (P3).
 */
class CreateInspectionUseCase @Inject constructor(
    private val repository: InspectionRepository,
    private val clock: Clock,
) {

    suspend operator fun invoke(template: Template, siteName: String): Resource<String> {
        val trimmedSiteName = siteName.trim()
        if (trimmedSiteName.isBlank()) {
            return Resource.Error(SITE_REQUIRED_MESSAGE)
        }

        val now = clock.millis()
        return repository.createInspection(
            template = template,
            siteName = trimmedSiteName,
            now = now,
        )
    }

    companion object {
        const val SITE_REQUIRED_MESSAGE = "Obligatorio"
    }
}
