package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.model.CreateInspectionResult
import ar.edu.uade.fieldcheck.domain.model.Template
import ar.edu.uade.fieldcheck.domain.repository.InspectionRepository
import java.time.Clock
import javax.inject.Inject

/**
 * Caso de uso: crear una inspección nueva a partir de una plantilla (regla R1).
 *
 * Reglas:
 * - El sitio es obligatorio: se le quitan los espacios de los extremos y, si queda vacío,
 *   se devuelve [CreateInspectionResult.SiteRequired] sin crear nada.
 * - La copia de los ítems de la plantilla la hace el repositorio al guardar.
 *
 * Si sale bien devuelve el id (`clientUuid`) de la inspección creada, para navegar a la
 * ejecución (P3).
 */
class CreateInspectionUseCase @Inject constructor(
    private val repository: InspectionRepository,
    private val clock: Clock,
) {

    suspend operator fun invoke(template: Template, siteName: String): CreateInspectionResult {
        val trimmedSiteName = siteName.trim()
        if (trimmedSiteName.isBlank()) {
            return CreateInspectionResult.SiteRequired
        }

        val now = clock.millis()
        val result = repository.createInspection(
            template = template,
            siteName = trimmedSiteName,
            now = now,
        )

        return when (result) {
            is Resource.Success -> CreateInspectionResult.Created(result.data)
            is Resource.Error -> CreateInspectionResult.Failed(result.message)
        }
    }
}
