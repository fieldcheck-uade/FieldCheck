package ar.edu.uade.fieldcheck.data.mapper

import ar.edu.uade.fieldcheck.data.local.entity.InspectionEntity
import ar.edu.uade.fieldcheck.data.local.entity.InspectionWithItems
import ar.edu.uade.fieldcheck.data.local.entity.ItemResultEntity
import ar.edu.uade.fieldcheck.data.local.entity.ItemResultWithEvidence
import ar.edu.uade.fieldcheck.data.remote.dto.ItemResultDto
import ar.edu.uade.fieldcheck.data.remote.dto.TemplateDto
import ar.edu.uade.fieldcheck.data.remote.dto.TemplateItemDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteMapperTest {

    @Test
    fun `inspection dto uses positions starting at 1 and keeps the template version`() {
        val row = InspectionWithItems(
            inspection = createInspection(status = "FINISHED", finishedAt = 2_000L),
            items = listOf(
                createItem(id = 2, position = 1, result = "NOT_COMPLIES", note = "Sin tapa"),
                createItem(id = 1, position = 0, result = "COMPLIES", note = null),
            ),
        )

        val dto = RemoteMapper.toInspectionDto(row)!!

        assertEquals(3, dto.templateVersion)
        assertEquals(2_000L, dto.finishedAt)
        assertEquals(
            listOf(
                ItemResultDto(position = 1, itemText = "Ítem 0", result = "COMPLIES", note = null),
                ItemResultDto(position = 2, itemText = "Ítem 1", result = "NOT_COMPLIES", note = "Sin tapa"),
            ),
            dto.items,
        )
    }

    @Test
    fun `an inspection in progress is not sent`() {
        val row = InspectionWithItems(
            inspection = createInspection(status = "IN_PROGRESS", finishedAt = null),
            items = listOf(createItem(id = 1, position = 0, result = "COMPLIES", note = null)),
        )

        assertNull(RemoteMapper.toInspectionDto(row))
    }

    @Test
    fun `an item without result is not sent`() {
        val row = InspectionWithItems(
            inspection = createInspection(status = "FINISHED", finishedAt = 2_000L),
            items = listOf(createItem(id = 1, position = 0, result = null, note = null)),
        )

        assertNull(RemoteMapper.toInspectionDto(row))
    }

    @Test
    fun `downloaded template items go back to positions starting at 0`() {
        val dto = TemplateDto(
            id = "template-1",
            name = "Sala técnica",
            version = 2,
            items = listOf(TemplateItemDto(id = "template-1-1", text = "Extintor", position = 1)),
        )

        val template = RemoteMapper.toTemplateEntity(dto, downloadedAt = 5_000L)
        val items = RemoteMapper.toTemplateItemEntities(dto)

        assertEquals(2, template.version)
        assertEquals(5_000L, template.downloadedAt)
        assertEquals(0, items[0].position)
        assertEquals("template-1", items[0].templateId)
    }

    private fun createInspection(status: String, finishedAt: Long?): InspectionEntity {
        return InspectionEntity(
            clientUuid = "uuid-1",
            templateId = "template-1",
            templateName = "Control semanal de obra",
            templateVersion = 3,
            siteName = "Obra Belgrano",
            status = status,
            startedAt = 1_000L,
            finishedAt = finishedAt,
            syncStatus = "PENDING",
            lastSyncError = null,
            updatedAt = 2_000L,
        )
    }

    private fun createItem(id: Long, position: Int, result: String?, note: String?): ItemResultWithEvidence {
        return ItemResultWithEvidence(
            itemResult = ItemResultEntity(
                id = id,
                inspectionId = "uuid-1",
                itemText = "Ítem $position",
                position = position,
                result = result,
                note = note,
                updatedAt = 2_000L,
            ),
            evidence = emptyList(),
        )
    }
}
