package ar.edu.uade.fieldcheck.presentation.execution.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ar.edu.uade.fieldcheck.R

/**
 * Barra superior de P3: atrás, nombre del sitio y "Ver todos" (Figma 4:16).
 *
 * El nombre del sitio va en una sola línea con puntos suspensivos: en obra suelen ser
 * largos ("Obra Av. Siempreviva 742 — Subsuelo 2") y no deben empujar los íconos.
 *
 * @param siteName `null` mientras la inspección se está cargando.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecutionTopBar(
    siteName: String?,
    onBackClick: () -> Unit,
    onSeeAllClick: (() -> Unit)?,
) {
    TopAppBar(
        title = {
            if (siteName != null) {
                Text(
                    text = siteName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
        },
        actions = {
            // Sin inspección cargada no hay nada que revisar: el ícono no se muestra.
            if (onSeeAllClick != null) {
                IconButton(onClick = onSeeAllClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = stringResource(R.string.execution_see_all_items),
                    )
                }
            }
        },
        // En Figma la barra no tiene fondo propio: es del mismo color que la pantalla.
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

/** "Ítem 7 de 20" con la barra de avance debajo (Figma 4:30). */
@Composable
fun ItemProgress(
    itemNumber: Int,
    totalItems: Int,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.execution_progress, itemNumber, totalItems),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val barShape = RoundedCornerShape(3.dp)
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(barShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}
