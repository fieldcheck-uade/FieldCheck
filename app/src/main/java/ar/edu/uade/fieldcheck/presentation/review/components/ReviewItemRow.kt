package ar.edu.uade.fieldcheck.presentation.review.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.fieldcheck.R
import ar.edu.uade.fieldcheck.core.theme.FieldCheckTheme
import ar.edu.uade.fieldcheck.core.theme.statusColors
import ar.edu.uade.fieldcheck.domain.model.ItemResult
import ar.edu.uade.fieldcheck.domain.model.ItemResultValue
import ar.edu.uade.fieldcheck.presentation.common.components.ItemResultLabel
import ar.edu.uade.fieldcheck.presentation.review.ReviewItem
import ar.edu.uade.fieldcheck.presentation.review.ReviewProblem

/**
 * Fila de un ítem en la lista de P4 (Figma 5:39 y 46:52).
 *
 * Muestra número, texto (dos líneas como máximo) y el resultado. Si el ítem impide finalizar,
 * en lugar del resultado se muestra el motivo con ícono de advertencia, así el usuario sabe
 * qué corregir sin depender solo del color (criterio A3).
 *
 * @param showChevron `true` en la lista de faltantes, donde Figma marca que se puede tocar.
 */
@Composable
fun ReviewItemRow(
    reviewItem: ReviewItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showChevron: Boolean = false,
) {
    val item = reviewItem.item
    val editLabel = stringResource(R.string.review_edit_item_action, item.displayNumber())

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = editLabel, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            ItemNumberBadge(number = item.displayNumber())

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = item.itemText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                ItemStatusLine(item = item, problem = reviewItem.problem)
            }

            // Figma muestra la cámara solo cuando el ítem tiene fotos adjuntas.
            val hasPhotos = item.evidence.isNotEmpty()
            if (hasPhotos) {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = stringResource(R.string.review_item_has_photos),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }

            if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterVertically),
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
    }
}

/** Círculo gris de 28 dp con el número del ítem, como en Figma. */
@Composable
private fun ItemNumberBadge(number: Int) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(color = MaterialTheme.colorScheme.surfaceContainer, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Segunda línea de la fila: el motivo del problema si lo hay; si no, el resultado normal. */
@Composable
private fun ItemStatusLine(item: ItemResult, problem: ReviewProblem?) {
    if (problem == null) {
        ItemResultLabel(result = item.result)
    } else {
        ProblemLabel(problem = problem)
    }
}

/** Ícono de advertencia + motivo ("Sin completar" / "No cumple sin evidencia") en rojo. */
@Composable
private fun ProblemLabel(problem: ReviewProblem) {
    val warningColor = MaterialTheme.statusColors.notComplies
    val reasonText = problemText(problem)

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = warningColor,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = reasonText,
            style = MaterialTheme.typography.labelLarge,
            color = warningColor,
        )
    }
}

@Composable
private fun problemText(problem: ReviewProblem): String {
    when (problem) {
        ReviewProblem.WITHOUT_RESULT -> {
            return stringResource(R.string.review_problem_without_result)
        }

        ReviewProblem.NOT_COMPLIES_WITHOUT_EVIDENCE -> {
            return stringResource(R.string.review_problem_not_complies_without_evidence)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReviewItemRowPreview() {
    FieldCheckTheme {
        Column {
            ReviewItemRow(
                reviewItem = ReviewItem(
                    item = ItemResult(
                        id = 7,
                        position = 6,
                        itemText = "Matafuegos señalizados y con carga vigente",
                        result = ItemResultValue.COMPLIES,
                        note = null,
                        evidence = emptyList(),
                    ),
                    problem = null,
                ),
                onClick = {},
            )
            ReviewItemRow(
                reviewItem = ReviewItem(
                    item = ItemResult(
                        id = 12,
                        position = 11,
                        itemText = "Tableros eléctricos con tapa",
                        result = ItemResultValue.NOT_COMPLIES,
                        note = null,
                        evidence = emptyList(),
                    ),
                    problem = ReviewProblem.NOT_COMPLIES_WITHOUT_EVIDENCE,
                ),
                onClick = {},
                showChevron = true,
            )
        }
    }
}
