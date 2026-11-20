package ar.edu.uade.fieldcheck.presentation.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ar.edu.uade.fieldcheck.R
import ar.edu.uade.fieldcheck.domain.model.Evidence
import coil3.compose.AsyncImage
import java.io.File

/**
 * Foto de evidencia a pantalla completa (P5, "Tocar una miniatura").
 *
 * Es un diálogo simple: la foto entera (sin recortar) sobre fondo oscuro.
 * Se cierra con la X, tocando la foto o con el botón atrás del sistema.
 *
 * @param description lo que anuncia TalkBack, por ejemplo "Foto 1 de 2 del ítem 7".
 */
@Composable
fun FullScreenPhotoDialog(
    evidence: Evidence,
    description: String,
    onDismiss: () -> Unit,
) {
    // usePlatformDefaultWidth = false: sin esto el diálogo queda angosto, como una alerta.
    val properties = DialogProperties(usePlatformDefaultWidth = false)

    Dialog(
        onDismissRequest = onDismiss,
        properties = properties,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim)
                .clickable(onClick = onDismiss),
        ) {
            AsyncImage(
                model = File(evidence.filePath),
                contentDescription = description,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.detail_photo_close),
                    // El fondo (scrim) es negro en modo claro y oscuro: la X va siempre en blanco.
                    tint = Color.White,
                )
            }
        }
    }
}
