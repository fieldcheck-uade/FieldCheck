package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.domain.model.InspectionStatus
import ar.edu.uade.fieldcheck.domain.model.ItemResultValue
import ar.edu.uade.fieldcheck.domain.model.SyncStatus
import ar.edu.uade.fieldcheck.fakes.createTestClock
import ar.edu.uade.fieldcheck.fakes.createTestDetail
import ar.edu.uade.fieldcheck.fakes.createTestEvidence
import ar.edu.uade.fieldcheck.fakes.createTestInspection
import ar.edu.uade.fieldcheck.fakes.createTestItemResult
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class BuildInspectionSummaryUseCaseTest {

    // El reloj de test está en UTC, así que las fechas se arman en UTC.
    private val useCase = BuildInspectionSummaryUseCase(createTestClock(0L))

    @Test
    fun `builds the plain text summary with counts and not complies items`() {
        val detail = createTestDetail(
            inspection = createTestInspection(
                clientUuid = "inspection-1",
                status = InspectionStatus.FINISHED,
                syncStatus = SyncStatus.PENDING,
                siteName = "Obra Av. Siempreviva 742 — Subsuelo 2",
                templateName = "Control semanal de obra",
                startedAt = utcMillis(2026, 9, 25, 10, 5),
                finishedAt = utcMillis(2026, 9, 25, 10, 48),
            ),
            items = listOf(
                createTestItemResult(0, ItemResultValue.COMPLIES),
                createTestItemResult(
                    position = 6,
                    result = ItemResultValue.NOT_COMPLIES,
                    note = "Falta señalización en el matafuego del palier.",
                    itemText = "Matafuegos señalizados y con carga vigente",
                ),
                createTestItemResult(2, ItemResultValue.NOT_APPLICABLE),
                createTestItemResult(
                    position = 17,
                    result = ItemResultValue.NOT_COMPLIES,
                    evidence = listOf(createTestEvidence()),
                    itemText = "Vallado perimetral completo",
                ),
            ),
        )

        val summary = useCase(detail)

        val expected = """
            Inspección: Control semanal de obra
            Sitio: Obra Av. Siempreviva 742 — Subsuelo 2
            Fecha: 25/09/2026 10:05 – 10:48
            Resultado: 1 cumple · 2 no cumple · 1 N/A

            No cumple:
            • 7. Matafuegos señalizados y con carga vigente — Falta señalización en el matafuego del palier.
            • 18. Vallado perimetral completo — (ver foto)

            Generado con FieldCheck
        """.trimIndent()
        assertEquals(expected, summary)
    }

    @Test
    fun `without not complies items the section is omitted and different days show both dates`() {
        val detail = createTestDetail(
            inspection = createTestInspection(
                clientUuid = "inspection-1",
                status = InspectionStatus.FINISHED,
                siteName = "Planta Norte",
                templateName = "Sala técnica",
                startedAt = utcMillis(2026, 9, 25, 23, 50),
                finishedAt = utcMillis(2026, 9, 26, 0, 10),
            ),
            items = listOf(createTestItemResult(0, ItemResultValue.COMPLIES)),
        )

        val summary = useCase(detail)

        val expected = """
            Inspección: Sala técnica
            Sitio: Planta Norte
            Fecha: 25/09/2026 23:50 – 26/09/2026 00:10
            Resultado: 1 cumple · 0 no cumple · 0 N/A

            Generado con FieldCheck
        """.trimIndent()
        assertEquals(expected, summary)
    }

    private fun utcMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        val dateTime = LocalDateTime.of(year, month, day, hour, minute)
        return dateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
    }
}
