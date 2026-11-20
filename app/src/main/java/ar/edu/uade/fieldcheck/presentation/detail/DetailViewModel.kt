package ar.edu.uade.fieldcheck.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.core.model.UiState
import ar.edu.uade.fieldcheck.core.navigation.NavArguments
import ar.edu.uade.fieldcheck.domain.model.Inspection
import ar.edu.uade.fieldcheck.domain.model.InspectionDetail
import ar.edu.uade.fieldcheck.domain.model.ItemResult
import ar.edu.uade.fieldcheck.domain.model.ItemResultValue
import ar.edu.uade.fieldcheck.domain.model.ResultCounts
import ar.edu.uade.fieldcheck.domain.model.SyncStatus
import ar.edu.uade.fieldcheck.domain.usecase.BuildInspectionSummaryUseCase
import ar.edu.uade.fieldcheck.domain.usecase.GetInspectionDetailUseCase
import ar.edu.uade.fieldcheck.domain.usecase.RetrySyncUseCase
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
 * Lo que dibuja P5 cuando la inspección se leyó bien.
 *
 * El ViewModel ya deja hechos los cálculos (conteos y filtro de "No cumple")
 * para que la pantalla solo tenga que mostrar.
 *
 * @property notCompliesItems solo los ítems "No cumple", en orden: es lo que más le importa
 *   a quien recibe el informe.
 * @property lastSyncError motivo del último error de sincronización, o `null` si no hubo.
 */
data class DetailContent(
    val inspection: Inspection,
    val counts: ResultCounts,
    val notCompliesItems: List<ItemResult>,
    val lastSyncError: String?,
)

/**
 * Eventos que pasan una sola vez (no son estado): si se guardaran en el estado,
 * la hoja de compartir se volvería a abrir al rotar la pantalla.
 */
sealed interface DetailEffect {

    /** Abrir la hoja de compartir de Android con este texto (regla R7). */
    data class ShareSummary(val text: String) : DetailEffect

    /** Mostrar un mensaje corto (Snackbar), por ejemplo si falló "Reintentar". */
    data class ShowMessage(val message: String) : DetailEffect
}

/**
 * ViewModel de la pantalla P5 · Detalle de inspección.
 *
 * Responsabilidades:
 * 1. Observar la inspección finalizada y traducirla a [UiState] para la UI (solo lectura, R6).
 * 2. "Reintentar" la sincronización cuando falló (S6: sin backend, solo vuelve a PENDING).
 * 3. Armar el resumen de texto y pedirle a la pantalla que lo comparta (R7).
 *
 * Abrir la hoja de compartir necesita un `Context` de Android, así que el ViewModel
 * no lo hace: emite un [DetailEffect.ShareSummary] y la pantalla se encarga.
 */
@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getInspectionDetail: GetInspectionDetailUseCase,
    private val retrySync: RetrySyncUseCase,
    private val buildInspectionSummary: BuildInspectionSummaryUseCase,
) : ViewModel() {

    /** Id que llega por navegación; puede faltar si la ruta se armó mal (caso límite). */
    private val inspectionId: String? = savedStateHandle.get<String>(NavArguments.INSPECTION_ID)

    private val _uiState = MutableStateFlow<UiState<DetailContent>>(UiState.Loading)
    val uiState: StateFlow<UiState<DetailContent>> = _uiState.asStateFlow()

    /** Sin `replay`: un evento ya consumido no se repite al volver a suscribirse. */
    private val _effects = MutableSharedFlow<DetailEffect>()
    val effects: SharedFlow<DetailEffect> = _effects.asSharedFlow()

    /**
     * Último detalle leído. Lo guardamos completo (y no solo [DetailContent]) porque
     * el resumen y el reintento necesitan la inspección con todos sus ítems.
     */
    private var currentDetail: InspectionDetail? = null

    /** Guardamos el trabajo de lectura para poder cancelarlo si el usuario toca "Reintentar". */
    private var detailJob: Job? = null

    /** Evita encolar dos veces si el usuario toca "Reintentar" muy rápido. */
    private var isRetryingSync = false

    init {
        loadDetail()
    }

    /** Empieza (o vuelve a empezar) a observar la inspección guardada en el teléfono. */
    fun loadDetail() {
        detailJob?.cancel()

        val id = inspectionId
        if (id == null) {
            _uiState.value = UiState.Error(NOT_FOUND_MESSAGE)
            return
        }

        _uiState.value = UiState.Loading
        detailJob = viewModelScope.launch {
            getInspectionDetail(id).collect { resource ->
                handleDetailResult(resource)
            }
        }
    }

    /**
     * "Reintentar" de la tarjeta de error.
     *
     * Solo hace algo si la sincronización falló: el botón solo se ve en ese estado,
     * pero el ViewModel no confía en la UI para hacer cumplir la regla.
     */
    fun onRetrySyncClick() {
        val detail = currentDetail
        if (detail == null) {
            return
        }

        val inspection = detail.inspection
        val syncFailed = inspection.syncStatus == SyncStatus.FAILED
        if (!syncFailed || isRetryingSync) {
            return
        }

        isRetryingSync = true
        viewModelScope.launch {
            val result = retrySync(inspection)
            handleRetryResult(result)
            isRetryingSync = false
        }
    }

    /** "Compartir resumen": arma el texto y le pide a la pantalla que abra la hoja de compartir. */
    fun onShareClick() {
        val detail = currentDetail
        if (detail == null) {
            return
        }

        val summary = buildInspectionSummary(detail)
        viewModelScope.launch {
            _effects.emit(DetailEffect.ShareSummary(summary))
        }
    }

    private fun handleDetailResult(resource: Resource<InspectionDetail>) {
        when (resource) {
            is Resource.Success -> {
                val detail = resource.data
                currentDetail = detail
                _uiState.value = UiState.Success(toContent(detail))
            }

            is Resource.Error -> {
                currentDetail = null
                _uiState.value = UiState.Error(resource.message)
            }
        }
    }

    /**
     * Si salió bien no hay nada que hacer acá: la base cambia a PENDING y el
     * observador del detalle actualiza la tarjeta solo.
     */
    private suspend fun handleRetryResult(result: Resource<Unit>) {
        when (result) {
            is Resource.Success -> {
                // La pantalla se actualiza sola al cambiar la base.
            }

            is Resource.Error -> {
                _effects.emit(DetailEffect.ShowMessage(result.message))
            }
        }
    }

    private fun toContent(detail: InspectionDetail): DetailContent {
        val counts = detail.countResults()
        val notCompliesItems = findNotCompliesItems(detail.items)

        return DetailContent(
            inspection = detail.inspection,
            counts = counts,
            notCompliesItems = notCompliesItems,
            lastSyncError = detail.lastSyncError,
        )
    }

    /** Los ítems ya vienen ordenados por posición desde el repositorio; solo filtramos. */
    private fun findNotCompliesItems(items: List<ItemResult>): List<ItemResult> {
        val notCompliesItems = mutableListOf<ItemResult>()
        for (item in items) {
            if (item.result == ItemResultValue.NOT_COMPLIES) {
                notCompliesItems.add(item)
            }
        }
        return notCompliesItems
    }

    private companion object {
        const val NOT_FOUND_MESSAGE = "No encontramos esta inspección."
    }
}
