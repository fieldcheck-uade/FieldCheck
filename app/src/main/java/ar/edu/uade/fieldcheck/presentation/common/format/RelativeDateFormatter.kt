package ar.edu.uade.fieldcheck.presentation.common.format

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Formatea fechas como en el historial de Figma (P1): "Hoy, 10:05", "Ayer, 16:30", "23/09, 11:12".
 *
 * Recibe "ahora" y la zona horaria como parámetros en lugar de leerlos del sistema:
 * así se puede testear con fechas fijas y el resultado no depende del día en que corre el test.
 */
object RelativeDateFormatter {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dayAndMonthFormatter = DateTimeFormatter.ofPattern("dd/MM")
    private val fullDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /**
     * - Mismo día que [nowMillis]: "Hoy, HH:mm".
     * - Día anterior: "Ayer, HH:mm".
     * - Mismo año: "dd/MM, HH:mm".
     * - Otro año: "dd/MM/yyyy, HH:mm", para no confundir septiembre de este año con el del anterior.
     */
    fun format(epochMillis: Long, nowMillis: Long, zoneId: ZoneId): String {
        val dateTime = toZonedDateTime(epochMillis, zoneId)
        val today = toZonedDateTime(nowMillis, zoneId).toLocalDate()
        val date = dateTime.toLocalDate()
        val time = timeFormatter.format(dateTime)

        val dayText = buildDayText(date, today)
        return "$dayText, $time"
    }

    private fun buildDayText(date: LocalDate, today: LocalDate): String {
        if (date == today) {
            return "Hoy"
        }

        val yesterday = today.minusDays(1)
        if (date == yesterday) {
            return "Ayer"
        }

        if (date.year == today.year) {
            return dayAndMonthFormatter.format(date)
        }
        return fullDateFormatter.format(date)
    }

    private fun toZonedDateTime(epochMillis: Long, zoneId: ZoneId): ZonedDateTime {
        val instant = Instant.ofEpochMilli(epochMillis)
        return instant.atZone(zoneId)
    }
}
