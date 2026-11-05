package ar.edu.uade.fieldcheck.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.fieldcheck.R
import ar.edu.uade.fieldcheck.core.theme.FieldCheckTheme
import ar.edu.uade.fieldcheck.core.theme.statusColors

/**
 * Franja "Sin conexión" bajo la barra superior (Figma 2:83).
 *
 * No se puede cerrar: la pantalla la deja de mostrar sola cuando vuelve la red.
 * Uso típico: `if (!isOnline) { OfflineBanner() }`.
 *
 * Es una región dinámica ("live region") para que TalkBack anuncie el cambio
 * de conexión sin que el usuario tenga que buscarlo (criterio A6).
 */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    val statusColors = MaterialTheme.statusColors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .background(statusColors.offlineBanner)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
            },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = statusColors.onOfflineBanner,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(R.string.offline_banner),
            style = MaterialTheme.typography.bodyMedium,
            color = statusColors.onOfflineBanner,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OfflineBannerPreview() {
    FieldCheckTheme {
        OfflineBanner()
    }
}
