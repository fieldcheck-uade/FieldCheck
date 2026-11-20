package ar.edu.uade.fieldcheck.presentation.review

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Contenedor de la pantalla P4 · Revisión y finalización (patrón container / presentational).
 *
 * Se conecta con el [ReviewViewModel]: lee el estado, escucha los eventos de una sola vez
 * (navegar a P5 o mostrar un error) y le pasa todo a [ReviewScreen], que solo dibuja.
 *
 * @param onEditItem vuelve a P3 en el ítem tocado (posición desde 0).
 * @param onFinalized la inspección quedó finalizada; `FieldCheckApp` navega a P5.
 */
@Composable
fun ReviewRoute(
    onBackClick: () -> Unit,
    onEditItem: (inspectionId: String, position: Int) -> Unit,
    onFinalized: (inspectionId: String) -> Unit,
    viewModel: ReviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val finalizationState by viewModel.finalizationState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Si el callback cambia entre recomposiciones, el efecto usa siempre el último.
    val currentOnFinalized by rememberUpdatedState(onFinalized)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ReviewEvent.Finalized -> {
                    currentOnFinalized(event.inspectionId)
                }

                is ReviewEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    ReviewScreen(
        uiState = uiState,
        finalizationState = finalizationState,
        isOnline = isOnline,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onItemClick = onEditItem,
        onFinalizeClick = viewModel::onFinalizeClick,
        onConfirmFinalize = viewModel::onConfirmFinalize,
        onDismissConfirmDialog = viewModel::onDismissConfirmDialog,
        onRetryClick = viewModel::loadInspection,
    )
}
