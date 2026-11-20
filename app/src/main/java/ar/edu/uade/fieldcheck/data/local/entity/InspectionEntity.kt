package ar.edu.uade.fieldcheck.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabla `inspections` de Room (ver docs/diagramas.md, punto 5).
 *
 * Es la forma del dato pensada para la base: los estados se guardan como texto
 * para que la base sea legible con cualquier visor de SQLite.
 * La conversión a modelo de dominio la hace `InspectionMapper`.
 *
 * `templateVersion` se agregó en la versión 2 de la base (ver `Migrations.kt`): el servidor
 * necesita saber con qué versión de la plantilla se hizo la inspección.
 */
@Entity(tableName = "inspections")
data class InspectionEntity(
    @PrimaryKey
    val clientUuid: String,
    val templateId: String,
    val templateName: String,
    @ColumnInfo(defaultValue = "1")
    val templateVersion: Int,
    val siteName: String,
    val status: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val syncStatus: String,
    val lastSyncError: String?,
    val updatedAt: Long,
)
