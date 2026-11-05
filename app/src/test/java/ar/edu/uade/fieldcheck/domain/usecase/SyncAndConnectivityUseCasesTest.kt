package ar.edu.uade.fieldcheck.domain.usecase

import app.cash.turbine.test
import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.model.InspectionStatus
import ar.edu.uade.fieldcheck.domain.model.SyncStatus
import ar.edu.uade.fieldcheck.fakes.FakeInspectionRepository
import ar.edu.uade.fieldcheck.fakes.FakeNetworkMonitor
import ar.edu.uade.fieldcheck.fakes.createTestDetail
import ar.edu.uade.fieldcheck.fakes.createTestInspection
import ar.edu.uade.fieldcheck.fakes.createTestItemResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncAndConnectivityUseCasesTest {

    @Test
    fun `retry re-queues an inspection whose sync failed`() = runTest {
        val repository = FakeInspectionRepository()
        val useCase = RetrySyncUseCase(repository)
        val failed = createTestInspection("abc", status = InspectionStatus.FINISHED, syncStatus = SyncStatus.FAILED)

        val result = useCase(failed)

        assertEquals(Resource.Success(Unit), result)
        assertEquals(listOf("abc"), repository.retriedInspectionIds)
    }

    @Test
    fun `retry is refused when the sync did not fail`() = runTest {
        val repository = FakeInspectionRepository()
        val useCase = RetrySyncUseCase(repository)
        val synced = createTestInspection("abc", status = InspectionStatus.FINISHED, syncStatus = SyncStatus.SYNCED)

        val result = useCase(synced)

        assertTrue(result is Resource.Error)
        assertTrue(repository.retriedInspectionIds.isEmpty())
    }

    @Test
    fun `connectivity only emits when the value changes`() = runTest {
        val monitor = FakeNetworkMonitor(initiallyOnline = true)
        val useCase = ObserveConnectivityUseCase(monitor)

        useCase().test {
            assertEquals(true, awaitItem())
            monitor.setOnline(false)
            assertEquals(false, awaitItem())
            monitor.setOnline(true)
            assertEquals(true, awaitItem())
        }
    }

    @Test
    fun `inspection detail is read from the repository`() = runTest {
        val repository = FakeInspectionRepository()
        val detail = createTestDetail(items = listOf(createTestItemResult(0)))
        repository.emitDetail("inspection-1", Resource.Success(detail))
        val useCase = GetInspectionDetailUseCase(repository)

        assertEquals(Resource.Success(detail), useCase("inspection-1").first())
    }
}
