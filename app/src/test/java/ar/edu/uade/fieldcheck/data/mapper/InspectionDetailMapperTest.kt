package ar.edu.uade.fieldcheck.data.mapper

import ar.edu.uade.fieldcheck.data.local.entity.EvidenceEntity
import ar.edu.uade.fieldcheck.data.local.entity.InspectionEntity
import ar.edu.uade.fieldcheck.data.local.entity.InspectionWithItems
import ar.edu.uade.fieldcheck.data.local.entity.ItemResultEntity
import ar.edu.uade.fieldcheck.data.local.entity.ItemResultWithEvidence
import ar.edu.uade.fieldcheck.data.local.entity.TemplateEntity
import ar.edu.uade.fieldcheck.data.local.entity.TemplateWithItemCount
import ar.edu.uade.fieldcheck.domain.model.Evidence
import ar.edu.uade.fieldcheck.domain.model.ItemResultValue
import ar.edu.uade.fieldcheck.domain.model.Template
import org.junit.Assert.assertEquals
import org.junit.Test

class InspectionDetailMapperTest {

    @Test
    fun `detail items are sorted by position and progress is counted from them`() {
        val row = InspectionWithItems(
            inspection = createInspectionEntity(lastSyncError = "El servidor no respondió."),
            items = listOf(
                itemRow(id = 2L, position = 1, result = null),
                itemRow(id = 1L, position = 0, result = "COMPLIES"),
                itemRow(id = 3L, position = 2, result = "NOT_APPLICABLE"),
            ),
        )

        val detail = InspectionMapper.toDomainDetail(row)

        assertEquals(listOf(0, 1, 2), detail.items.map { item -> item.position })
        assertEquals(3, detail.inspection.totalItems)
        assertEquals(2, detail.inspection.completedItems)
        assertEquals("El servidor no respondió.", detail.lastSyncError)
    }

    @Test
    fun `item result text and evidence are converted`() {
        val evidence = EvidenceEntity(
            clientUuid = "photo-1",
            itemResultId = 1L,
            filePath = "/files/evidence/photo-1.jpg",
            capturedAt = 5_000L,
            latitude = -34.6,
            longitude = -58.4,
            syncStatus = "PENDING",
        )
        val row = InspectionWithItems(
            inspection = createInspectionEntity(),
            items = listOf(itemRow(id = 1L, position = 0, result = "NOT_COMPLIES", evidence = listOf(evidence))),
        )

        val item = InspectionMapper.toDomainDetail(row).items.single()

        assertEquals(ItemResultValue.NOT_COMPLIES, item.result)
        val expectedEvidence = Evidence(
            clientUuid = "photo-1",
            filePath = "/files/evidence/photo-1.jpg",
            capturedAt = 5_000L,
            latitude = -34.6,
            longitude = -58.4,
        )
        assertEquals(listOf(expectedEvidence), item.evidence)
    }

    @Test
    fun `result values round trip to the text stored in the database`() {
        assertEquals("COMPLIES", InspectionMapper.toDatabaseValue(ItemResultValue.COMPLIES))
        assertEquals("NOT_COMPLIES", InspectionMapper.toDatabaseValue(ItemResultValue.NOT_COMPLIES))
        assertEquals("NOT_APPLICABLE", InspectionMapper.toDatabaseValue(ItemResultValue.NOT_APPLICABLE))
        assertEquals(null, InspectionMapper.toDatabaseValue(null))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown result text fails loudly instead of guessing`() {
        val row = InspectionWithItems(
            inspection = createInspectionEntity(),
            items = listOf(itemRow(id = 1L, position = 0, result = "MAYBE")),
        )

        InspectionMapper.toDomainDetail(row)
    }

    @Test
    fun `template row is converted with its item count`() {
        val row = TemplateWithItemCount(
            template = TemplateEntity(id = "t-1", name = "Sala técnica", version = 2, downloadedAt = 1_000L),
            itemCount = 14,
        )

        val template = TemplateMapper.toDomain(row)

        assertEquals(Template(id = "t-1", name = "Sala técnica", version = 2, itemCount = 14, downloadedAt = 1_000L), template)
    }

    private fun itemRow(
        id: Long,
        position: Int,
        result: String?,
        evidence: List<EvidenceEntity> = emptyList(),
    ): ItemResultWithEvidence {
        val entity = ItemResultEntity(
            id = id,
            inspectionId = "uuid-1",
            itemText = "Ítem ${position + 1}",
            position = position,
            result = result,
            note = null,
            updatedAt = 1_000L,
        )
        return ItemResultWithEvidence(itemResult = entity, evidence = evidence)
    }

    private fun createInspectionEntity(lastSyncError: String? = null): InspectionEntity {
        return InspectionEntity(
            clientUuid = "uuid-1",
            templateId = "template-1",
            templateName = "Control semanal de obra",
            siteName = "Planta Norte",
            status = "IN_PROGRESS",
            startedAt = 1_000L,
            finishedAt = null,
            syncStatus = "PENDING",
            lastSyncError = lastSyncError,
            updatedAt = 1_000L,
        )
    }
}
