package ar.edu.uade.fieldcheck.presentation.common.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import ar.edu.uade.fieldcheck.R

/**
 * Botón de la barra superior para pasar de modo claro a oscuro y viceversa.
 *
 * El ícono muestra el modo al que se va a pasar (luna en claro, sol en oscuro) y la
 * descripción para TalkBack dice la acción, no el estado actual. `IconButton` ya cumple
 * el área táctil mínima de 48 dp.
 */
@Composable
fun DarkModeToggleButton(
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon: ImageVector
    val description: String
    if (isDarkMode) {
        icon = Icons.Filled.LightMode
        description = stringResource(R.string.action_enable_light_mode)
    } else {
        icon = Icons.Filled.DarkMode
        description = stringResource(R.string.action_enable_dark_mode)
    }

    IconButton(
        onClick = { onDarkModeChange(!isDarkMode) },
        modifier = modifier,
    ) {
        Icon(imageVector = icon, contentDescription = description)
    }
}
