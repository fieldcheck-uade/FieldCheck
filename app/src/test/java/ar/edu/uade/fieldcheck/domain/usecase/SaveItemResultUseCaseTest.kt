package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.model.ItemResultValue
import ar.edu.uade.fieldcheck.fakes.FakeInspectionRepository
import ar.edu.uade.fieldcheck.fakes.SavedItemResult
import ar.edu.uade.fieldcheck.fakes.createTestClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveItemResultUseCaseTest {

    @Test
    fun `saves result and note immediately with the current time`() = runTest {
        val repository = FakeInspectionRepository()
        val useCase = SaveItemResultUseCase(repository, createTestClock(7_000L))

        val result = useCase(itemResultId = 3L, result = ItemResultValue.NOT_COMPLIES, note = "Falta señalización")

        assertEquals(Resource.Success(Unit), result)
        val expected = SavedItemResult(3L, ItemResultValue.NOT_COMPLIES, "Falta señalización", 7_000L)
        assertEquals(listOf(expected), repository.savedItemResults)
    }

    @Test
    fun `a blank note is saved as null`() = runTest {
        val repository = FakeInspectionRepository()
        val useCase = SaveItemResultUseCase(repository, createTestClock(7_000L))

        useCase(itemResultId = 3L, result = ItemResultValue.COMPLIES, note = "   ")

        assertEquals(null, repository.savedItemResults.single().note)
    }

    @Test
    fun `clearing the result is allowed`() = runTest {
        val repository = FakeInspectionRepository()
        val useCase = SaveItemResultUseCase(repository, createTestClock(7_000L))

        useCase(itemResultId = 3L, result = null, note = null)

        assertEquals(null, repository.savedItemResults.single().result)
    }

    @Test
    fun `repository error is returned`() = runTest {
        val repository = FakeInspectionRepository()
        repository.writeResult = Resource.Error("No pudimos guardar el cambio.")
        val useCase = SaveItemResultUseCase(repository, createTestClock(7_000L))

        val result = useCase(itemResultId = 3L, result = ItemResultValue.COMPLIES, note = null)

        assertEquals(Resource.Error("No pudimos guardar el cambio."), result)
    }
}
