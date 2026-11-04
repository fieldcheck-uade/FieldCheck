package ar.edu.uade.fieldcheck.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabla `inspections` de Room (ver docs/diagramas.md, punto 5).
 *
 * Es la forma del dato pensada para la base: los estados se guardan como texto
 * para que la base sea legible con cualquier visor de SQLite.
 * La conversión a modelo de dominio la hace `InspectionMapper`.
 */
@Entity(tableName = "inspections")
data class InspectionEntity(
    @PrimaryKey
    val clientUuid: String,
    val templateId: String,
    val templateName: String,
    val siteName: String,
    val status: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val syncStatus: String,
    val lastSyncError: String?,
    val updatedAt: Long,
)
