package ar.edu.uade.fieldcheck.presentation.execution.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ar.edu.uade.fieldcheck.R
import ar.edu.uade.fieldcheck.core.theme.statusColors
import ar.edu.uade.fieldcheck.domain.model.Evidence
import ar.edu.uade.fieldcheck.presentation.common.components.EvidenceThumbnail

/** Alpha del fondo del aviso: un tinte suave del color "No cumple", como en Figma (4:50). */
private const val WARNING_BACKGROUND_ALPHA = 0.08f

/** "✓ Guardado": confirma que el resultado del ítem quedó registrado (Figma 4:36). */
@Composable
fun SavedIndicator(modifier: Modifier = Modifier) {
    val savedColor = MaterialTheme.statusColors.complies

    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            liveRegion = LiveRegionMode.Polite
        },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = savedColor,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.execution_saved),
            style = MaterialTheme.typography.labelLarge,
            color = savedColor,
        )
    }
}

/**
 * Campo "Nota (opcional)": varias líneas y crece con el texto.
 * Cada cambio sube al ViewModel, que decide cuándo guardarlo.
 */
@Composable
fun NoteField(
    note: String,
    onNoteChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.execution_note_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            enabled = enabled,
            minLines = 2,
            placeholder = {
                Text(text = stringResource(R.string.execution_note_placeholder))
            },
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Fila de evidencia: miniaturas de las fotos y, al final, el botón "Agregar foto".
 *
 * @param itemNumber se usa en la descripción para TalkBack: "Foto 1 de 2 del ítem 7".
 * @param canEdit `false` en una inspección finalizada: sin botón de agregar ni borrar.
 */
@Composable
fun EvidenceRow(
    evidence: List<Evidence>,
    itemNumber: Int,
    canEdit: Boolean,
    onAddPhotoClick: () -> Unit,
    onPhotoClick: (Evidence) -> Unit,
    onPhotoLongClick: (Evidence) -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalPhotos = evidence.size

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(items = evidence, key = { _, photo -> photo.clientUuid }) { index, photo ->
            val description = stringResource(
                R.string.execution_photo_description,
                index + 1,
                totalPhotos,
                itemNumber,
            )

            val longClickAction: (() -> Unit)?
            if (canEdit) {
                longClickAction = { onPhotoLongClick(photo) }
            } else {
                longClickAction = null
            }

            EvidenceThumbnail(
                evidence = photo,
                contentDescription = description,
                onClick = { onPhotoClick(photo) },
                onLongClick = longClickAction,
            )
        }

        if (canEdit) {
            item(key = "add-photo") {
                AddPhotoButton(onClick = onAddPhotoClick)
            }
        }
    }
}

/** Botón "Agregar foto" con borde punteado, del mismo alto que una miniatura (Figma 4:45). */
@Composable
private fun AddPhotoButton(onClick: () -> Unit) {
    val borderColor = MaterialTheme.colorScheme.outline
    val contentColor = MaterialTheme.colorScheme.primary
    val shape = MaterialTheme.shapes.small

    Column(
        modifier = Modifier
            .width(104.dp)
            .height(72.dp)
            .clip(shape)
            .drawBehind {
                val dashLength = 6.dp.toPx()
                val dashedStroke = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength, dashLength)),
                )
                drawRoundRect(
                    color = borderColor,
                    style = dashedStroke,
                    cornerRadius = CornerRadius(8.dp.toPx()),
                )
            }
            .clickable(role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.CameraAlt,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = stringResource(R.string.execution_add_photo),
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
        )
    }
}

/**
 * Aviso de la regla R4: "No cumple" sin nota ni foto (Figma 4:50).
 * Informa sin bloquear; el bloqueo real ocurre al finalizar en P4.
 */
@Composable
fun EvidenceWarningCard(modifier: Modifier = Modifier) {
    val warningColor = MaterialTheme.statusColors.notComplies
    val shape = MaterialTheme.shapes.small

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = warningColor.copy(alpha = WARNING_BACKGROUND_ALPHA), shape = shape)
            .border(width = 1.dp, color = warningColor, shape = shape)
            .padding(12.dp)
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
            },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = warningColor,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(R.string.execution_evidence_warning),
            style = MaterialTheme.typography.bodyMedium,
            color = warningColor,
        )
    }
}

/** Aviso para una inspección finalizada abierta en P3: se puede mirar pero no cambiar (R6). */
@Composable
fun ReadOnlyNotice(modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.small

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surfaceContainer, shape = shape)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(R.string.execution_read_only),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
