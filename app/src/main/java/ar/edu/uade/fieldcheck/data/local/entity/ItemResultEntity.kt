package ar.edu.uade.fieldcheck.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla `item_results`: el resultado de cada ítem del checklist dentro de una inspección.
 *
 * - `itemText` es una copia del texto de la plantilla: si la plantilla cambia mañana,
 *   la inspección de hoy sigue mostrando lo que se revisó.
 * - `result` es null mientras el ítem no se completó; así se calcula el progreso ("3 de 8").
 * - Si se borra la inspección, sus ítems se borran en cascada.
 */
@Entity(
    tableName = "item_results",
    foreignKeys = [
        ForeignKey(
            entity = InspectionEntity::class,
            parentColumns = ["clientUuid"],
            childColumns = ["inspectionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["inspectionId"])],
)
data class ItemResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val inspectionId: String,
    val itemText: String,
    val position: Int,
    val result: String?,
    val note: String?,
    val updatedAt: Long,
)
