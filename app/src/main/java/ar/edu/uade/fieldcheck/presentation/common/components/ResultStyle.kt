package ar.edu.uade.fieldcheck.presentation.common.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import ar.edu.uade.fieldcheck.R
import ar.edu.uade.fieldcheck.core.theme.statusColors
import ar.edu.uade.fieldcheck.domain.model.ItemResultValue

/**
 * Cómo se ve un resultado: ícono, texto y colores.
 *
 * @property color color del resultado (texto, ícono y borde; fondo cuando está seleccionado).
 * @property onColor color del contenido cuando el fondo es [color] (botón seleccionado).
 */
data class ResultStyle(
    val icon: ImageVector,
    val label: String,
    val color: Color,
    val onColor: Color,
)

/**
 * Devuelve el estilo de un resultado; `null` es "Sin completar".
 *
 * Está en un solo lugar para que ResultButton, CountChip e ItemResultLabel usen siempre
 * el mismo ícono, texto y color: un resultado nunca se comunica solo con color (criterio A3).
 */
@Composable
fun resultStyleOf(result: ItemResultValue?): ResultStyle {
    val statusColors = MaterialTheme.statusColors

    when (result) {
        ItemResultValue.COMPLIES -> {
            return ResultStyle(
                icon = Icons.Filled.Check,
                label = stringResource(R.string.result_complies),
                color = statusColors.complies,
                onColor = statusColors.onComplies,
            )
        }

        ItemResultValue.NOT_COMPLIES -> {
            return ResultStyle(
                icon = Icons.Filled.Close,
                label = stringResource(R.string.result_not_complies),
                color = statusColors.notComplies,
                onColor = statusColors.onNotComplies,
            )
        }

        ItemResultValue.NOT_APPLICABLE -> {
            return ResultStyle(
                icon = Icons.Filled.Remove,
                label = stringResource(R.string.result_not_applicable),
                color = statusColors.notApplicable,
                onColor = statusColors.onNotApplicable,
            )
        }

        null -> {
            return ResultStyle(
                icon = Icons.Outlined.RadioButtonUnchecked,
                label = stringResource(R.string.result_pending),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                onColor = MaterialTheme.colorScheme.surface,
            )
        }
    }
}
