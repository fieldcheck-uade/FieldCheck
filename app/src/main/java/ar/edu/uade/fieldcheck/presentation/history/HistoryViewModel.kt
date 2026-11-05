package ar.edu.uade.fieldcheck.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.core.model.UiState
import ar.edu.uade.fieldcheck.domain.model.Inspection
import ar.edu.uade.fieldcheck.domain.model.InspectionHistory
import ar.edu.uade.fieldcheck.domain.usecase.GetInspectionHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Eventos de una sola vez (one-shot): se muestran y desaparecen.
 * No son estado, por eso no viajan en el StateFlow (si no, se repetirían al rotar la pantalla).
 */
sealed interface HistoryUiEffect {
    data class ShowSnackbar(val message: String) : HistoryUiEffect
}

/**
 * ViewModel de la pantalla P1 · Historial.
 *
 * Responsabilidades:
 * 1. Pedir el historial al caso de uso y traducirlo a [UiState] para la UI.
 * 2. Recibir las acciones del usuario (tocar una tarjeta, tocar "Nueva inspección").
 *
 * No conoce Room ni Compose: solo habla con el caso de uso. Por eso se puede testear
 * con un repositorio falso.
 *
 * - [uiState] usa StateFlow: siempre tiene un valor y la pantalla recibe el último al suscribirse.
 * - [effects] usa SharedFlow: un evento emitido se consume una vez y no se vuelve a disparar.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getInspectionHistory: GetInspectionHistoryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<InspectionHistory>>(UiState.Loading)
    val uiState: StateFlow<UiState<InspectionHistory>> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<HistoryUiEffect>()
    val effects: SharedFlow<HistoryUiEffect> = _effects.asSharedFlow()

    /** Guardamos el trabajo de lectura para poder cancelarlo si el usuario toca "Reintentar". */
    private var historyJob: Job? = null

    init {
        loadHistory()
    }

    /** Empieza (o vuelve a empezar) a observar el historial guardado en el teléfono. */
    fun loadHistory() {
        historyJob?.cancel()
        _uiState.value = UiState.Loading

        historyJob = viewModelScope.launch {
            getInspectionHistory().collect { resource ->
                handleHistoryResult(resource)
            }
        }
    }

    private fun handleHistoryResult(resource: Resource<InspectionHistory>) {
        when (resource) {
            is Resource.Success -> {
                _uiState.value = UiState.Success(resource.data)
            }

            is Resource.Error -> {
                _uiState.value = UiState.Error(resource.message)
            }
        }
    }

    /** TODO(P2): navegar a "Nueva inspección" cuando exista la pantalla. */
    fun onNewInspectionClick() {
        showSnackbar("La pantalla \"Nueva inspección\" llega en una próxima entrega.")
    }

    /** TODO(P3/P5): abrir la ejecución (si está en curso) o el detalle (si está finalizada). */
    fun onInspectionClick(inspection: Inspection) {
        val destination: String
        if (inspection.isFinished()) {
            destination = "el detalle"
        } else {
            destination = "la ejecución"
        }
        showSnackbar("Abrir $destination de \"${inspection.siteName}\" llega en una próxima entrega.")
    }

    private fun showSnackbar(message: String) {
        viewModelScope.launch {
            _effects.emit(HistoryUiEffect.ShowSnackbar(message))
        }
    }
}
