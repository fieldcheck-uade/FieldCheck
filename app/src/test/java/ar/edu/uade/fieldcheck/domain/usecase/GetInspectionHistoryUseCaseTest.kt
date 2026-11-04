package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.model.InspectionHistory
import ar.edu.uade.fieldcheck.domain.model.InspectionStatus
import ar.edu.uade.fieldcheck.fakes.FakeInspectionRepository
import ar.edu.uade.fieldcheck.fakes.createTestInspection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetInspectionHistoryUseCaseTest {

    @Test
    fun `separates in progress and finished inspections`() = runTest {
        val repository = FakeInspectionRepository(
            Resource.Success(
                listOf(
                    createTestInspection("in-progress", status = InspectionStatus.IN_PROGRESS),
                    createTestInspection("finished", status = InspectionStatus.FINISHED, finishedAt = 2_000L),
                )
            )
        )
        val useCase = GetInspectionHistoryUseCase(repository)

        val history = getHistoryOrFail(useCase)

        assertEquals(listOf("in-progress"), history.inProgress.map { inspection -> inspection.clientUuid })
        assertEquals(listOf("finished"), history.finished.map { inspection -> inspection.clientUuid })
    }

    @Test
    fun `sorts in progress by start date, most recent first`() = runTest {
        val repository = FakeInspectionRepository(
            Resource.Success(
                listOf(
                    createTestInspection("old", startedAt = 1_000L),
                    createTestInspection("new", startedAt = 3_000L),
                    createTestInspection("middle", startedAt = 2_000L),
                )
            )
        )
        val useCase = GetInspectionHistoryUseCase(repository)

        val history = getHistoryOrFail(useCase)

        assertEquals(listOf("new", "middle", "old"), history.inProgress.map { inspection -> inspection.clientUuid })
    }

    @Test
    fun `sorts finished by finish date, most recent first`() = runTest {
        val repository = FakeInspectionRepository(
            Resource.Success(
                listOf(
                    createTestInspection("old", status = InspectionStatus.FINISHED, startedAt = 900L, finishedAt = 1_000L),
                    createTestInspection("new", status = InspectionStatus.FINISHED, startedAt = 100L, finishedAt = 5_000L),
                )
            )
        )
        val useCase = GetInspectionHistoryUseCase(repository)

        val history = getHistoryOrFail(useCase)

        assertEquals(listOf("new", "old"), history.finished.map { inspection -> inspection.clientUuid })
    }

    @Test
    fun `finished inspection without finish date is sorted by its start date`() = runTest {
        val repository = FakeInspectionRepository(
            Resource.Success(
                listOf(
                    createTestInspection("with-date", status = InspectionStatus.FINISHED, finishedAt = 2_000L),
                    createTestInspection("no-date", status = InspectionStatus.FINISHED, startedAt = 3_000L, finishedAt = null),
                )
            )
        )
        val useCase = GetInspectionHistoryUseCase(repository)

        val history = getHistoryOrFail(useCase)

        assertEquals(listOf("no-date", "with-date"), history.finished.map { inspection -> inspection.clientUuid })
    }

    @Test
    fun `no inspections returns an empty history`() = runTest {
        val repository = FakeInspectionRepository(Resource.Success(emptyList()))
        val useCase = GetInspectionHistoryUseCase(repository)

        val history = getHistoryOrFail(useCase)

        assertTrue(history.isEmpty())
    }

    @Test
    fun `repository error is passed through unchanged`() = runTest {
        val repositoryError = Resource.Error("No pudimos leer las inspecciones guardadas.")
        val repository = FakeInspectionRepository(repositoryError)
        val useCase = GetInspectionHistoryUseCase(repository)

        val result = useCase().first()

        assertEquals(repositoryError, result)
    }

    private suspend fun getHistoryOrFail(useCase: GetInspectionHistoryUseCase): InspectionHistory {
        val result = useCase().first()
        if (result is Resource.Success) {
            return result.data
        }
        throw AssertionError("Se esperaba Resource.Success y llegó: $result")
    }
}
