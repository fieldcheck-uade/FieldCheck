package ar.edu.uade.fieldcheck.data.local.files

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoResizeCalculatorTest {

    @Test
    fun `a 12 MP photo is decoded at half size and then scaled to 1600 px`() {
        // 4000 × 3000: a la mitad queda 2000 (todavía ≥ 1600); a un cuarto quedaría 1000 (muy chica).
        assertEquals(2, PhotoResizeCalculator.sampleSize(4000, 3000))
        assertEquals(Pair(1600, 1200), PhotoResizeCalculator.targetSize(2000, 1500))
    }

    @Test
    fun `a portrait photo limits its height`() {
        assertEquals(Pair(1200, 1600), PhotoResizeCalculator.targetSize(3000, 4000))
    }

    @Test
    fun `a very large photo uses a bigger sample size`() {
        // 8000 px: 8000/4 = 2000 ≥ 1600, 8000/8 = 1000 < 1600.
        assertEquals(4, PhotoResizeCalculator.sampleSize(8000, 6000))
    }

    @Test
    fun `a small photo is never enlarged`() {
        assertEquals(1, PhotoResizeCalculator.sampleSize(1280, 960))
        assertEquals(Pair(1280, 960), PhotoResizeCalculator.targetSize(1280, 960))
    }
}
