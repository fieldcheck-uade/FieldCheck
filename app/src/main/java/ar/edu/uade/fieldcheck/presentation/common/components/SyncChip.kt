package ar.edu.uade.fieldcheck.presentation.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ar.edu.uade.fieldcheck.R
import ar.edu.uade.fieldcheck.core.theme.statusColors
import ar.edu.uade.fieldcheck.domain.model.SyncStatus

/**
 * Chip con el estado de sincronización de una inspección finalizada.
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

    when (syncStatus) {
        SyncStatus.PENDING -> {
            icon = Icons.Outlined.Schedule
            label = stringResource(R.string.sync_pending)
            color = MaterialTheme.colorScheme.onSurfaceVariant
        }

        SyncStatus.SYNCED -> {
            icon = Icons.Outlined.CloudDone
            label = stringResource(R.string.sync_synced)
            color = MaterialTheme.statusColors.complies
        }

        SyncStatus.FAILED -> {
            icon = Icons.Outlined.ErrorOutline
            label = stringResource(R.string.sync_failed)
            color = MaterialTheme.statusColors.notComplies
        }
    }

    Row(
        modifier = modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
    }
}
