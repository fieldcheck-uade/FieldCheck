package ar.edu.uade.fieldcheck.presentation.history.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ar.edu.uade.fieldcheck.R
import ar.edu.uade.fieldcheck.domain.model.Inspection
import ar.edu.uade.fieldcheck.presentation.common.components.SyncChip
import ar.edu.uade.fieldcheck.presentation.common.format.RelativeDateFormatter
import java.time.ZoneId

/**
 * Tarjeta del historial (docs/pantallas.md, componente `InspectionCard`; Figma 3:2).
 *
 * - Título: el sitio. Subtítulo: "Plantilla · Hoy, 10:05".
 * - En curso: "7 de 20 ítems" en color primario sobre una barra de progreso de 6 dp.
 * - Finalizada: el estado de sincronización con [SyncChip].
 * Toda la tarjeta es tocable.
 *
 * @param nowMillis "ahora", para decidir si la fecha es "Hoy" o "Ayer".
 *   Se recibe como parámetro para que los previews muestren siempre lo mismo.
 */
@Composable
fun InspectionCard(
    inspection: Inspection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val subtitle = stringResource(
        R.string.history_card_subtitle,
        inspection.templateName,
        formatCardDate(inspection, nowMillis),
    )

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = inspection.siteName,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (inspection.isFinished()) {
                SyncChip(
                    syncStatus = inspection.syncStatus,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                InspectionProgress(
                    completedItems = inspection.completedItems,
                    totalItems = inspection.totalItems,
                )
            }
        }
    }
}

@Composable
private fun InspectionProgress(
    completedItems: Int,
    totalItems: Int,
) {
    val progress: Float
    if (totalItems == 0) {
        progress = 0f
    } else {
        progress = completedItems.toFloat() / totalItems.toFloat()
    }

    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.history_progress, completedItems, totalItems),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainer,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

/** En curso se muestra cuándo empezó; finalizada, cuándo terminó. */
private fun formatCardDate(inspection: Inspection, nowMillis: Long): String {
    val dateToShow: Long
    val finishedAt = inspection.finishedAt
    if (inspection.isFinished() && finishedAt != null) {
        dateToShow = finishedAt
    } else {
        dateToShow = inspection.startedAt
    }

    return RelativeDateFormatter.format(
        epochMillis = dateToShow,
        nowMillis = nowMillis,
        zoneId = ZoneId.systemDefault(),
    )
}
