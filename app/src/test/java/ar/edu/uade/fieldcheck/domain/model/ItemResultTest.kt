package ar.edu.uade.fieldcheck.domain.model

import ar.edu.uade.fieldcheck.fakes.createTestDetail
import ar.edu.uade.fieldcheck.fakes.createTestEvidence
import ar.edu.uade.fieldcheck.fakes.createTestItemResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemResultTest {

    @Test
    fun `a note or a photo counts as evidence, a blank note does not`() {
        assertTrue(createTestItemResult(0, note = "Sin tapa").hasEvidence())
        assertTrue(createTestItemResult(0, evidence = listOf(createTestEvidence())).hasEvidence())
        assertFalse(createTestItemResult(0, note = "  ").hasEvidence())
        assertFalse(createTestItemResult(0, note = null).hasEvidence())
    }

    @Test
    fun `only not complies items require evidence`() {
        val notCompliesWithout = createTestItemResult(0, ItemResultValue.NOT_COMPLIES)
        val compliesWithout = createTestItemResult(1, ItemResultValue.COMPLIES)

        assertTrue(notCompliesWithout.isNotCompliesWithoutEvidence())
        assertFalse(compliesWithout.isNotCompliesWithoutEvidence())
    }

    @Test
    fun `display number starts at one`() {
        assertEquals(7, createTestItemResult(6).displayNumber())
    }

    @Test
    fun `evidence has location only with both coordinates`() {
        assertTrue(createTestEvidence(latitude = -34.6, longitude = -58.4).hasLocation())
        assertFalse(createTestEvidence(latitude = -34.6, longitude = null).hasLocation())
    }

    @Test
    fun `counts each result and the pending items`() {
        val detail = createTestDetail(
            items = listOf(
                createTestItemResult(0, ItemResultValue.COMPLIES),
                createTestItemResult(1, ItemResultValue.COMPLIES),
                createTestItemResult(2, ItemResultValue.NOT_COMPLIES),
                createTestItemResult(3, ItemResultValue.NOT_APPLICABLE),
                createTestItemResult(4, null),
            ),
        )

        assertEquals(ResultCounts(complies = 2, notComplies = 1, notApplicable = 1, pending = 1), detail.countResults())
    }
}
