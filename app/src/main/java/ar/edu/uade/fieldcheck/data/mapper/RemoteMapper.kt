package ar.edu.uade.fieldcheck.data.mapper

import ar.edu.uade.fieldcheck.data.local.entity.InspectionWithItems
import ar.edu.uade.fieldcheck.data.local.entity.TemplateEntity
import ar.edu.uade.fieldcheck.data.local.entity.TemplateItemEntity
import ar.edu.uade.fieldcheck.data.remote.dto.InspectionDto
import ar.edu.uade.fieldcheck.data.remote.dto.ItemResultDto
import ar.edu.uade.fieldcheck.data.remote.dto.TemplateDto

/**
 * Convierte entre las entidades de Room y los DTO de la API.
 *
 * Diferencia importante: en la app las posiciones empiezan en 0 y en la API en 1.
 * La conversión se hace solo acá, para que ninguna otra clase tenga que acordarse.
 */
object RemoteMapper {

    /** Posición de la app (desde 0) → posición de la API (desde 1). */
    fun toApiPosition(appPosition: Int): Int {
        return appPosition + 1
    }

    /** Posición de la API (desde 1) → posición de la app (desde 0). */
    fun toAppPosition(apiPosition: Int): Int {
        return apiPosition - 1
    }

    /**
     * Arma el envío de una inspección finalizada. Devuelve `null` si la inspección todavía no
     * está lista para subir (no está finalizada o tiene ítems sin resultado), algo que no debería
     * pasar porque solo se sincronizan las que pasaron la regla R5.
     */
    fun toInspectionDto(row: InspectionWithItems): InspectionDto? {
        val inspection = row.inspection
        val finishedAt = inspection.finishedAt
        if (inspection.status != "FINISHED" || finishedAt == null) {
            return null
        }

        val items = mutableListOf<ItemResultDto>()
        val sortedItems = row.items.sortedBy { it.itemResult.position }
        for (itemWithEvidence in sortedItems) {
            val item = itemWithEvidence.itemResult
            val result = item.result
            if (result == null) {
                return null
            }
            items.add(
                ItemResultDto(
                    position = toApiPosition(item.position),
                    itemText = item.itemText,
                    result = result,
                    note = item.note,
                ),
            )
        }

        return InspectionDto(
            clientUuid = inspection.clientUuid,
            templateName = inspection.templateName,
            templateVersion = inspection.templateVersion,
            siteName = inspection.siteName,
            status = inspection.status,
            startedAt = inspection.startedAt,
            finishedAt = finishedAt,
            updatedAt = inspection.updatedAt,
            items = items,
        )
    }

    fun toTemplateEntity(dto: TemplateDto, downloadedAt: Long): TemplateEntity {
        return TemplateEntity(
            id = dto.id,
            name = dto.name,
            version = dto.version,
            downloadedAt = downloadedAt,
        )
    }

    fun toTemplateItemEntities(dto: TemplateDto): List<TemplateItemEntity> {
        val items = mutableListOf<TemplateItemEntity>()
        for (itemDto in dto.items) {
            items.add(
                TemplateItemEntity(
                    id = itemDto.id,
                    templateId = dto.id,
                    text = itemDto.text,
                    position = toAppPosition(itemDto.position),
                ),
            )
        }
        return items
    }
}
