package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.model.SyncResult
import ar.edu.uade.fieldcheck.fakes.FakeSyncRepository
import ar.edu.uade.fieldcheck.fakes.FakeTemplateRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPendingInspectionsUseCaseTest {

    @Test
    fun `uploaded inspections are marked as synced`() = runTest {
        val repository = FakeSyncRepository(pendingIds = listOf("a", "b"))
        val useCase = SyncPendingInspectionsUseCase(repository)

        val outcome = useCase()

        assertEquals(SyncPendingInspectionsUseCase.Outcome.DONE, outcome)
        assertEquals(listOf("a", "b"), repository.syncedIds)
        assertTrue(repository.failedInspections.isEmpty())
    }

    @Test
    fun `a transient error keeps the inspection pending and asks to retry later`() = runTest {
        val repository = FakeSyncRepository(
            pendingIds = listOf("a"),
            results = mapOf("a" to SyncResult.TransientError("Sin conexión")),
        )
        val useCase = SyncPendingInspectionsUseCase(repository)

        val outcome = useCase()

        assertEquals(SyncPendingInspectionsUseCase.Outcome.RETRY_LATER, outcome)
        assertTrue(repository.syncedIds.isEmpty())
        assertTrue(repository.failedInspections.isEmpty())
    }

    @Test
    fun `a permanent error marks the inspection as failed without automatic retry`() = runTest {
        val repository = FakeSyncRepository(
            pendingIds = listOf("a"),
            results = mapOf("a" to SyncResult.PermanentError("API key inválida")),
        )
        val useCase = SyncPendingInspectionsUseCase(repository)

        val outcome = useCase()

        assertEquals(SyncPendingInspectionsUseCase.Outcome.DONE, outcome)
        assertEquals(mapOf("a" to "API key inválida"), repository.failedInspections)
        assertTrue(repository.syncedIds.isEmpty())
    }

    @Test
    fun `one failing inspection does not stop the others`() = runTest {
        val repository = FakeSyncRepository(
            pendingIds = listOf("a", "b", "c"),
            results = mapOf(
                "a" to SyncResult.PermanentError("Datos inválidos"),
                "b" to SyncResult.TransientError("Timeout"),
            ),
        )
        val useCase = SyncPendingInspectionsUseCase(repository)

        val outcome = useCase()

        assertEquals(listOf("a", "b", "c"), repository.uploadedIds)
        assertEquals(listOf("c"), repository.syncedIds)
        assertEquals(setOf("a"), repository.failedInspections.keys)
        assertEquals(SyncPendingInspectionsUseCase.Outcome.RETRY_LATER, outcome)
    }

    @Test
    fun `nothing pending means nothing to do`() = runTest {
        val repository = FakeSyncRepository(pendingIds = emptyList())
        val useCase = SyncPendingInspectionsUseCase(repository)

        assertEquals(SyncPendingInspectionsUseCase.Outcome.DONE, useCase())
        assertTrue(repository.uploadedIds.isEmpty())
    }

    @Test
    fun `refresh templates delegates to the repository`() = runTest {
        val repository = FakeTemplateRepository(refreshResult = Resource.Error("Sin conexión"))
        val useCase = RefreshTemplatesUseCase(repository)

        assertEquals(Resource.Error("Sin conexión"), useCase())
        assertEquals(1, repository.refreshCount)
    }
}
