package ar.edu.uade.fieldcheck.data.repository

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.data.local.dao.TemplateDao
import ar.edu.uade.fieldcheck.data.local.entity.TemplateWithItemCount
import ar.edu.uade.fieldcheck.data.mapper.TemplateMapper
import ar.edu.uade.fieldcheck.domain.model.Template
import ar.edu.uade.fieldcheck.domain.repository.TemplateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación del repositorio de plantillas usando la caché de Room.
 *
 * Todavía no hay backend: la caché la llena `TemplateSeeder` al abrir la app por primera vez.
 */
@Singleton
class TemplateRepositoryImpl @Inject constructor(
    private val templateDao: TemplateDao,
) : TemplateRepository {

    override fun observeTemplates(): Flow<Resource<List<Template>>> {
        val rowsFlow: Flow<List<TemplateWithItemCount>> = templateDao.observeTemplatesWithItemCount()

        val successFlow: Flow<Resource<List<Template>>> = rowsFlow.map { rows ->
            val templates = TemplateMapper.toDomainList(rows)
            Resource.Success(templates)
        }

        return successFlow.catch { exception ->
            val error = Resource.Error(
                message = "No pudimos leer las plantillas guardadas.",
                cause = exception,
            )
            emit(error)
        }
    }

    override suspend fun getTemplate(templateId: String): Resource<Template> {
        try {
            val row = templateDao.getTemplateWithItemCount(templateId)
            if (row == null) {
                return Resource.Error("No encontramos la plantilla.")
            }
            return Resource.Success(TemplateMapper.toDomain(row))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            return Resource.Error(message = "No pudimos leer la plantilla.", cause = exception)
        }
    }
}
