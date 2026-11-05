package ar.edu.uade.fieldcheck.presentation.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.fieldcheck.R
import ar.edu.uade.fieldcheck.core.theme.FieldCheckTheme
import ar.edu.uade.fieldcheck.domain.model.Evidence
import ar.edu.uade.fieldcheck.domain.model.ItemResult
import ar.edu.uade.fieldcheck.domain.model.ItemResultValue
import ar.edu.uade.fieldcheck.presentation.common.components.EvidenceThumbnail
import ar.edu.uade.fieldcheck.presentation.common.components.ItemResultLabel

/**
 * Tarjeta de solo lectura de un ítem "No cumple" en P5 (Figma 5:202).
 *
 * Muestra "7. Texto del ítem", el resultado, la nota (si hay) y las miniaturas de las fotos.
 * No tiene acciones de edición: la inspección ya está finalizada (regla R6).
 *
 * @param onPhotoClick recibe la foto tocada y su descripción para TalkBack,
 *   para verla a pantalla completa.
 */
@Composable
fun NotCompliesItemCard(
    item: ItemResult,
    onPhotoClick: (evidence: Evidence, description: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.detail_item_title, item.displayNumber(), item.itemText),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            ItemResultLabel(result = item.result)

            val note = item.note
            if (note != null && note.isNotBlank()) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (item.evidence.isNotEmpty()) {
                EvidenceRow(
                    item = item,
                    onPhotoClick = onPhotoClick,
                )
            }
        }
    }
}

/**
 * Fila de miniaturas. Se desplaza de costado si hay muchas fotos, para no cortarlas.
 * Cada miniatura se anuncia como "Foto 1 de 2 del ítem 7" (criterio A4).
 */
@Composable
private fun EvidenceRow(
    item: ItemResult,
    onPhotoClick: (evidence: Evidence, description: String) -> Unit,
) {
    val totalPhotos = item.evidence.size

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item.evidence.forEachIndexed { index, evidence ->
            val photoNumber = index + 1
            val description = stringResource(
                R.string.detail_photo_description,
                photoNumber,
                totalPhotos,
                item.displayNumber(),
            )

            // Sin onLongClick: en P5 no se pueden borrar fotos (solo lectura).
            EvidenceThumbnail(
                evidence = evidence,
                contentDescription = description,
                onClick = { onPhotoClick(evidence, description) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotCompliesItemCardPreview() {
    val photo = Evidence(
        clientUuid = "preview-photo",
        filePath = "",
        capturedAt = 0L,
        latitude = -34.6,
        longitude = -58.4,
    )
    val item = ItemResult(
        id = 7L,
        position = 6,
        itemText = "Matafuegos señalizados y con carga vigente",
        result = ItemResultValue.NOT_COMPLIES,
        note = "Falta señalización en el matafuego del palier.",
        evidence = listOf(photo),
    )

    FieldCheckTheme {
        NotCompliesItemCard(
            item = item,
            onPhotoClick = { _, _ -> },
            modifier = Modifier.padding(16.dp),
        )
    }
}
