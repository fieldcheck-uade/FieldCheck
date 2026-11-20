package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.domain.model.Inspection
import ar.edu.uade.fieldcheck.domain.model.InspectionDetail
import ar.edu.uade.fieldcheck.domain.model.ItemResult
import ar.edu.uade.fieldcheck.domain.model.ItemResultValue
import ar.edu.uade.fieldcheck.domain.model.ResultCounts
import java.time.Clock
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Caso de uso: armar el texto del resumen para "Compartir resumen" en P5 (regla R7).
 *
 * Reglas (formato de docs/pantallas.md, P5):
 * - Texto plano, sin formato: se pega igual en WhatsApp, mail o un SMS.
 * - Incluye plantilla, sitio, fechas de inicio y fin, y el conteo de resultados.
 * - Lista los "No cumple" en orden con su nota, porque es lo que más le importa
 *   a quien recibe el informe. Si el ítem no tiene nota pero sí fotos, dice "(ver foto)".
 * - Las fotos no se adjuntan al texto en esta versión.
 *
 * El reloj se usa solo por su zona horaria: las fechas se muestran en la hora local del teléfono.
 */
class BuildInspectionSummaryUseCase @Inject constructor(
    private val clock: Clock,
) {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    operator fun invoke(detail: InspectionDetail): String {
        val inspection = detail.inspection
        val lines = mutableListOf<String>()

        lines.add("Inspección: ${inspection.templateName}")
        lines.add("Sitio: ${inspection.siteName}")
        lines.add("Fecha: ${formatDateRange(inspection)}")
        lines.add("Resultado: ${formatCounts(detail.countResults())}")

        val notCompliesItems = findNotCompliesItems(detail.items)
        if (notCompliesItems.isNotEmpty()) {
            lines.add("")
            lines.add("No cumple:")
            for (item in notCompliesItems) {
                lines.add(formatNotCompliesItem(item))
            }
        }

        lines.add("")
        lines.add("Generado con FieldCheck")

        return lines.joinToString(separator = "\n")
    }

    /**
     * "25/09/2026 10:05 – 10:48" si empezó y terminó el mismo día;
     * si no, las dos fechas completas. Sin fecha de fin, solo la de inicio.
     */
    private fun formatDateRange(inspection: Inspection): String {
        val start = toLocalDateTime(inspection.startedAt)
        val startText = dateTimeFormatter.format(start)

        val finishedAt = inspection.finishedAt
        if (finishedAt == null) {
            return startText
        }

        val end = toLocalDateTime(finishedAt)
        val isSameDay = start.toLocalDate() == end.toLocalDate()
        if (isSameDay) {
            return "$startText – ${timeFormatter.format(end)}"
        }
        return "$startText – ${dateTimeFormatter.format(end)}"
    }

    /** "15 cumple · 3 no cumple · 1 N/A"; los pendientes solo aparecen si hay alguno. */
    private fun formatCounts(counts: ResultCounts): String {
        var text = "${counts.complies} cumple · ${counts.notComplies} no cumple · ${counts.notApplicable} N/A"
        if (counts.pending > 0) {
            text = "$text · ${counts.pending} sin completar"
        }
        return text
    }

    private fun findNotCompliesItems(items: List<ItemResult>): List<ItemResult> {
        val notCompliesItems = mutableListOf<ItemResult>()
        for (item in items) {
            if (item.result == ItemResultValue.NOT_COMPLIES) {
                notCompliesItems.add(item)
            }
        }
        return notCompliesItems.sortedBy { item -> item.position }
    }

    /** "• 7. Matafuegos señalizados y con carga vigente — Falta señalización..." */
    private fun formatNotCompliesItem(item: ItemResult): String {
        val title = "• ${item.displayNumber()}. ${item.itemText}"

        val note = item.note
        if (note != null && note.isNotBlank()) {
            return "$title — ${note.trim()}"
        }
        if (item.evidence.isNotEmpty()) {
            return "$title — (ver foto)"
        }
        return title
    }

    private fun toLocalDateTime(epochMillis: Long): ZonedDateTime {
        val instant = Instant.ofEpochMilli(epochMillis)
        return instant.atZone(clock.zone)
    }
}
