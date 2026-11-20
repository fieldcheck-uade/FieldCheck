package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.fakes.CreatedInspection
import ar.edu.uade.fieldcheck.fakes.FakeInspectionRepository
import ar.edu.uade.fieldcheck.fakes.createTestClock
import ar.edu.uade.fieldcheck.fakes.createTestTemplate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateInspectionUseCaseTest {

    private val template = createTestTemplate("control-obra", name = "Control semanal de obra")

    @Test
    fun `blank site name is rejected with Obligatorio and nothing is created`() = runTest {
        val repository = FakeInspectionRepository()
        val useCase = CreateInspectionUseCase(repository, createTestClock(5_000L))

        val result = useCase(template, "   ")

        assertEquals(Resource.Error("Obligatorio"), result)
        assertTrue(repository.createdInspections.isEmpty())
    }

    @Test
    fun `site name is trimmed and the current time is used`() = runTest {
        val repository = FakeInspectionRepository()
        val useCase = CreateInspectionUseCase(repository, createTestClock(5_000L))

        useCase(template, "  Obra Av. Siempreviva 742  ")

        val expected = CreatedInspection(template = template, siteName = "Obra Av. Siempreviva 742", now = 5_000L)
        assertEquals(listOf(expected), repository.createdInspections)
    }

    @Test
    fun `returns the id of the new inspection`() = runTest {
        val repository = FakeInspectionRepository()
        repository.createInspectionResult = Resource.Success("uuid-123")
        val useCase = CreateInspectionUseCase(repository, createTestClock(5_000L))

        val result = useCase(template, "Planta Norte")

        assertEquals(Resource.Success("uuid-123"), result)
    }
}
