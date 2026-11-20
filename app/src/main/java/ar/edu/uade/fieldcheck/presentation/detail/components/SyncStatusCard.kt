package ar.edu.uade.fieldcheck.presentation.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.fieldcheck.R
import ar.edu.uade.fieldcheck.core.theme.FieldCheckTheme
import ar.edu.uade.fieldcheck.core.theme.statusColors
import ar.edu.uade.fieldcheck.domain.model.SyncStatus

/**
 * Transparencia del fondo de la tarjeta en los estados "Sincronizada" y "Error".
 *
 * El tema no tiene un color "contenedor" para cumple / no cumple, así que usamos el
 * mismo color del estado bien suave. Así funciona igual en modo claro y oscuro.
 */
private const val STATUS_BACKGROUND_ALPHA = 0.12f

/**
 * Cómo se ve la tarjeta en cada estado: ícono, colores y textos.
 * Lo separamos para que [SyncStatusCard] tenga un único `when` fácil de leer.
 */
private data class SyncCardStyle(
    val icon: ImageVector,
    val iconColor: Color,
    val titleColor: Color,
    val backgroundColor: Color,
    val title: String,
    val description: String,
)

/**
 * Tarjeta de sincronización de P5 (Figma 5:169, 46:53 y 46:54).
 *
 * - Pendiente: reloj + "Pendiente de subir. Se sube sola cuando haya conexión."
 * - Sincronizada: nube con tilde + "Sincronizada con el servidor."
 * - Error: admiración + motivo en lenguaje simple + botón "Reintentar".
 *
 * Es una región dinámica: TalkBack anuncia el cambio de estado (criterio A6).
 *
 * @param lastSyncError motivo del error; si falta se muestra un texto genérico.
 * @param onRetryClick solo se usa en el estado de error.
 */
@Composable
fun SyncStatusCard(
    syncStatus: SyncStatus,
    lastSyncError: String?,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = syncCardStyleOf(syncStatus, lastSyncError)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = style.backgroundColor, shape = MaterialTheme.shapes.medium)
            .padding(16.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = style.icon,
            contentDescription = null,
            tint = style.iconColor,
            modifier = Modifier.size(22.dp),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = style.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = style.titleColor,
            )
            Text(
                text = style.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            val showRetry = syncStatus == SyncStatus.FAILED
            if (showRetry) {
                RetryButton(
                    onClick = onRetryClick,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun syncCardStyleOf(syncStatus: SyncStatus, lastSyncError: String?): SyncCardStyle {
    val statusColors = MaterialTheme.statusColors

    when (syncStatus) {
        SyncStatus.PENDING -> {
            return SyncCardStyle(
                icon = Icons.Outlined.Schedule,
                iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                titleColor = MaterialTheme.colorScheme.onSurface,
                backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                title = stringResource(R.string.detail_sync_pending_title),
                description = stringResource(R.string.detail_sync_pending_description),
            )
        }

        SyncStatus.SYNCED -> {
            return SyncCardStyle(
                icon = Icons.Outlined.CloudDone,
                iconColor = statusColors.complies,
                titleColor = statusColors.complies,
                backgroundColor = statusColors.complies.copy(alpha = STATUS_BACKGROUND_ALPHA),
                title = stringResource(R.string.detail_sync_synced_title),
                description = stringResource(R.string.detail_sync_synced_description),
            )
        }

        SyncStatus.FAILED -> {
            val reason = chooseErrorReason(lastSyncError)
            return SyncCardStyle(
                icon = Icons.Outlined.ErrorOutline,
                iconColor = statusColors.notComplies,
                titleColor = statusColors.notComplies,
                backgroundColor = statusColors.notComplies.copy(alpha = STATUS_BACKGROUND_ALPHA),
                title = stringResource(R.string.detail_sync_failed_title),
                description = reason,
            )
        }
    }
}

/** Si la base no guardó el motivo, mostramos uno genérico para no dejar la tarjeta vacía. */
@Composable
private fun chooseErrorReason(lastSyncError: String?): String {
    if (lastSyncError == null || lastSyncError.isBlank()) {
        return stringResource(R.string.detail_sync_failed_default_reason)
    }
    return lastSyncError
}

/** Botón contorneado "Reintentar" del color de error, como en Figma 46:53. */
@Composable
private fun RetryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val errorColor = MaterialTheme.statusColors.notComplies

    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        border = BorderStroke(width = 1.dp, color = errorColor),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = errorColor),
    ) {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.action_retry),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SyncStatusCardPreview() {
    FieldCheckTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SyncStatusCard(syncStatus = SyncStatus.PENDING, lastSyncError = null, onRetryClick = {})
            SyncStatusCard(syncStatus = SyncStatus.SYNCED, lastSyncError = null, onRetryClick = {})
            SyncStatusCard(
                syncStatus = SyncStatus.FAILED,
                lastSyncError = "El servidor no respondió después de varios intentos.",
                onRetryClick = {},
            )
        }
    }
}
