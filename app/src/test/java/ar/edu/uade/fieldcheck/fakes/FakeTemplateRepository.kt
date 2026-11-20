package ar.edu.uade.fieldcheck.fakes

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.model.Template
import ar.edu.uade.fieldcheck.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Repositorio de plantillas falso. Desde el test se cambia lo guardado con [emit]. */
class FakeTemplateRepository(
    initialValue: Resource<List<Template>> = Resource.Success(emptyList()),
) : TemplateRepository {

    private val templates = MutableStateFlow(initialValue)

    override fun observeTemplates(): Flow<Resource<List<Template>>> {
        return templates
    }

    override suspend fun getTemplate(templateId: String): Resource<Template> {
        val current = templates.value
        if (current is Resource.Success) {
            for (template in current.data) {
                if (template.id == templateId) {
                    return Resource.Success(template)
                }
            }
        }
        return Resource.Error("No encontramos la plantilla.")
    }

    fun emit(value: Resource<List<Template>>) {
        templates.value = value
    }
}

/** Crea una plantilla de prueba; cada test cambia solo lo que le importa. */
fun createTestTemplate(
    id: String,
    name: String = "Plantilla $id",
    version: Int = 1,
    itemCount: Int = 10,
    downloadedAt: Long = 1_000L,
): Template {
    return Template(
        id = id,
        name = name,
        version = version,
        itemCount = itemCount,
        downloadedAt = downloadedAt,
    )
}
