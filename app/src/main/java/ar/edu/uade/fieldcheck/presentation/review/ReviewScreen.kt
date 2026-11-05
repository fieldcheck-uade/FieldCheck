package ar.edu.uade.fieldcheck.presentation.review

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.fieldcheck.R
import ar.edu.uade.fieldcheck.core.model.UiState
import ar.edu.uade.fieldcheck.core.theme.FieldCheckTheme
import ar.edu.uade.fieldcheck.domain.model.ItemResult
import ar.edu.uade.fieldcheck.domain.model.ItemResultValue
import ar.edu.uade.fieldcheck.domain.model.ResultCounts
import ar.edu.uade.fieldcheck.presentation.common.components.ErrorState
import ar.edu.uade.fieldcheck.presentation.common.components.LoadingState
import ar.edu.uade.fieldcheck.presentation.common.components.OfflineBanner
import ar.edu.uade.fieldcheck.presentation.common.components.ResultCountsRow
import ar.edu.uade.fieldcheck.presentation.review.components.MissingItemsCard
import ar.edu.uade.fieldcheck.presentation.review.components.ReviewItemRow

/**
 * Pantalla P4 sin lógica: recibe el estado y avisa las acciones por callbacks.
 * Al no depender del ViewModel se puede ver en el Preview de Android Studio.
 *
 * @param onItemClick recibe el id de la inspección y la posición del ítem tocado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    uiState: UiState<ReviewContent>,
    finalizationState: FinalizationUiState,
    isOnline: Boolean,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onItemClick: (inspectionId: String, position: Int) -> Unit,
    onFinalizeClick: () -> Unit,
    onConfirmFinalize: () -> Unit,
    onDismissConfirmDialog: () -> Unit,
    onRetryClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.review_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                // En Figma la barra no tiene fondo propio: es del mismo color que la pantalla.
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            // El botón solo tiene sentido cuando la inspección se pudo leer.
            if (uiState is UiState.Success) {
                FinalizeThumbZone(
                    isFinalizing = finalizationState.isFinalizing,
                    onFinalizeClick = onFinalizeClick,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Finalizar funciona sin conexión; el banner solo informa (docs/pantallas.md, P4 · Offline).
            if (!isOnline) {
                OfflineBanner()
            }

            ReviewContentState(
                uiState = uiState,
                isShowingMissingItems = finalizationState.isShowingMissingItems,
                onItemClick = onItemClick,
                onRetryClick = onRetryClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }

    if (finalizationState.isConfirmDialogVisible) {
        ConfirmFinalizeDialog(
            onConfirm = onConfirmFinalize,
            onDismiss = onDismissConfirmDialog,
        )
    }
}

@Composable
private fun ReviewContentState(
    uiState: UiState<ReviewContent>,
    isShowingMissingItems: Boolean,
    onItemClick: (inspectionId: String, position: Int) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is UiState.Loading -> {
            LoadingState(modifier = modifier)
        }

        is UiState.Error -> {
            ErrorState(
                message = uiState.message,
                onRetryClick = onRetryClick,
                modifier = modifier,
            )
        }

        is UiState.Success -> {
            ReviewList(
                content = uiState.data,
                isShowingMissingItems = isShowingMissingItems,
                onItemClick = onItemClick,
                modifier = modifier,
            )
        }
    }
}

/**
 * Lista de P4. Tiene dos modos:
 * - Normal (Figma 5:39): todos los ítems, con los problemáticos marcados.
 * - Faltantes (Figma 46:52): después de tocar "Finalizar" con faltantes, tarjeta de aviso
 *   y solo los ítems que hay que corregir.
 */
@Composable
private fun ReviewList(
    content: ReviewContent,
    isShowingMissingItems: Boolean,
    onItemClick: (inspectionId: String, position: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Si el usuario corrigió todo en P3 y volvió, ya no hay faltantes: se vuelve a la lista normal.
    val missingMessage = content.missingMessage
    val showOnlyMissing = isShowingMissingItems && missingMessage != null

    val visibleItems: List<ReviewItem>
    if (showOnlyMissing) {
        visibleItems = content.missingItems
    } else {
        visibleItems = content.items
    }

    ScrollToTopWhenMissingAppear(listState = listState, showOnlyMissing = showOnlyMissing)

    LazyColumn(
        modifier = modifier,
        state = listState,
    ) {
        item(key = "counts") {
            ResultCountsRow(
                counts = content.counts,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            )
        }

        // Kotlin sabe que, si showOnlyMissing es true, missingMessage no es null.
        if (showOnlyMissing) {
            item(key = "missing-card") {
                MissingItemsCard(
                    message = missingMessage,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            item(key = "missing-title") {
                Text(
                    text = stringResource(R.string.review_missing_section),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                        .semantics { heading() },
                )
            }
        }

        items(
            items = visibleItems,
            key = { reviewItem -> reviewItem.item.id },
        ) { reviewItem ->
            ReviewItemRow(
                reviewItem = reviewItem,
                showChevron = showOnlyMissing,
                onClick = {
                    onItemClick(content.inspectionId, reviewItem.item.position)
                },
            )
        }
    }
}

/** Al aparecer la tarjeta de faltantes se sube al principio, para que el aviso se vea seguro. */
@Composable
private fun ScrollToTopWhenMissingAppear(listState: LazyListState, showOnlyMissing: Boolean) {
    LaunchedEffect(showOnlyMissing) {
        if (showOnlyMissing) {
            listState.animateScrollToItem(index = 0)
        }
    }
}

/** Zona del pulgar: botón de ancho completo, 56 dp de alto, sobre un borde superior (Figma 5:158). */
@Composable
private fun FinalizeThumbZone(
    isFinalizing: Boolean,
    onFinalizeClick: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Button(
                onClick = onFinalizeClick,
                // El botón no se deshabilita por faltantes (ver ReviewViewModel); solo mientras guarda.
                enabled = !isFinalizing,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .heightIn(min = 56.dp),
            ) {
                if (isFinalizing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.review_finalize_button),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

/** "¿Finalizar la inspección? Después no se va a poder editar." con Cancelar / Finalizar. */
@Composable
private fun ConfirmFinalizeDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.review_confirm_title)) },
        text = { Text(text = stringResource(R.string.review_confirm_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.review_confirm_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.review_confirm_cancel))
            }
        },
    )
}

// --- Previews ---

private fun previewItem(position: Int, text: String, result: ItemResultValue?): ItemResult {
    return ItemResult(
        id = position.toLong() + 1L,
        position = position,
        itemText = text,
        result = result,
        note = null,
        evidence = emptyList(),
    )
}

private fun previewContent(): ReviewContent {
    val okItem = ReviewItem(
        item = previewItem(0, "Acceso a obra con cartel de identificación", ItemResultValue.COMPLIES),
        problem = null,
    )
    val notCompliesItem = ReviewItem(
        item = previewItem(11, "Tableros eléctricos con tapa", ItemResultValue.NOT_COMPLIES),
        problem = ReviewProblem.NOT_COMPLIES_WITHOUT_EVIDENCE,
    )
    val pendingItem = ReviewItem(
        item = previewItem(17, "Vallado perimetral completo", null),
        problem = ReviewProblem.WITHOUT_RESULT,
    )
    return ReviewContent(
        inspectionId = "preview",
        counts = ResultCounts(complies = 15, notComplies = 3, notApplicable = 1, pending = 1),
        items = listOf(okItem, notCompliesItem, pendingItem),
        missingItems = listOf(notCompliesItem, pendingItem),
        missingMessage = "Faltan 2 ítems para poder finalizar.",
    )
}

@Preview(showBackground = true, name = "P4 · Contenido")
@Composable
private fun ReviewScreenPreview() {
    FieldCheckTheme {
        ReviewScreen(
            uiState = UiState.Success(previewContent()),
            finalizationState = FinalizationUiState(),
            isOnline = true,
            snackbarHostState = remember { SnackbarHostState() },
            onBackClick = {},
            onItemClick = { _, _ -> },
            onFinalizeClick = {},
            onConfirmFinalize = {},
            onDismissConfirmDialog = {},
            onRetryClick = {},
        )
    }
}

@Preview(showBackground = true, name = "P4 · Faltantes")
@Composable
private fun ReviewScreenMissingPreview() {
    FieldCheckTheme {
        ReviewScreen(
            uiState = UiState.Success(previewContent()),
            finalizationState = FinalizationUiState(isShowingMissingItems = true),
            isOnline = false,
            snackbarHostState = remember { SnackbarHostState() },
            onBackClick = {},
            onItemClick = { _, _ -> },
            onFinalizeClick = {},
            onConfirmFinalize = {},
            onDismissConfirmDialog = {},
            onRetryClick = {},
        )
    }
}
