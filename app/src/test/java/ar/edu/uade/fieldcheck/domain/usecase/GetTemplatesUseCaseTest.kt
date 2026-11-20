package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.fakes.FakeTemplateRepository
import ar.edu.uade.fieldcheck.fakes.createTestTemplate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetTemplatesUseCaseTest {

    @Test
    fun `templates are sorted by name ignoring upper and lower case`() = runTest {
        val repository = FakeTemplateRepository(
            Resource.Success(
                listOf(
                    createTestTemplate("1", name = "Tableros eléctricos"),
                    createTestTemplate("2", name = "control semanal de obra"),
                    createTestTemplate("3", name = "Sala técnica"),
                    createTestTemplate("4", name = "Seguridad e higiene"),
                )
            )
        )
        val useCase = GetTemplatesUseCase(repository)

        val result = useCase().first() as Resource.Success

        val names = result.data.map { template -> template.name }
        assertEquals(
            listOf("control semanal de obra", "Sala técnica", "Seguridad e higiene", "Tableros eléctricos"),
            names,
        )
    }

    @Test
    fun `repository error is passed through unchanged`() = runTest {
        val error = Resource.Error("No pudimos leer las plantillas guardadas.")
        val useCase = GetTemplatesUseCase(FakeTemplateRepository(error))

        assertEquals(error, useCase().first())
    }
}
