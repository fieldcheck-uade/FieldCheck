package ar.edu.uade.fieldcheck.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla `template_items`: los ítems de cada plantilla, con su orden.
 *
 * Si se borra la plantilla, sus ítems se borran en cascada.
 */
@Entity(
    tableName = "template_items",
    foreignKeys = [
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["templateId"])],
)
data class TemplateItemEntity(
    @PrimaryKey
    val id: String,
    val templateId: String,
    val text: String,
    val position: Int,
)
