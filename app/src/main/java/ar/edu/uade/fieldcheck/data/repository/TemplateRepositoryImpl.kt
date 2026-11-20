package ar.edu.uade.fieldcheck.data.repository

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.data.local.dao.TemplateDao
import ar.edu.uade.fieldcheck.data.local.entity.TemplateWithItemCount
import ar.edu.uade.fieldcheck.data.local.entity.TemplateEntity
import ar.edu.uade.fieldcheck.data.local.entity.TemplateItemEntity
import ar.edu.uade.fieldcheck.data.mapper.RemoteMapper
import ar.edu.uade.fieldcheck.data.mapper.TemplateMapper
import ar.edu.uade.fieldcheck.data.remote.ApiResult
import ar.edu.uade.fieldcheck.data.remote.FieldCheckRemoteDataSource
import ar.edu.uade.fieldcheck.data.remote.dto.TemplateDto
import ar.edu.uade.fieldcheck.domain.model.Template
import ar.edu.uade.fieldcheck.domain.repository.TemplateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación del repositorio de plantillas: Room como caché y la API como fuente remota.
 *
 * La UI siempre lee de Room (offline first). La primera vez la caché la llena `TemplateSeeder`;
 * cada vez que corre la sincronización con red, [refreshTemplates] la reemplaza por la del servidor.
 */
@Singleton
class TemplateRepositoryImpl @Inject constructor(
    private val templateDao: TemplateDao,
    private val remoteDataSource: FieldCheckRemoteDataSource,
    private val clock: Clock,
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

    override suspend fun refreshTemplates(): Resource<Unit> {
        val remoteResult = remoteDataSource.getTemplates()
        val templates = when (remoteResult) {
            is ApiResult.Success -> remoteResult.data
            is ApiResult.TransientError -> return Resource.Error(remoteResult.message)
            is ApiResult.PermanentError -> return Resource.Error(remoteResult.message)
        }

        // Una lista vacía casi seguro es un problema del servidor: mejor conservar la caché.
        if (templates.isEmpty()) {
            return Resource.Error("El servidor no devolvió plantillas.")
        }

        try {
            saveTemplates(templates)
            return Resource.Success(Unit)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            return Resource.Error(message = "No pudimos guardar las plantillas descargadas.", cause = exception)
        }
    }

    private suspend fun saveTemplates(templates: List<TemplateDto>) {
        val downloadedAt = clock.millis()
        val templateEntities = mutableListOf<TemplateEntity>()
        val itemEntities = mutableListOf<TemplateItemEntity>()
        for (dto in templates) {
            templateEntities.add(RemoteMapper.toTemplateEntity(dto, downloadedAt))
            itemEntities.addAll(RemoteMapper.toTemplateItemEntities(dto))
        }
        templateDao.replaceAllTemplates(templateEntities, itemEntities)
    }
}
