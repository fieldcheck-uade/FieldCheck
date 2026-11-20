package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.model.Template
import ar.edu.uade.fieldcheck.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Caso de uso: obtener las plantillas para elegir en "Nueva inspección" (P2).
 *
 * Regla: se ordenan alfabéticamente por nombre, sin distinguir mayúsculas y minúsculas,
 * para que el inspector encuentre siempre cada plantilla en el mismo lugar.
 */
class GetTemplatesUseCase @Inject constructor(
    private val repository: TemplateRepository,
) {

    operator fun invoke(): Flow<Resource<List<Template>>> {
        return repository.observeTemplates().map { resource ->
            sortTemplates(resource)
        }
    }

    private fun sortTemplates(resource: Resource<List<Template>>): Resource<List<Template>> {
        when (resource) {
            is Resource.Success -> {
                val byNameIgnoringCase = compareBy(String.CASE_INSENSITIVE_ORDER) { template: Template ->
                    template.name
                }
                val sortedTemplates = resource.data.sortedWith(byNameIgnoringCase)
                return Resource.Success(sortedTemplates)
            }

            is Resource.Error -> {
                return resource
            }
        }
    }
}
