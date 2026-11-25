package ar.edu.uade.fieldcheck.presentation.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.fieldcheck.domain.model.Inspection

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
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onNewInspectionClick: () -> Unit,
    onInspectionClick: (Inspection) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    HistoryScreen(
        uiState = uiState,
        isOnline = isOnline,
        isDarkMode = isDarkMode,
        onDarkModeChange = onDarkModeChange,
        onNewInspectionClick = onNewInspectionClick,
        onInspectionClick = onInspectionClick,
        onRetryClick = viewModel::loadHistory,
    )
}
