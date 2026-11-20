package ar.edu.uade.fieldcheck.presentation.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ar.edu.uade.fieldcheck.R

/**
 * Estado de carga de P1 (Figma 46:47): tres tarjetas "esqueleto" grises, sin texto.
 *
 * Muestra la forma de lo que viene en lugar de un círculo girando, así la pantalla
 * no "salta" cuando llegan los datos. En general la lectura local es tan rápida que no se ve.
 */
@Composable
fun HistoryLoadingSkeleton(modifier: Modifier = Modifier) {
    val loadingDescription = stringResource(R.string.history_loading)

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = loadingDescription
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SkeletonBar(width = 72.dp, height = 12.dp, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
        SkeletonCard(titleWidth = 210.dp)
        SkeletonCard(titleWidth = 180.dp)
        SkeletonCard(titleWidth = 230.dp)
    }
}

@Composable
private fun SkeletonCard(titleWidth: Dp) {
    val shape = MaterialTheme.shapes.medium

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surface, shape = shape)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SkeletonBar(width = titleWidth, height = 16.dp)
        SkeletonBar(width = 150.dp, height = 12.dp)
        SkeletonBar(width = 110.dp, height = 28.dp)
    }
}

@Composable
private fun SkeletonBar(width: Dp, height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.small,
            ),
    )
}
