package ar.edu.uade.fieldcheck.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.fieldcheck.core.theme.FieldCheckTheme
import ar.edu.uade.fieldcheck.domain.model.ItemResultValue

/**
 * Selector de resultado de P3: tres botones del mismo ancho en fila (Figma 2:72).
 *
 * Se anuncia como grupo de opciones ("Cumple, seleccionado, 1 de 3", criterio A5).
 *
 * El selector solo avisa qué botón se tocó: decidir qué hacer (por ejemplo, que tocar
 * el resultado ya elegido lo deje sin completar) es responsabilidad del ViewModel.
 *
 * @param selected resultado elegido, o `null` si el ítem está sin completar.
 * @param onResultClick se llama con el resultado del botón tocado, aunque ya estuviera elegido.
 */
@Composable
fun ResultSelector(
    selected: ItemResultValue?,
    onResultClick: (ItemResultValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val options = listOf(
        ItemResultValue.COMPLIES,
        ItemResultValue.NOT_COMPLIES,
        ItemResultValue.NOT_APPLICABLE,
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (option in options) {
            ResultButton(
                result = option,
                isSelected = option == selected,
                onClick = { onResultClick(option) },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Un botón del [ResultSelector]: 64 dp de alto, esquinas de 12 dp.
 * Seleccionado = fondo lleno del color del resultado; no seleccionado = solo contorno.
 */
@Composable
fun ResultButton(
    result: ItemResultValue,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val style = resultStyleOf(result)
    val shape = MaterialTheme.shapes.medium

    val backgroundColor: Color
    val contentColor: Color
    if (isSelected) {
        backgroundColor = style.color
        contentColor = style.onColor
    } else {
        backgroundColor = Color.Transparent
        contentColor = style.color
    }

    Column(
        modifier = modifier
            .height(64.dp)
            .clip(shape)
            .background(color = backgroundColor, shape = shape)
            .border(width = 1.dp, color = style.color, shape = shape)
            .selectable(
                selected = isSelected,
                onClick = onClick,
                enabled = enabled,
                role = Role.RadioButton,
            )
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = style.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = style.label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultSelectorPreview() {
    FieldCheckTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ResultSelector(selected = null, onResultClick = {})
            ResultSelector(selected = ItemResultValue.NOT_COMPLIES, onResultClick = {})
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121316)
@Composable
private fun ResultSelectorDarkPreview() {
    FieldCheckTheme(darkTheme = true) {
        ResultSelector(
            selected = ItemResultValue.COMPLIES,
            onResultClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
