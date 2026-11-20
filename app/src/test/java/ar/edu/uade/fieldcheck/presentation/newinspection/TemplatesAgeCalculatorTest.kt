package ar.edu.uade.fieldcheck.presentation.newinspection

import ar.edu.uade.fieldcheck.fakes.createTestTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class TemplatesAgeCalculatorTest {

    private val zone = ZoneOffset.UTC

    /** "Ahora" fijo: 25/09/2026 18:00 UTC. */
    private val now = toMillis(LocalDateTime.of(2026, 9, 25, 18, 0))

    @Test
    fun `templates downloaded today show today with the time`() {
        val template = createTestTemplate(id = "1", downloadedAt = toMillis(LocalDateTime.of(2026, 9, 25, 9, 14)))

        val age = TemplatesAgeCalculator.calculate(listOf(template), now, zone)

        assertEquals(TemplatesAge.Today("09:14"), age)
    }

    @Test
    fun `templates downloaded yesterday show yesterday with the time`() {
        val template = createTestTemplate(id = "1", downloadedAt = toMillis(LocalDateTime.of(2026, 9, 24, 23, 30)))

        val age = TemplatesAgeCalculator.calculate(listOf(template), now, zone)

        assertEquals(TemplatesAge.Yesterday("23:30"), age)
    }

    @Test
    fun `older templates show the number of days using the most recent download`() {
        val older = createTestTemplate(id = "1", downloadedAt = toMillis(LocalDateTime.of(2026, 9, 10, 8, 0)))
        val newer = createTestTemplate(id = "2", downloadedAt = toMillis(LocalDateTime.of(2026, 9, 22, 8, 0)))

        val age = TemplatesAgeCalculator.calculate(listOf(older, newer), now, zone)

        assertEquals(TemplatesAge.DaysAgo(3), age)
    }

    @Test
    fun `no templates means no age line`() {
        val age = TemplatesAgeCalculator.calculate(emptyList(), now, zone)

        assertNull(age)
    }

    private fun toMillis(dateTime: LocalDateTime): Long {
        return dateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
    }
}
