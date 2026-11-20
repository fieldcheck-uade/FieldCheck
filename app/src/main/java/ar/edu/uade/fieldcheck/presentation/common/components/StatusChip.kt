package ar.edu.uade.fieldcheck.presentation.common.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Chip de solo lectura con contorno: ícono + texto, ambos del mismo color (Figma 2:19).
 *
 * Es la base visual de [SyncChip] y [CountChip]: 32 dp de alto, esquinas de 8 dp,
 * borde de 1 dp y 12 dp de margen horizontal. No es tocable, por eso no usa `AssistChip`.
 *
 * @param borderColor color del borde; por defecto el mismo que el texto.
 */
@Composable
fun StatusChip(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    borderColor: Color = color,
) {
    val shape = RoundedCornerShape(8.dp)

    Row(
        modifier = modifier
            .height(32.dp)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .padding(horizontal = 12.dp)
            // Ícono y texto se leen juntos en TalkBack: "Cumple 16".
            .semantics(mergeDescendants = true) {},
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
