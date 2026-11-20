package ar.edu.uade.fieldcheck.presentation.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.fieldcheck.R
import ar.edu.uade.fieldcheck.core.theme.FieldCheckTheme
import ar.edu.uade.fieldcheck.domain.model.ItemResultValue
import ar.edu.uade.fieldcheck.domain.model.ResultCounts

/**
 * Chip de conteo de P4 y P5: ícono + "Cumple 16", del color del resultado.
 *
 * @param result resultado contado; `null` cuenta los ítems "Sin completar".
 */
@Composable
fun CountChip(
    result: ItemResultValue?,
    count: Int,
    modifier: Modifier = Modifier,
) {
    val style = resultStyleOf(result)

    StatusChip(
        icon = style.icon,
        label = stringResource(R.string.result_count, style.label, count),
        color = style.color,
        modifier = modifier,
    )
}

/**
 * Fila de chips con el resumen de conteos ("Cumple 16", "No cumple 3", "N/A 1").
 *
 * "Sin completar" solo aparece si hay alguno, como pide docs/pantallas.md (P4).
 * Usa `FlowRow` para que, con letra grande, los chips bajen de línea en lugar de cortarse.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResultCountsRow(
    counts: ResultCounts,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CountChip(result = ItemResultValue.COMPLIES, count = counts.complies)
        CountChip(result = ItemResultValue.NOT_COMPLIES, count = counts.notComplies)
        CountChip(result = ItemResultValue.NOT_APPLICABLE, count = counts.notApplicable)
        if (counts.pending > 0) {
            CountChip(result = null, count = counts.pending)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultCountsRowPreview() {
    FieldCheckTheme {
        ResultCountsRow(
            counts = ResultCounts(complies = 16, notComplies = 3, notApplicable = 1, pending = 2),
            modifier = Modifier.padding(16.dp),
        )
    }
}
