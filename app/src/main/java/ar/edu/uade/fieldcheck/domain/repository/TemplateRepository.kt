package ar.edu.uade.fieldcheck.domain.repository

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.model.Template
import kotlinx.coroutines.flow.Flow

/**
 * Contrato del repositorio de plantillas.
 *
 * La primera vez las plantillas salen de una semilla local (para poder usar la app sin haber
 * tenido nunca conexión); después se reemplazan por las que descarga [refreshTemplates].
 */
interface TemplateRepository {

    /** Observa las plantillas guardadas en el teléfono (la caché). */
    fun observeTemplates(): Flow<Resource<List<Template>>>

    /** Busca una plantilla por id. Devuelve [Resource.Error] si no existe. */
    suspend fun getTemplate(templateId: String): Resource<Template>

    /**
     * Descarga las plantillas del servidor y reemplaza la caché local.
     * Si la descarga falla, la caché queda como estaba.
     */
    suspend fun refreshTemplates(): Resource<Unit>
}
