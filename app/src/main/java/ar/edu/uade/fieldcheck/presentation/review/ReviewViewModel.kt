package ar.edu.uade.fieldcheck.presentation.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.core.model.UiState
import ar.edu.uade.fieldcheck.core.navigation.NavArguments
import ar.edu.uade.fieldcheck.domain.model.FinalizationCheck
import ar.edu.uade.fieldcheck.domain.model.InspectionDetail
import ar.edu.uade.fieldcheck.domain.model.ItemResult
import ar.edu.uade.fieldcheck.domain.usecase.CheckFinalizationUseCase
import ar.edu.uade.fieldcheck.domain.usecase.FinalizeInspectionUseCase
import ar.edu.uade.fieldcheck.domain.usecase.GetInspectionDetailUseCase
import ar.edu.uade.fieldcheck.domain.usecase.ObserveConnectivityUseCase
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
 * ViewModel de la pantalla P4 · Revisión y finalización.
 *
 * Responsabilidades:
 * 1. Observar la inspección y armar el resumen: conteos, lista de ítems y faltantes (R4, R5).
 * 2. Manejar el flujo de "Finalizar inspección": si falta algo, mostrar los faltantes;
 *    si está todo, pedir confirmación y recién después finalizar.
 * 3. Avisar a la pantalla, con eventos de una sola vez, cuándo navegar a P5 o mostrar un error.
 *
 * El botón "Finalizar" nunca se deshabilita: un botón gris no explica qué falta
 * (docs/pantallas.md, P4). Por eso la validación vive acá y responde mostrando los faltantes.
 */
@HiltViewModel
class ReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getInspectionDetail: GetInspectionDetailUseCase,
    private val checkFinalization: CheckFinalizationUseCase,
    private val finalizeInspection: FinalizeInspectionUseCase,
    private val observeConnectivity: ObserveConnectivityUseCase,
) : ViewModel() {

    /** Id de la inspección que llega por navegación; `null` solo si la ruta vino mal armada. */
    private val inspectionId: String? = savedStateHandle.get<String>(NavArguments.INSPECTION_ID)

    private val _uiState = MutableStateFlow<UiState<ReviewContent>>(UiState.Loading)
    val uiState: StateFlow<UiState<ReviewContent>> = _uiState.asStateFlow()

    private val _finalizationState = MutableStateFlow(FinalizationUiState())
    val finalizationState: StateFlow<FinalizationUiState> = _finalizationState.asStateFlow()

    /** Arranca en `true` para que el banner no parpadee al abrir la pantalla con red. */
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    /** Navegar y mostrar un Snackbar pasan una sola vez: por eso van en un SharedFlow y no en el estado. */
    private val _events = MutableSharedFlow<ReviewEvent>()
    val events: SharedFlow<ReviewEvent> = _events.asSharedFlow()

    /** Guardamos la lectura para cancelarla si el usuario toca "Reintentar". */
    private var detailJob: Job? = null

    init {
        loadInspection()
        observeNetwork()
    }

    /** Empieza (o vuelve a empezar) a observar la inspección guardada en el teléfono. */
    fun loadInspection() {
        detailJob?.cancel()

        val currentId = inspectionId
        if (currentId == null) {
            _uiState.value = UiState.Error(INSPECTION_NOT_FOUND_MESSAGE)
            return
        }

        _uiState.value = UiState.Loading
        detailJob = viewModelScope.launch {
            getInspectionDetail(currentId).collect { resource ->
                handleDetailResult(currentId, resource)
            }
        }
    }

    /**
     * El usuario tocó "Finalizar inspección".
     *
     * Con faltantes: no se finaliza y se muestra la lista filtrada con el aviso.
     * Sin faltantes: se abre el diálogo de confirmación, porque finalizar no tiene vuelta atrás.
     */
    fun onFinalizeClick() {
        val content = currentContent()
        if (content == null) {
            return
        }

        val hasMissingItems = content.missingItems.isNotEmpty()
        if (hasMissingItems) {
            _finalizationState.value = _finalizationState.value.copy(
                isShowingMissingItems = true,
                isConfirmDialogVisible = false,
            )
        } else {
            _finalizationState.value = _finalizationState.value.copy(
                isShowingMissingItems = false,
                isConfirmDialogVisible = true,
            )
        }
    }

    /** El usuario eligió "Cancelar" en el diálogo (o lo cerró): no pasa nada más. */
    fun onDismissConfirmDialog() {
        _finalizationState.value = _finalizationState.value.copy(isConfirmDialogVisible = false)
    }

    /**
     * El usuario confirmó en el diálogo: se finaliza una sola vez.
     *
     * Se ignora si el diálogo no estaba abierto o si ya hay una finalización en curso,
     * para que un doble toque no dispare dos escrituras.
     */
    fun onConfirmFinalize() {
        val state = _finalizationState.value
        val canStartFinalizing = state.isConfirmDialogVisible && !state.isFinalizing
        if (!canStartFinalizing) {
            return
        }

        val currentId = inspectionId
        if (currentId == null) {
            return
        }

        _finalizationState.value = state.copy(
            isConfirmDialogVisible = false,
            isFinalizing = true,
        )

        viewModelScope.launch {
            // El caso de uso vuelve a validar R5: la regla no depende de lo que muestre la pantalla.
            val result = finalizeInspection(currentId)
            handleFinalizeResult(currentId, result)
        }
    }

    private suspend fun handleFinalizeResult(currentId: String, result: Resource<Unit>) {
        when (result) {
            is Resource.Success -> {
                // Queda en "finalizando" a propósito: la pantalla se va a ir a P5 y no hay que
                // permitir otro toque mientras tanto.
                _events.emit(ReviewEvent.Finalized(currentId))
            }

            is Resource.Error -> {
                _finalizationState.value = _finalizationState.value.copy(isFinalizing = false)
                _events.emit(ReviewEvent.ShowMessage(result.message))
            }
        }
    }

    private fun handleDetailResult(currentId: String, resource: Resource<InspectionDetail>) {
        when (resource) {
            is Resource.Success -> {
                _uiState.value = UiState.Success(buildContent(currentId, resource.data))
            }

            is Resource.Error -> {
                _uiState.value = UiState.Error(resource.message)
            }
        }
    }

    /**
     * Arma lo que muestra la pantalla a partir de la inspección.
     *
     * Se recalcula cada vez que cambian los datos: si el usuario vuelve de P3 con un ítem
     * corregido, el faltante desaparece solo.
     */
    private fun buildContent(currentId: String, detail: InspectionDetail): ReviewContent {
        val check = checkFinalization(detail.items)

        val reviewItems = mutableListOf<ReviewItem>()
        for (item in detail.items) {
            val problem = findProblem(item, check)
            reviewItems.add(ReviewItem(item = item, problem = problem))
        }

        val missingItems = mutableListOf<ReviewItem>()
        for (reviewItem in reviewItems) {
            if (reviewItem.problem != null) {
                missingItems.add(reviewItem)
            }
        }

        val missingMessage: String?
        if (check.canFinalize()) {
            missingMessage = null
        } else {
            missingMessage = check.missingMessage()
        }

        return ReviewContent(
            inspectionId = currentId,
            counts = detail.countResults(),
            items = reviewItems,
            missingItems = missingItems,
            missingMessage = missingMessage,
        )
    }

    /** Traduce el resultado del caso de uso al motivo que se le muestra al usuario en el ítem. */
    private fun findProblem(item: ItemResult, check: FinalizationCheck): ReviewProblem? {
        if (check.itemsWithoutResult.contains(item)) {
            return ReviewProblem.WITHOUT_RESULT
        }
        if (check.notCompliesWithoutEvidence.contains(item)) {
            return ReviewProblem.NOT_COMPLIES_WITHOUT_EVIDENCE
        }
        return null
    }

    private fun currentContent(): ReviewContent? {
        val state = _uiState.value
        if (state is UiState.Success) {
            return state.data
        }
        return null
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            observeConnectivity().collect { online ->
                _isOnline.value = online
            }
        }
    }

    private companion object {
        const val INSPECTION_NOT_FOUND_MESSAGE = "No encontramos esta inspección."
    }
}
