package ar.edu.uade.fieldcheck.presentation.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.fieldcheck.core.theme.FieldCheckTheme
import ar.edu.uade.fieldcheck.domain.model.ItemResultValue

/**
 * Resultado de un ítem en modo lectura: ícono chico + "Cumple" / "No cumple" / "N/A" /
 * "Sin completar", del color del resultado. Se usa en las listas de P4 y P5.
 */
@Composable
fun ItemResultLabel(
    result: ItemResultValue?,
    modifier: Modifier = Modifier,
) {
    val style = resultStyleOf(result)

    Row(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = style.icon,
            contentDescription = null,
            tint = style.color,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = style.label,
            style = MaterialTheme.typography.labelLarge,
            color = style.color,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemResultLabelPreview() {
    FieldCheckTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ItemResultLabel(result = ItemResultValue.COMPLIES)
            ItemResultLabel(result = ItemResultValue.NOT_COMPLIES)
            ItemResultLabel(result = ItemResultValue.NOT_APPLICABLE)
            ItemResultLabel(result = null)
        }
    }
}
