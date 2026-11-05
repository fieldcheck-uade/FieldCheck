package ar.edu.uade.fieldcheck.presentation.newinspection

import ar.edu.uade.fieldcheck.domain.model.Template
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Antigüedad de la caché de plantillas, para la línea "Plantillas actualizadas hoy, 09:14" de P2.
 *
 * Es un modelo y no un texto: la pantalla elige el string traducido según el caso.
 * Así el cálculo de días se puede testear en la JVM sin recursos de Android.
 */
sealed interface TemplatesAge {

    /** Se descargaron hoy; [time] es la hora "HH:mm". */
    data class Today(val time: String) : TemplatesAge

    /** Se descargaron ayer; [time] es la hora "HH:mm". */
    data class Yesterday(val time: String) : TemplatesAge

    /** Se descargaron hace [days] días (2 o más). */
    data class DaysAgo(val days: Int) : TemplatesAge
}

object TemplatesAgeCalculator {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Calcula la antigüedad a partir de la descarga más reciente.
     *
     * Usamos la más reciente porque las plantillas se descargan juntas: la última fecha
     * es la de la última actualización exitosa. Devuelve `null` si no hay plantillas,
     * porque en ese caso la pantalla muestra el estado vacío y no la línea de antigüedad.
     */
    fun calculate(templates: List<Template>, nowMillis: Long, zoneId: ZoneId): TemplatesAge? {
        if (templates.isEmpty()) {
            return null
        }

        val lastDownloadMillis = findLastDownload(templates)
        val downloadDateTime = Instant.ofEpochMilli(lastDownloadMillis).atZone(zoneId)
        val downloadDate = downloadDateTime.toLocalDate()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val daysBetween = ChronoUnit.DAYS.between(downloadDate, today).toInt()
        val time = downloadDateTime.format(timeFormatter)

        return buildAge(daysBetween, time)
    }

    private fun findLastDownload(templates: List<Template>): Long {
        var lastDownload = templates[0].downloadedAt
        for (template in templates) {
            if (template.downloadedAt > lastDownload) {
                lastDownload = template.downloadedAt
            }
        }
        return lastDownload
    }

    private fun buildAge(daysBetween: Int, time: String): TemplatesAge {
        // Una fecha "en el futuro" (reloj del teléfono atrasado) se trata como "hoy"
        // para no mostrar un texto absurdo como "hace -1 días".
        if (daysBetween <= 0) {
            return TemplatesAge.Today(time)
        }
        if (daysBetween == 1) {
            return TemplatesAge.Yesterday(time)
        }
        return TemplatesAge.DaysAgo(daysBetween)
    }
}
