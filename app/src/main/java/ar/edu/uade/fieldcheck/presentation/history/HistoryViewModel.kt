package ar.edu.uade.fieldcheck.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.core.model.UiState
import ar.edu.uade.fieldcheck.domain.model.InspectionHistory
import ar.edu.uade.fieldcheck.domain.usecase.GetInspectionHistoryUseCase
import ar.edu.uade.fieldcheck.domain.usecase.ObserveConnectivityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel de la pantalla P1 · Historial.
 *
 * Responsabilidades:
 * 1. Pedir el historial al caso de uso y traducirlo a [UiState] para la UI.
 * 2. Informar si hay conexión, para mostrar el banner "Sin conexión".
 *
 * La navegación (tocar una tarjeta o "Nueva inspección") no pasa por acá: la pantalla
 * avisa por callbacks y `FieldCheckApp` decide a dónde ir. No hay lógica que testear
 * en "ir a otra pantalla", así que no hace falta un evento one-shot.
 *
 * No conoce Room ni Compose: solo habla con casos de uso. Por eso se puede testear
 * con un repositorio falso.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getInspectionHistory: GetInspectionHistoryUseCase,
    private val observeConnectivity: ObserveConnectivityUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<InspectionHistory>>(UiState.Loading)
    val uiState: StateFlow<UiState<InspectionHistory>> = _uiState.asStateFlow()

    /** Arranca en `true` para que el banner no aparezca un instante al abrir la pantalla con red. */
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    /** Guardamos el trabajo de lectura para poder cancelarlo si el usuario toca "Reintentar". */
    private var historyJob: Job? = null

    init {
        loadHistory()
        observeNetwork()
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

    private fun observeNetwork() {
        viewModelScope.launch {
            observeConnectivity().collect { online ->
                _isOnline.value = online
            }
        }
    }
}
