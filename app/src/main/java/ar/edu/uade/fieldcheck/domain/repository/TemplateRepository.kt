package ar.edu.uade.fieldcheck.domain.repository

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.model.Template
import kotlinx.coroutines.flow.Flow

/**
 * Contrato del repositorio de plantillas.
 *
 * Hoy las plantillas salen de una semilla local que simula la descarga de la API;
 * cuando exista el backend, solo cambia la implementación en Data.
 */
interface TemplateRepository {

    /** Observa las plantillas guardadas en el teléfono (la caché). */
    fun observeTemplates(): Flow<Resource<List<Template>>>

    /** Busca una plantilla por id. Devuelve [Resource.Error] si no existe. */
    suspend fun getTemplate(templateId: String): Resource<Template>
}
