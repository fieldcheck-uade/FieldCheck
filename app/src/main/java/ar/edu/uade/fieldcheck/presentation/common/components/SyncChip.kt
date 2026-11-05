package ar.edu.uade.fieldcheck.presentation.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.fieldcheck.R
import ar.edu.uade.fieldcheck.core.theme.FieldCheckTheme
import ar.edu.uade.fieldcheck.core.theme.statusColors
import ar.edu.uade.fieldcheck.domain.model.SyncStatus

/**
 * Chip con el estado de sincronización de una inspección finalizada (Figma 2:19).
 *
 * Cada estado lleva ícono + texto + color: el color nunca es el único portador
 * de significado (criterio de accesibilidad de docs/pantallas.md).
 */
@Composable
fun SyncChip(
    syncStatus: SyncStatus,
    modifier: Modifier = Modifier,
) {
    val icon: ImageVector
    val label: String
    val color: Color
    val borderColor: Color

    when (syncStatus) {
        SyncStatus.PENDING -> {
            icon = Icons.Outlined.Schedule
            label = stringResource(R.string.sync_pending)
            color = MaterialTheme.colorScheme.onSurfaceVariant
            // En Figma el borde de "Pendiente" es más claro que el texto (outline).
            borderColor = MaterialTheme.colorScheme.outline
        }

        SyncStatus.SYNCED -> {
            icon = Icons.Outlined.CloudDone
            label = stringResource(R.string.sync_synced)
            color = MaterialTheme.statusColors.complies
            borderColor = color
        }

        SyncStatus.FAILED -> {
            icon = Icons.Outlined.ErrorOutline
            label = stringResource(R.string.sync_failed)
            color = MaterialTheme.statusColors.notComplies
            borderColor = color
        }
    }

    StatusChip(
        icon = icon,
        label = label,
        color = color,
        borderColor = borderColor,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun SyncChipPreview() {
    FieldCheckTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SyncChip(syncStatus = SyncStatus.PENDING)
            SyncChip(syncStatus = SyncStatus.SYNCED)
            SyncChip(syncStatus = SyncStatus.FAILED)
        }
    }
}
