package ar.edu.uade.fieldcheck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import ar.edu.uade.fieldcheck.data.local.entity.TemplateEntity
import ar.edu.uade.fieldcheck.data.local.entity.TemplateItemEntity
import ar.edu.uade.fieldcheck.data.local.entity.TemplateWithItemCount
import kotlinx.coroutines.flow.Flow

/** Acceso a la caché de plantillas y sus ítems. */
@Dao
interface TemplateDao {

    /**
     * Todas las plantillas con su cantidad de ítems.
     * Se cuenta en SQL para no cargar todos los textos solo para mostrar "20 ítems".
     */
    @Query(
        """
        SELECT templates.*, COUNT(template_items.id) AS itemCount
        FROM templates
        LEFT JOIN template_items ON template_items.templateId = templates.id
        GROUP BY templates.id
        """
    )
    fun observeTemplatesWithItemCount(): Flow<List<TemplateWithItemCount>>

    @Query(
        """
        SELECT templates.*, COUNT(template_items.id) AS itemCount
        FROM templates
        LEFT JOIN template_items ON template_items.templateId = templates.id
        WHERE templates.id = :templateId
        GROUP BY templates.id
        """
    )
    suspend fun getTemplateWithItemCount(templateId: String): TemplateWithItemCount?

    @Query("SELECT * FROM template_items WHERE templateId = :templateId ORDER BY position")
    suspend fun getItemsForTemplate(templateId: String): List<TemplateItemEntity>

    @Query("SELECT COUNT(*) FROM templates")
    suspend fun countTemplates(): Int

    @Insert
    suspend fun insertTemplate(template: TemplateEntity)

    @Insert
    suspend fun insertTemplateItems(items: List<TemplateItemEntity>)

    /** Guarda la plantilla y sus ítems en una sola transacción: todo o nada. */
    @Transaction
    suspend fun insertTemplateWithItems(
        template: TemplateEntity,
        items: List<TemplateItemEntity>,
    ) {
        insertTemplate(template)
        insertTemplateItems(items)
    }
}
