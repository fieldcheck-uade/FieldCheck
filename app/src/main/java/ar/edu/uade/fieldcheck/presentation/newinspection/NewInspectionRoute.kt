package ar.edu.uade.fieldcheck.presentation.newinspection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Contenedor de la pantalla P2 (patrón container / presentational).
 *
 * Se conecta con el [NewInspectionViewModel]: lee el estado, se lo pasa a [NewInspectionScreen]
 * y escucha los eventos de una sola vez (inspección creada) para navegar.
 *
 * @param onInspectionStarted recibe el id de la inspección nueva; `FieldCheckApp` abre la
 *   ejecución (P3) y saca a P2 de la pila.
 */
@Composable
fun NewInspectionRoute(
    onBackClick: () -> Unit,
    onInspectionStarted: (inspectionId: String) -> Unit,
) {
    val viewModel: NewInspectionViewModel = hiltViewModel()
    val templatesState by viewModel.templatesState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val startSheetState by viewModel.startSheetState.collectAsStateWithLifecycle()

    // Si la pantalla se recompone con otro callback, el efecto usa siempre el más reciente
    // sin tener que volver a suscribirse a los eventos.
    val currentOnInspectionStarted by rememberUpdatedState(onInspectionStarted)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is NewInspectionEvent.InspectionStarted -> {
                    currentOnInspectionStarted(event.inspectionId)
                }
            }
        }
    }

    NewInspectionScreen(
        templatesState = templatesState,
        isOnline = isOnline,
        startSheetState = startSheetState,
        onBackClick = onBackClick,
        onRetryClick = viewModel::loadTemplates,
        onTemplateClick = viewModel::onTemplateSelected,
        onSiteNameChange = viewModel::onSiteNameChange,
        onStartInspectionClick = viewModel::onStartInspectionClick,
        onSheetDismiss = viewModel::onSheetDismissed,
    )
}
