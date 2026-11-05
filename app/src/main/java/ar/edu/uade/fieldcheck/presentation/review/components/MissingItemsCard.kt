package ar.edu.uade.fieldcheck.presentation.review.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/** Opacidad del fondo de la tarjeta: un rojo suave que no compite con el texto. */
private const val CARD_BACKGROUND_ALPHA = 0.1f

/**
 * Tarjeta de aviso de P4 cuando no se puede finalizar (Figma 46:52):
 * "Faltan 2 ítems para poder finalizar" + "Tocá cada uno para completarlo.".
 *
 * Es una región dinámica para que TalkBack la lea apenas aparece (criterio A6).
 */
@Composable
fun MissingItemsCard(
    message: String,
    modifier: Modifier = Modifier,
) {
    val warningColor = MaterialTheme.statusColors.notComplies
    val backgroundColor = warningColor.copy(alpha = CARD_BACKGROUND_ALPHA)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium,
        color = backgroundColor,
        border = BorderStroke(width = 1.dp, color = warningColor),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = warningColor,
                modifier = Modifier.size(24.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = warningColor,
                )
                Text(
                    text = stringResource(R.string.review_missing_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MissingItemsCardPreview() {
    FieldCheckTheme {
        MissingItemsCard(
            message = "Faltan 2 ítems para poder finalizar.",
            modifier = Modifier.padding(16.dp),
        )
    }
}
