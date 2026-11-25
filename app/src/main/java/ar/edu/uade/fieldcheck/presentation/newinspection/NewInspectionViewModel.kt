package ar.edu.uade.fieldcheck.presentation.newinspection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.core.model.UiState
import ar.edu.uade.fieldcheck.domain.model.CreateInspectionResult
import ar.edu.uade.fieldcheck.domain.model.Template
import ar.edu.uade.fieldcheck.domain.usecase.CreateInspectionUseCase
import ar.edu.uade.fieldcheck.domain.usecase.GetTemplatesUseCase
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
 * Estado de la hoja inferior "Empezar inspección" de P2.
 *
 * @property isSiteMissing `true` si se intentó empezar con el sitio vacío: el campo se marca en rojo.
 * @property creationError error al guardar la inspección; se muestra arriba del botón.
 * @property isCreating `true` mientras se guarda: deshabilita el botón para evitar el doble toque.
 */
data class StartSheetState(
    val template: Template,
    val siteName: String,
    val isSiteMissing: Boolean,
    val creationError: String?,
    val isCreating: Boolean,
)

/** Eventos que ocurren una sola vez (no son estado): por ejemplo, navegar a la ejecución. */
sealed interface NewInspectionEvent {

    data class InspectionStarted(val inspectionId: String) : NewInspectionEvent
}

/**
 * ViewModel de la pantalla P2 · Nueva inspección.
 *
 * Responsabilidades:
 * 1. Mostrar las plantillas guardadas en el teléfono (o el error / vacío).
 * 2. Informar si hay conexión, para el banner y el estado vacío "sin conexión".
 * 3. Manejar la hoja inferior: plantilla elegida, sitio escrito y creación de la inspección.
 *
 * La validación del sitio NO se repite acá: la hace [CreateInspectionUseCase] (regla R1),
 * y este ViewModel solo traduce su respuesta a estado de pantalla.
 */
@HiltViewModel
class NewInspectionViewModel @Inject constructor(
    private val getTemplates: GetTemplatesUseCase,
    private val createInspection: CreateInspectionUseCase,
    private val observeConnectivity: ObserveConnectivityUseCase,
) : ViewModel() {

    private val _templatesState = MutableStateFlow<UiState<List<Template>>>(UiState.Loading)
    val templatesState: StateFlow<UiState<List<Template>>> = _templatesState.asStateFlow()

    /** Arranca en `true` para que el banner no aparezca un instante al abrir la pantalla con red. */
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    /** `null` significa que la hoja inferior está cerrada. */
    private val _startSheetState = MutableStateFlow<StartSheetState?>(null)
    val startSheetState: StateFlow<StartSheetState?> = _startSheetState.asStateFlow()

    /**
     * Navegar es un evento de una sola vez: si fuera estado, al volver a esta pantalla
     * se volvería a navegar. Por eso va en un SharedFlow y no en un StateFlow.
     */
    private val _events = MutableSharedFlow<NewInspectionEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<NewInspectionEvent> = _events.asSharedFlow()

    /** Guardamos el trabajo de lectura para poder cancelarlo si el usuario toca "Reintentar". */
    private var templatesJob: Job? = null

    init {
        loadTemplates()
        observeNetwork()
    }

    /** Empieza (o vuelve a empezar) a observar las plantillas guardadas. */
    fun loadTemplates() {
        templatesJob?.cancel()
        _templatesState.value = UiState.Loading

        templatesJob = viewModelScope.launch {
            getTemplates().collect { resource ->
                handleTemplatesResult(resource)
            }
        }
    }

    /** El usuario tocó una tarjeta: se abre la hoja inferior con el sitio vacío. */
    fun onTemplateSelected(template: Template) {
        _startSheetState.value = StartSheetState(
            template = template,
            siteName = "",
            isSiteMissing = false,
            creationError = null,
            isCreating = false,
        )
    }

    /** Cada tecla actualiza el sitio y borra los errores: ya no corresponden al texto nuevo. */
    fun onSiteNameChange(siteName: String) {
        val currentSheet = _startSheetState.value
        if (currentSheet == null) {
            return
        }
        _startSheetState.value = currentSheet.copy(
            siteName = siteName,
            isSiteMissing = false,
            creationError = null,
        )
    }

    fun onSheetDismissed() {
        val currentSheet = _startSheetState.value
        // Mientras se guarda no se cierra: si no, el usuario perdería el aviso de error.
        if (currentSheet != null && currentSheet.isCreating) {
            return
        }
        _startSheetState.value = null
    }

    /**
     * "Empezar inspección". Marca `isCreating` ANTES de lanzar la corrutina: así un segundo
     * toque que llegue mientras se guarda encuentra la marca y no crea otra inspección.
     */
    fun onStartInspectionClick() {
        val currentSheet = _startSheetState.value
        if (currentSheet == null) {
            return
        }
        if (currentSheet.isCreating) {
            return
        }

        _startSheetState.value = currentSheet.copy(
            isCreating = true,
            isSiteMissing = false,
            creationError = null,
        )

        viewModelScope.launch {
            val result = createInspection(currentSheet.template, currentSheet.siteName)
            handleCreationResult(result)
        }
    }

    private fun handleTemplatesResult(resource: Resource<List<Template>>) {
        when (resource) {
            is Resource.Success -> {
                _templatesState.value = UiState.Success(resource.data)
            }

            is Resource.Error -> {
                _templatesState.value = UiState.Error(resource.message)
            }
        }
    }

    /** El sitio vacío se marca en el campo; una falla al guardar se muestra arriba del botón. */
    private fun handleCreationResult(result: CreateInspectionResult) {
        when (result) {
            is CreateInspectionResult.Created -> {
                _startSheetState.value = null
                _events.tryEmit(NewInspectionEvent.InspectionStarted(result.inspectionId))
            }

            is CreateInspectionResult.SiteRequired -> {
                updateSheet { sheet -> sheet.copy(isCreating = false, isSiteMissing = true) }
            }

            is CreateInspectionResult.Failed -> {
                updateSheet { sheet -> sheet.copy(isCreating = false, creationError = result.message) }
            }
        }
    }

    /** Cambia la hoja solo si sigue abierta. */
    private fun updateSheet(change: (StartSheetState) -> StartSheetState) {
        val currentSheet = _startSheetState.value
        if (currentSheet != null) {
            _startSheetState.value = change(currentSheet)
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
