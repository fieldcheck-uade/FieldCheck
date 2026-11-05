package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.domain.model.ItemResultValue
import ar.edu.uade.fieldcheck.fakes.createTestItemResult
import org.junit.Assert.assertEquals
import org.junit.Test

class GetStartPositionUseCaseTest {

    private val useCase = GetStartPositionUseCase()

    @Test
    fun `opens on the first item without result`() {
        val items = listOf(
            createTestItemResult(0, ItemResultValue.COMPLIES),
            createTestItemResult(1, ItemResultValue.NOT_APPLICABLE),
            createTestItemResult(2, null),
            createTestItemResult(3, null),
        )

        assertEquals(2, useCase(items))
    }

    @Test
    fun `opens on the last item when every item is complete`() {
        val items = listOf(
            createTestItemResult(0, ItemResultValue.COMPLIES),
            createTestItemResult(1, ItemResultValue.NOT_COMPLIES),
            createTestItemResult(2, ItemResultValue.COMPLIES),
        )

        assertEquals(2, useCase(items))
    }

    @Test
    fun `a requested position that exists wins over the rule`() {
        val items = listOf(
            createTestItemResult(0, null),
            createTestItemResult(1, ItemResultValue.COMPLIES),
        )

        assertEquals(1, useCase(items, requestedPosition = 1))
    }

    @Test
    fun `a requested position that does not exist falls back to the rule`() {
        val items = listOf(
            createTestItemResult(0, ItemResultValue.COMPLIES),
            createTestItemResult(1, null),
        )

        assertEquals(1, useCase(items, requestedPosition = 99))
    }

    @Test
    fun `an inspection without items starts at position zero`() {
        assertEquals(0, useCase(emptyList()))
    }
}
