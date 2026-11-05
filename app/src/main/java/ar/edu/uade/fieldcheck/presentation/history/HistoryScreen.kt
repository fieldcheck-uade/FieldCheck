package ar.edu.uade.fieldcheck.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import ar.edu.uade.fieldcheck.presentation.common.components.OfflineBanner
import ar.edu.uade.fieldcheck.presentation.history.components.HistoryLoadingSkeleton
import ar.edu.uade.fieldcheck.presentation.history.components.InspectionCard

/**
 * Contenedor de la pantalla P1 (patrón container / presentational).
 *
 * Se conecta con el [HistoryViewModel]: lee el estado y le pasa todo a [HistoryScreen],
 * que solo dibuja. Las acciones de navegación suben a `FieldCheckApp` por callbacks.
 *
 * @param onInspectionClick recibe la inspección tocada; `FieldCheckApp` decide si abre
 *   la ejecución (en curso) o el detalle (finalizada).
 */
@Composable
fun HistoryRoute(
    onNewInspectionClick: () -> Unit,
    onInspectionClick: (Inspection) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    HistoryScreen(
        uiState = uiState,
        isOnline = isOnline,
        onNewInspectionClick = onNewInspectionClick,
        onInspectionClick = onInspectionClick,
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
    isOnline: Boolean,
    onNewInspectionClick: () -> Unit,
    onInspectionClick: (Inspection) -> Unit,
    onRetryClick: () -> Unit,
    nowMillis: Long = System.currentTimeMillis(),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.app_name)) },
                // En Figma la barra no tiene fondo propio: es del mismo color que la pantalla.
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewInspectionClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                text = { Text(text = stringResource(R.string.history_new_inspection)) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // El banner va arriba de todo y no deshabilita nada (docs/pantallas.md, P1 · Offline).
            if (!isOnline) {
                OfflineBanner()
            }

            HistoryContent(
                uiState = uiState,
                onInspectionClick = onInspectionClick,
                onRetryClick = onRetryClick,
                nowMillis = nowMillis,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }
}

@Composable
private fun HistoryContent(
    uiState: UiState<InspectionHistory>,
    onInspectionClick: (Inspection) -> Unit,
    onRetryClick: () -> Unit,
    nowMillis: Long,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is UiState.Loading -> {
            HistoryLoadingSkeleton(modifier = modifier)
        }

        is UiState.Error -> {
            ErrorState(
                message = uiState.message,
                onRetryClick = onRetryClick,
                modifier = modifier,
            )
        }

        is UiState.Success -> {
            val history = uiState.data
            if (history.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.history_empty_title),
                    description = stringResource(R.string.history_empty_description),
                    modifier = modifier,
                )
            } else {
                HistoryList(
                    history = history,
                    onInspectionClick = onInspectionClick,
                    nowMillis = nowMillis,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun HistoryList(
    history: InspectionHistory,
    onInspectionClick: (Inspection) -> Unit,
    nowMillis: Long,
    modifier: Modifier = Modifier,
) {
    val inProgressTitle = stringResource(R.string.history_section_in_progress)
    val finishedTitle = stringResource(R.string.history_section_finished)

    LazyColumn(
        modifier = modifier,
        // Espacio abajo para que el FAB no tape la última tarjeta.
        contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // "En curso" va primero y solo se muestra si hay alguna.
        if (history.inProgress.isNotEmpty()) {
            inspectionSection(
                title = inProgressTitle,
                inspections = history.inProgress,
                onInspectionClick = onInspectionClick,
                nowMillis = nowMillis,
            )
        }
        if (history.finished.isNotEmpty()) {
            inspectionSection(
                title = finishedTitle,
                inspections = history.finished,
                onInspectionClick = onInspectionClick,
                nowMillis = nowMillis,
            )
        }
    }
}

/** Agrega a la lista un título de sección seguido de sus tarjetas. */
private fun LazyListScope.inspectionSection(
    title: String,
    inspections: List<Inspection>,
    onInspectionClick: (Inspection) -> Unit,
    nowMillis: Long,
) {
    item(key = "section-$title") {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 4.dp, top = 8.dp)
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
            nowMillis = nowMillis,
        )
    }
}

// ----- Previews: se ven en Android Studio sin correr la app -----

/** "Ahora" fijo de los previews: 25/09/2026 18:00 (hora local aproximada). */
private const val PREVIEW_NOW = 1_790_362_800_000L
private const val ONE_HOUR = 60L * 60L * 1000L

private fun previewInspection(
    id: String,
    siteName: String,
    templateName: String,
    status: InspectionStatus,
    syncStatus: SyncStatus,
    completedItems: Int,
    hoursAgo: Long,
): Inspection {
    val date = PREVIEW_NOW - hoursAgo * ONE_HOUR
    return Inspection(
        clientUuid = id,
        templateName = templateName,
        siteName = siteName,
        status = status,
        syncStatus = syncStatus,
        startedAt = date,
        finishedAt = date,
        totalItems = 20,
        completedItems = completedItems,
    )
}

private fun previewHistory(): InspectionHistory {
    return InspectionHistory(
        inProgress = listOf(
            previewInspection(
                "1", "Obra Av. Siempreviva 742 — Subsuelo 2", "Control semanal de obra",
                InspectionStatus.IN_PROGRESS, SyncStatus.PENDING, completedItems = 7, hoursAgo = 8,
            ),
        ),
        finished = listOf(
            previewInspection(
                "2", "Planta Norte — Sala de bombas", "Sala técnica",
                InspectionStatus.FINISHED, SyncStatus.PENDING, completedItems = 20, hoursAgo = 26,
            ),
            previewInspection(
                "3", "Edificio Rivadavia 1200 — Azotea", "Control semanal de obra",
                InspectionStatus.FINISHED, SyncStatus.SYNCED, completedItems = 20, hoursAgo = 55,
            ),
            previewInspection(
                "4", "Depósito Lanús — Nave 3", "Seguridad e higiene",
                InspectionStatus.FINISHED, SyncStatus.FAILED, completedItems = 20, hoursAgo = 104,
            ),
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenContentPreview() {
    FieldCheckTheme {
        HistoryScreen(
            uiState = UiState.Success(previewHistory()),
            isOnline = true,
            onNewInspectionClick = {},
            onInspectionClick = {},
            onRetryClick = {},
            nowMillis = PREVIEW_NOW,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenOfflinePreview() {
    FieldCheckTheme {
        HistoryScreen(
            uiState = UiState.Success(previewHistory()),
            isOnline = false,
            onNewInspectionClick = {},
            onInspectionClick = {},
            onRetryClick = {},
            nowMillis = PREVIEW_NOW,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenLoadingPreview() {
    FieldCheckTheme {
        HistoryScreen(
            uiState = UiState.Loading,
            isOnline = true,
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
            isOnline = true,
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
            isOnline = true,
            onNewInspectionClick = {},
            onInspectionClick = {},
            onRetryClick = {},
        )
    }
}
