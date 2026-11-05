package ar.edu.uade.fieldcheck.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.fieldcheck.R
import ar.edu.uade.fieldcheck.core.model.UiState
import ar.edu.uade.fieldcheck.core.theme.FieldCheckTheme
import ar.edu.uade.fieldcheck.domain.model.Inspection
import ar.edu.uade.fieldcheck.domain.model.InspectionHistory
import ar.edu.uade.fieldcheck.domain.model.InspectionStatus
import ar.edu.uade.fieldcheck.domain.model.SyncStatus
import ar.edu.uade.fieldcheck.presentation.common.components.EmptyState
import ar.edu.uade.fieldcheck.presentation.common.components.ErrorState
import ar.edu.uade.fieldcheck.presentation.common.components.LoadingState
import ar.edu.uade.fieldcheck.presentation.history.components.InspectionCard

/**
 * Contenedor de la pantalla P1 (patrón container / presentational).
 *
 * Se conecta con el [HistoryViewModel]: lee el estado, escucha los eventos one-shot
 * y le pasa todo a [HistoryScreen], que solo dibuja.
 */
@Composable
fun HistoryRoute(
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Escuchamos los eventos de una sola vez mientras la pantalla esté visible.
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HistoryUiEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    HistoryScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNewInspectionClick = viewModel::onNewInspectionClick,
        onInspectionClick = viewModel::onInspectionClick,
        onRetryClick = viewModel::loadHistory,
    )
}

/**
 * Pantalla P1 sin lógica: recibe el estado y avisa las acciones por callbacks.
 * Al no depender del ViewModel se puede ver en el Preview de Android Studio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    uiState: UiState<InspectionHistory>,
    snackbarHostState: SnackbarHostState,
    onNewInspectionClick: () -> Unit,
    onInspectionClick: (Inspection) -> Unit,
    onRetryClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = stringResource(R.string.app_name)) })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewInspectionClick,
                icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                text = { Text(text = stringResource(R.string.history_new_inspection)) },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when (uiState) {
            is UiState.Loading -> {
                LoadingState(modifier = contentModifier)
            }

            is UiState.Error -> {
                ErrorState(
                    message = uiState.message,
                    onRetryClick = onRetryClick,
                    modifier = contentModifier,
                )
            }

            is UiState.Success -> {
                val history = uiState.data
                if (history.isEmpty()) {
                    EmptyState(
                        title = stringResource(R.string.history_empty_title),
                        description = stringResource(R.string.history_empty_description),
                        modifier = contentModifier,
                    )
                } else {
                    HistoryList(
                        history = history,
                        onInspectionClick = onInspectionClick,
                        modifier = contentModifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryList(
    history: InspectionHistory,
    onInspectionClick: (Inspection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inProgressTitle = stringResource(R.string.history_section_in_progress)
    val finishedTitle = stringResource(R.string.history_section_finished)

    LazyColumn(
        modifier = modifier,
        // Espacio abajo para que el FAB no tape la última tarjeta.
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // "En curso" va primero y solo se muestra si hay alguna.
        if (history.inProgress.isNotEmpty()) {
            inspectionSection(
                title = inProgressTitle,
                inspections = history.inProgress,
                onInspectionClick = onInspectionClick,
            )
        }
        if (history.finished.isNotEmpty()) {
            inspectionSection(
                title = finishedTitle,
                inspections = history.finished,
                onInspectionClick = onInspectionClick,
            )
        }
    }
}

/** Agrega a la lista un título de sección seguido de sus tarjetas. */
private fun LazyListScope.inspectionSection(
    title: String,
    inspections: List<Inspection>,
    onInspectionClick: (Inspection) -> Unit,
) {
    item(key = "section-$title") {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(top = 8.dp, bottom = 4.dp)
                .semantics { heading() },
        )
    }
    items(
        items = inspections,
        key = { inspection -> inspection.clientUuid },
    ) { inspection ->
        InspectionCard(
            inspection = inspection,
            onClick = { onInspectionClick(inspection) },
        )
    }
}

// ----- Previews: se ven en Android Studio sin correr la app -----

private fun previewInspection(
    id: String,
    siteName: String,
    status: InspectionStatus,
    syncStatus: SyncStatus,
    completedItems: Int,
): Inspection {
    return Inspection(
        clientUuid = id,
        templateName = "Seguridad eléctrica",
        siteName = siteName,
        status = status,
        syncStatus = syncStatus,
        startedAt = 1_793_000_000_000L,
        finishedAt = 1_793_003_600_000L,
        totalItems = 8,
        completedItems = completedItems,
    )
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenContentPreview() {
    val history = InspectionHistory(
        inProgress = listOf(
            previewInspection("1", "Planta Norte · Sala de bombas", InspectionStatus.IN_PROGRESS, SyncStatus.PENDING, 3),
        ),
        finished = listOf(
            previewInspection("2", "Obra Belgrano · Piso 3", InspectionStatus.FINISHED, SyncStatus.SYNCED, 8),
            previewInspection("3", "Depósito Sur · Tablero", InspectionStatus.FINISHED, SyncStatus.FAILED, 8),
        ),
    )
    FieldCheckTheme {
        HistoryScreen(
            uiState = UiState.Success(history),
            snackbarHostState = remember { SnackbarHostState() },
            onNewInspectionClick = {},
            onInspectionClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenEmptyPreview() {
    val emptyHistory = InspectionHistory(inProgress = emptyList(), finished = emptyList())
    FieldCheckTheme {
        HistoryScreen(
            uiState = UiState.Success(emptyHistory),
            snackbarHostState = remember { SnackbarHostState() },
            onNewInspectionClick = {},
            onInspectionClick = {},
            onRetryClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenErrorPreview() {
    FieldCheckTheme {
        HistoryScreen(
            uiState = UiState.Error("No pudimos leer las inspecciones guardadas."),
            snackbarHostState = remember { SnackbarHostState() },
            onNewInspectionClick = {},
            onInspectionClick = {},
            onRetryClick = {},
        )
    }
}
