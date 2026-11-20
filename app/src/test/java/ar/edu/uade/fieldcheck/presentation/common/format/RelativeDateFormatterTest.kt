package ar.edu.uade.fieldcheck.presentation.common.format

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class RelativeDateFormatterTest {

    private val zone: ZoneId = ZoneId.of("America/Argentina/Buenos_Aires")
    private val now = millisOf(2026, 9, 25, 18, 0)

    @Test
    fun `same day shows Hoy and the time`() {
        assertEquals("Hoy, 10:05", RelativeDateFormatter.format(millisOf(2026, 9, 25, 10, 5), now, zone))
    }

    @Test
    fun `previous day shows Ayer and the time`() {
        assertEquals("Ayer, 16:30", RelativeDateFormatter.format(millisOf(2026, 9, 24, 16, 30), now, zone))
    }

    @Test
    fun `older dates of the same year show day and month`() {
        assertEquals("23/09, 11:12", RelativeDateFormatter.format(millisOf(2026, 9, 23, 11, 12), now, zone))
    }

    @Test
    fun `dates of another year include the year`() {
        assertEquals("23/09/2025, 11:12", RelativeDateFormatter.format(millisOf(2025, 9, 23, 11, 12), now, zone))
    }

    @Test
    fun `just after midnight yesterday is still Ayer`() {
        val justAfterMidnight = millisOf(2026, 9, 25, 0, 10)

        assertEquals("Ayer, 23:50", RelativeDateFormatter.format(millisOf(2026, 9, 24, 23, 50), justAfterMidnight, zone))
    }

    private fun millisOf(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        val dateTime = LocalDateTime.of(year, month, day, hour, minute)
        return dateTime.atZone(zone).toInstant().toEpochMilli()
    }
}
