package ar.edu.uade.fieldcheck.presentation.common.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ar.edu.uade.fieldcheck.R
import ar.edu.uade.fieldcheck.domain.model.Evidence
import coil3.compose.AsyncImage
import java.io.File

/**
 * Miniatura de 72 dp de una foto de evidencia (P3 y P5).
 *
 * - La imagen se carga con Coil desde el archivo guardado en el teléfono, sin bloquear la UI.
 * - Si la foto tiene coordenadas, lleva un pin de ubicación abajo a la derecha.
 *
 * @param contentDescription lo que anuncia TalkBack, por ejemplo "Foto 1 de 2 del ítem 7" (A4).
 * @param onClick tocar: ver la foto a pantalla completa.
 * @param onLongClick mantener apretado: eliminar. `null` en pantallas de solo lectura (P5).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EvidenceThumbnail(
    evidence: Evidence,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val shape = MaterialTheme.shapes.small

    Box(
        modifier = modifier
            .size(72.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .semantics {
                this.contentDescription = contentDescription
            },
    ) {
        AsyncImage(
            model = File(evidence.filePath),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(72.dp),
        )

        if (evidence.hasLocation()) {
            LocationBadge(modifier = Modifier.align(Alignment.BottomEnd))
        }
    }
}

/** Pin de ubicación sobre la miniatura, con fondo para que se vea sobre cualquier foto. */
@Composable
private fun LocationBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .size(20.dp)
            .background(color = MaterialTheme.colorScheme.surface, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Place,
            contentDescription = stringResource(R.string.evidence_location_badge),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
        )
    }
}
