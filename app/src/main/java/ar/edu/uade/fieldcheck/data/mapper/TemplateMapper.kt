package ar.edu.uade.fieldcheck.data.mapper

import ar.edu.uade.fieldcheck.data.local.entity.TemplateWithItemCount
import ar.edu.uade.fieldcheck.domain.model.Template

/** Convierte las filas de la caché de plantillas al modelo de dominio. */
object TemplateMapper {

    fun toDomain(row: TemplateWithItemCount): Template {
        val entity = row.template

        return Template(
            id = entity.id,
            name = entity.name,
            version = entity.version,
            itemCount = row.itemCount,
            downloadedAt = entity.downloadedAt,
        )
    }

    fun toDomainList(rows: List<TemplateWithItemCount>): List<Template> {
        val templates = mutableListOf<Template>()
        for (row in rows) {
            templates.add(toDomain(row))
        }
        return templates
    }
}
