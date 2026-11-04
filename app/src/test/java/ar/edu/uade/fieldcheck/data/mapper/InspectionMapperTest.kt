package ar.edu.uade.fieldcheck.data.mapper

import ar.edu.uade.fieldcheck.data.local.entity.InspectionEntity
import ar.edu.uade.fieldcheck.data.local.entity.InspectionWithProgress
import ar.edu.uade.fieldcheck.domain.model.Inspection
import ar.edu.uade.fieldcheck.domain.model.InspectionStatus
import ar.edu.uade.fieldcheck.domain.model.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class InspectionMapperTest {

    @Test
    fun `converts a database row into the domain model`() {
        val row = InspectionWithProgress(
            inspection = createEntity(status = "FINISHED", syncStatus = "FAILED"),
            totalItems = 8,
            completedItems = 8,
        )

        val inspection = InspectionMapper.toDomain(row)

        val expected = Inspection(
            clientUuid = "uuid-1",
            templateName = "Seguridad eléctrica",
            siteName = "Planta Norte",
            status = InspectionStatus.FINISHED,
            syncStatus = SyncStatus.FAILED,
            startedAt = 1_000L,
            finishedAt = 2_000L,
            totalItems = 8,
            completedItems = 8,
        )
        assertEquals(expected, inspection)
    }

    @Test
    fun `converts every status stored as text`() {
        val inProgress = InspectionMapper.toDomain(rowWith(status = "IN_PROGRESS", syncStatus = "PENDING"))
        val synced = InspectionMapper.toDomain(rowWith(status = "FINISHED", syncStatus = "SYNCED"))

        assertEquals(InspectionStatus.IN_PROGRESS, inProgress.status)
        assertEquals(SyncStatus.PENDING, inProgress.syncStatus)
        assertEquals(InspectionStatus.FINISHED, synced.status)
        assertEquals(SyncStatus.SYNCED, synced.syncStatus)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown status text fails loudly instead of guessing`() {
        InspectionMapper.toDomain(rowWith(status = "DELETED", syncStatus = "PENDING"))
    }

    private fun rowWith(status: String, syncStatus: String): InspectionWithProgress {
        return InspectionWithProgress(
            inspection = createEntity(status = status, syncStatus = syncStatus),
            totalItems = 0,
            completedItems = 0,
        )
    }

    private fun createEntity(status: String, syncStatus: String): InspectionEntity {
        return InspectionEntity(
            clientUuid = "uuid-1",
            templateId = "template-1",
            templateName = "Seguridad eléctrica",
            siteName = "Planta Norte",
            status = status,
            startedAt = 1_000L,
            finishedAt = 2_000L,
            syncStatus = syncStatus,
            lastSyncError = null,
            updatedAt = 1_000L,
        )
    }
}
