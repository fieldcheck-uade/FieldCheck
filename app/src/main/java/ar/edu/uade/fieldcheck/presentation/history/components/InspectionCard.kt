package ar.edu.uade.fieldcheck.presentation.history.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ar.edu.uade.fieldcheck.R
import ar.edu.uade.fieldcheck.domain.model.Inspection
import ar.edu.uade.fieldcheck.presentation.common.components.SyncChip
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

/**
 * Tarjeta del historial (docs/pantallas.md, componente `InspectionCard`).
 *
 * - En curso: muestra una barra de progreso "n de N ítems".
 * - Finalizada: muestra el estado de sincronización con [SyncChip].
 * Toda la tarjeta es tocable.
 */
@Composable
fun InspectionCard(
    inspection: Inspection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
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
                text = inspection.templateName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatDate(inspection.startedAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (inspection.isFinished()) {
                SyncChip(syncStatus = inspection.syncStatus)
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

    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    )
    Text(
        text = stringResource(R.string.history_progress, completedItems, totalItems),
        style = MaterialTheme.typography.bodyMedium,
    )
}

private fun formatDate(epochMillis: Long): String {
    val instant = Instant.ofEpochMilli(epochMillis)
    val localDateTime = instant.atZone(ZoneId.systemDefault())
    return dateTimeFormatter.format(localDateTime)
}
