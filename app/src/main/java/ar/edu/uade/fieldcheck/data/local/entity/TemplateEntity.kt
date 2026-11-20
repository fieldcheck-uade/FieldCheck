package ar.edu.uade.fieldcheck.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabla `templates`: caché de las plantillas descargadas (ver docs/diagramas.md, punto 5).
 *
 * Es una caché que se puede reemplazar entera: las inspecciones no dependen de ella
 * porque copian el nombre y los textos al crearse.
 */
@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val version: Int,
    val downloadedAt: Long,
)
