package ar.edu.uade.fieldcheck.presentation.execution.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ar.edu.uade.fieldcheck.R
import ar.edu.uade.fieldcheck.domain.model.ItemResultValue
import ar.edu.uade.fieldcheck.presentation.common.components.ResultSelector

/**
 * Zona del pulgar, fija abajo (Figma 4:56): el selector de resultado y la navegación.
 *
 * Está abajo porque en obra el teléfono se usa con una mano: lo que se toca en cada
 * ítem tiene que quedar al alcance del pulgar.
 *
 * @param isLastItem en el último ítem "Siguiente" pasa a ser "Revisar y finalizar".
 */
@Composable
fun ThumbZone(
    selectedResult: ItemResultValue?,
    canEdit: Boolean,
    isFirstItem: Boolean,
    isLastItem: Boolean,
    onResultClick: (ItemResultValue) -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ResultSelector(
                    selected = selectedResult,
                    onResultClick = onResultClick,
                    enabled = canEdit,
                )

                NavigationRow(
                    isFirstItem = isFirstItem,
                    isLastItem = isLastItem,
                    onPreviousClick = onPreviousClick,
                    onNextClick = onNextClick,
                )
            }
        }
    }
}

/** "Anterior" (texto) a la izquierda y "Siguiente" (botón tonal) a la derecha. */
@Composable
private fun NavigationRow(
    isFirstItem: Boolean,
    isLastItem: Boolean,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
) {
    val nextLabel: String
    if (isLastItem) {
        nextLabel = stringResource(R.string.execution_review_and_finish)
    } else {
        nextLabel = stringResource(R.string.execution_next)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onPreviousClick,
            enabled = !isFirstItem,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text(text = stringResource(R.string.execution_previous))
        }

        Button(
            onClick = onNextClick,
            modifier = Modifier.heightIn(min = 48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Text(text = nextLabel)
        }
    }
}
