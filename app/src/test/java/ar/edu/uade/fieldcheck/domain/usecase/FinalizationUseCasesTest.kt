package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.model.InspectionStatus
import ar.edu.uade.fieldcheck.domain.model.ItemResultValue
import ar.edu.uade.fieldcheck.fakes.FakeInspectionRepository
import ar.edu.uade.fieldcheck.fakes.FinalizedInspection
import ar.edu.uade.fieldcheck.fakes.createTestClock
import ar.edu.uade.fieldcheck.fakes.createTestDetail
import ar.edu.uade.fieldcheck.fakes.createTestEvidence
import ar.edu.uade.fieldcheck.fakes.createTestInspection
import ar.edu.uade.fieldcheck.fakes.createTestItemResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinalizationUseCasesTest {

    private val checkFinalization = CheckFinalizationUseCase()

    @Test
    fun `check lists items without result and not complies without evidence`() {
        val items = listOf(
            createTestItemResult(0, ItemResultValue.COMPLIES),
            createTestItemResult(1, null),
            createTestItemResult(2, ItemResultValue.NOT_COMPLIES, note = "   "),
            createTestItemResult(3, ItemResultValue.NOT_COMPLIES, note = "Sin tapa"),
            createTestItemResult(4, ItemResultValue.NOT_COMPLIES, evidence = listOf(createTestEvidence())),
        )

        val check = checkFinalization(items)

        assertEquals(listOf(1), check.itemsWithoutResult.map { item -> item.position })
        assertEquals(listOf(2), check.notCompliesWithoutEvidence.map { item -> item.position })
        assertEquals(2, check.missingCount())
        assertEquals(listOf(1, 2), check.missingItems().map { item -> item.position })
        assertFalse(check.canFinalize())
    }

    @Test
    fun `check allows finalizing when everything is complete`() {
        val items = listOf(
            createTestItemResult(0, ItemResultValue.COMPLIES),
            createTestItemResult(1, ItemResultValue.NOT_APPLICABLE),
            createTestItemResult(2, ItemResultValue.NOT_COMPLIES, note = "Falta matafuego"),
        )

        assertTrue(checkFinalization(items).canFinalize())
    }

    @Test
    fun `finalize refuses and explains how many items are missing`() = runTest {
        val repository = FakeInspectionRepository()
        val detail = createTestDetail(
            items = listOf(
                createTestItemResult(0, null),
                createTestItemResult(1, ItemResultValue.NOT_COMPLIES),
            ),
        )
        repository.emitDetail("inspection-1", Resource.Success(detail))
        val useCase = FinalizeInspectionUseCase(repository, checkFinalization, createTestClock(10_000L))

        val result = useCase("inspection-1")

        assertEquals(Resource.Error("Faltan 2 ítems para poder finalizar."), result)
        assertTrue(repository.finalizedInspections.isEmpty())
    }

    @Test
    fun `finalize saves the finish time when everything is complete`() = runTest {
        val repository = FakeInspectionRepository()
        val detail = createTestDetail(items = listOf(createTestItemResult(0, ItemResultValue.COMPLIES)))
        repository.emitDetail("inspection-1", Resource.Success(detail))
        val useCase = FinalizeInspectionUseCase(repository, checkFinalization, createTestClock(10_000L))

        val result = useCase("inspection-1")

        assertEquals(Resource.Success(Unit), result)
        assertEquals(listOf(FinalizedInspection("inspection-1", 10_000L)), repository.finalizedInspections)
    }

    @Test
    fun `finalize uses the singular when only one item is missing`() = runTest {
        val repository = FakeInspectionRepository()
        val detail = createTestDetail(items = listOf(createTestItemResult(0, null)))
        repository.emitDetail("inspection-1", Resource.Success(detail))
        val useCase = FinalizeInspectionUseCase(repository, checkFinalization, createTestClock(10_000L))

        val result = useCase("inspection-1")

        assertEquals(Resource.Error("Falta 1 ítem para poder finalizar."), result)
    }

    @Test
    fun `finalize refuses an inspection that is already finished`() = runTest {
        val repository = FakeInspectionRepository()
        val finished = createTestInspection("inspection-1", status = InspectionStatus.FINISHED, finishedAt = 2_000L)
        val detail = createTestDetail(
            items = listOf(createTestItemResult(0, ItemResultValue.COMPLIES)),
            inspection = finished,
        )
        repository.emitDetail("inspection-1", Resource.Success(detail))
        val useCase = FinalizeInspectionUseCase(repository, checkFinalization, createTestClock(10_000L))

        val result = useCase("inspection-1")

        assertTrue(result is Resource.Error)
        assertTrue(repository.finalizedInspections.isEmpty())
    }

    @Test
    fun `finalize passes through the error when the inspection cannot be read`() = runTest {
        val repository = FakeInspectionRepository()
        val useCase = FinalizeInspectionUseCase(repository, checkFinalization, createTestClock(10_000L))

        val result = useCase("does-not-exist")

        assertEquals(Resource.Error("No encontramos esta inspección."), result)
    }
}
