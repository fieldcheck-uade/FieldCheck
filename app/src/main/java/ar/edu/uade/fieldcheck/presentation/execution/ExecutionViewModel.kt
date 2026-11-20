package ar.edu.uade.fieldcheck.presentation.execution

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.core.model.UiState
import ar.edu.uade.fieldcheck.core.navigation.NavArguments
import ar.edu.uade.fieldcheck.domain.model.Evidence
import ar.edu.uade.fieldcheck.domain.model.InspectionDetail
import ar.edu.uade.fieldcheck.domain.model.ItemResult
import ar.edu.uade.fieldcheck.domain.model.ItemResultValue
import ar.edu.uade.fieldcheck.domain.model.PendingPhoto
import ar.edu.uade.fieldcheck.domain.usecase.AddEvidenceUseCase
import ar.edu.uade.fieldcheck.domain.usecase.DeleteEvidenceUseCase
import ar.edu.uade.fieldcheck.domain.usecase.DiscardPhotoUseCase
import ar.edu.uade.fieldcheck.domain.usecase.GetInspectionDetailUseCase
import ar.edu.uade.fieldcheck.domain.usecase.GetStartPositionUseCase
import ar.edu.uade.fieldcheck.domain.usecase.ObserveConnectivityUseCase
import ar.edu.uade.fieldcheck.domain.usecase.PreparePhotoCaptureUseCase
import ar.edu.uade.fieldcheck.domain.usecase.SaveItemResultUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel de la pantalla P3 · Ejecución de la inspección.
 *
 * Responsabilidades:
 * 1. Observar la inspección guardada y decidir en qué ítem arrancar (regla R2).
 * 2. Guardar cada cambio al instante: resultado, nota y fotos (regla R3).
 * 3. Avisar cuando un "No cumple" no tiene evidencia (regla R4).
 * 4. Pedirle a la pantalla, con eventos de una sola vez, que abra la cámara o vaya a P4.
 *
 * Decisión sobre inspecciones finalizadas (R6): si se abre una, se muestra en modo
 * solo lectura (se puede recorrer pero todos los cambios se ignoran). Es lo más simple
 * y seguro: no depende de la navegación y nunca modifica datos ya cerrados.
 */
@HiltViewModel
class ExecutionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getInspectionDetail: GetInspectionDetailUseCase,
    private val getStartPosition: GetStartPositionUseCase,
    private val saveItemResult: SaveItemResultUseCase,
    private val addEvidence: AddEvidenceUseCase,
    private val deleteEvidence: DeleteEvidenceUseCase,
    private val preparePhotoCapture: PreparePhotoCaptureUseCase,
    private val discardPhoto: DiscardPhotoUseCase,
    private val observeConnectivity: ObserveConnectivityUseCase,
) : ViewModel() {

    private val inspectionId: String? = savedStateHandle.get<String>(NavArguments.INSPECTION_ID)
    private val requestedPosition: Int = readRequestedPosition(savedStateHandle)

    private val _uiState = MutableStateFlow<UiState<ExecutionContent>>(UiState.Loading)
    val uiState: StateFlow<UiState<ExecutionContent>> = _uiState.asStateFlow()

    /** Arranca en `true` para que el banner no aparezca un instante al abrir la pantalla con red. */
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _events = MutableSharedFlow<ExecutionEvent>(extraBufferCapacity = EVENT_BUFFER_SIZE)
    val events: SharedFlow<ExecutionEvent> = _events.asSharedFlow()

    /** Última versión de la inspección que llegó de la base. */
    private var latestDetail: InspectionDetail? = null

    /** Índice (en la lista de ítems) del ítem que se está mostrando. */
    private var currentIndex: Int = 0

    /** `true` después de elegir el ítem inicial: las lecturas siguientes no deben mover al usuario. */
    private var hasChosenStartItem: Boolean = false

    /** Texto del campo de nota; puede ir adelante de lo guardado mientras el usuario escribe. */
    private var noteDraft: String = ""

    private var evidencePendingDeletion: Evidence? = null

    /** Foto preparada para la cámara y el ítem al que pertenece, mientras la cámara está abierta. */
    private var pendingPhoto: PendingPhoto? = null
    private var pendingPhotoItemId: Long? = null

    private var detailJob: Job? = null
    private var noteSaveJob: Job? = null
    private var autoAdvanceJob: Job? = null

    init {
        loadInspection()
        observeNetwork()
    }

    // region Carga

    /** Empieza (o vuelve a empezar, con "Reintentar") a observar la inspección. */
    fun loadInspection() {
        detailJob?.cancel()
        _uiState.value = UiState.Loading

        val id = inspectionId
        if (id == null) {
            _uiState.value = UiState.Error(MESSAGE_NOT_FOUND)
            return
        }

        detailJob = viewModelScope.launch {
            getInspectionDetail(id).collect { resource ->
                handleDetailResult(resource)
            }
        }
    }

    private fun handleDetailResult(resource: Resource<InspectionDetail>) {
        when (resource) {
            is Resource.Success -> {
                onDetailLoaded(resource.data)
            }

            is Resource.Error -> {
                _uiState.value = UiState.Error(resource.message)
            }
        }
    }

    private fun onDetailLoaded(detail: InspectionDetail) {
        if (detail.items.isEmpty()) {
            _uiState.value = UiState.Error(MESSAGE_NO_ITEMS)
            return
        }

        latestDetail = detail

        // Solo la primera lectura decide dónde arrancar. Las siguientes llegan porque
        // guardamos algo, y en ese caso el usuario tiene que quedarse donde está.
        if (!hasChosenStartItem) {
            val startPosition = getStartPosition(detail.items, requestedPosition)
            currentIndex = findIndexOfPosition(detail.items, startPosition)
            noteDraft = savedNoteOf(detail.items[currentIndex])
            hasChosenStartItem = true
        }

        // Por las dudas, si la lista se achicara, no nos salimos de los límites.
        val lastIndex = detail.items.size - 1
        if (currentIndex > lastIndex) {
            currentIndex = lastIndex
        }

        publishContent()
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            observeConnectivity().collect { online ->
                _isOnline.value = online
            }
        }
    }

    // endregion

    // region Navegación entre ítems

    fun onNextClick() {
        val detail = latestDetail
        if (detail == null) {
            return
        }

        val isLastItem = currentIndex == detail.items.size - 1
        if (isLastItem) {
            goToReview()
        } else {
            moveToIndex(currentIndex + 1)
        }
    }

    fun onPreviousClick() {
        if (currentIndex == 0) {
            return
        }
        moveToIndex(currentIndex - 1)
    }

    /** Ícono de lista de la barra superior: "Ver todos" (P4). */
    fun onReviewClick() {
        goToReview()
    }

    /**
     * La pantalla se va (atrás o la app pasa a segundo plano): se guarda ya la nota
     * que estaba esperando, para no perder lo último que se escribió.
     */
    fun onLeaveScreen() {
        saveNoteNowIfPending()
    }

    private fun goToReview() {
        val id = inspectionId
        if (id == null) {
            return
        }
        saveNoteNowIfPending()
        autoAdvanceJob?.cancel()
        sendEvent(ExecutionEvent.NavigateToReview(id))
    }

    private fun moveToIndex(newIndex: Int) {
        val detail = latestDetail
        if (detail == null) {
            return
        }

        // Antes de cambiar de ítem se guarda la nota pendiente del ítem que se deja.
        saveNoteNowIfPending()
        autoAdvanceJob?.cancel()

        currentIndex = newIndex
        noteDraft = savedNoteOf(detail.items[newIndex])
        evidencePendingDeletion = null
        publishContent()
    }

    // endregion

    // region Resultado y nota

    /**
     * Toque en el [ResultSelector][ar.edu.uade.fieldcheck.presentation.common.components.ResultSelector].
     *
     * - Tocar el resultado ya elegido lo deja sin completar (corrige un toque accidental).
     * - Cumple y N/A avanzan solos al siguiente ítem tras una pausa breve.
     * - No cumple se queda, porque lo esperable es agregar evidencia.
     */
    fun onResultClick(tapped: ItemResultValue) {
        val item = currentItem()
        if (item == null || isReadOnly()) {
            return
        }

        val newResult: ItemResultValue?
        if (item.result == tapped) {
            newResult = null
        } else {
            newResult = tapped
        }

        // El resultado se guarda junto con la nota actual, así que la nota pendiente ya no hace falta.
        noteSaveJob?.cancel()
        autoAdvanceJob?.cancel()
        val noteToSave = noteDraft

        viewModelScope.launch {
            val saveResult = saveItemResult(item.id, newResult, noteToSave)
            reportIfFailed(saveResult)
        }

        val shouldAdvance = newResult == ItemResultValue.COMPLIES || newResult == ItemResultValue.NOT_APPLICABLE
        if (shouldAdvance) {
            scheduleAutoAdvance(fromIndex = currentIndex)
        }
    }

    /** Cada letra actualiza el campo; el guardado espera a que el usuario deje de escribir. */
    fun onNoteChange(text: String) {
        if (isReadOnly()) {
            return
        }

        noteDraft = text
        publishContent()

        noteSaveJob?.cancel()
        noteSaveJob = viewModelScope.launch {
            delay(NOTE_SAVE_DELAY_MILLIS)
            saveCurrentNote()
        }
    }

    private fun scheduleAutoAdvance(fromIndex: Int) {
        autoAdvanceJob = viewModelScope.launch {
            delay(AUTO_ADVANCE_DELAY_MILLIS)

            // Si en la pausa el usuario ya se movió a otro ítem, no lo movemos de nuevo.
            val isStillOnSameItem = currentIndex == fromIndex
            if (isStillOnSameItem) {
                onNextClickWithoutReview()
            }
        }
    }

    /** El avance automático nunca abre P4: en el último ítem simplemente se queda. */
    private fun onNextClickWithoutReview() {
        val detail = latestDetail
        if (detail == null) {
            return
        }
        val hasNextItem = currentIndex < detail.items.size - 1
        if (hasNextItem) {
            moveToIndex(currentIndex + 1)
        }
    }

    private fun saveNoteNowIfPending() {
        val job = noteSaveJob
        if (job == null || !job.isActive) {
            return
        }
        job.cancel()

        // Se toman el ítem y el texto AHORA, antes de que el usuario cambie de ítem.
        val item = currentItem()
        if (item == null) {
            return
        }
        val noteToSave = noteDraft
        viewModelScope.launch {
            saveNote(item, noteToSave)
        }
    }

    private suspend fun saveCurrentNote() {
        val item = currentItem()
        if (item == null) {
            return
        }
        saveNote(item, noteDraft)
    }

    /** Se guarda el resultado que ya tiene el ítem: escribir una nota no lo cambia. */
    private suspend fun saveNote(item: ItemResult, note: String) {
        val saveResult = saveItemResult(item.id, item.result, note)
        reportIfFailed(saveResult)
    }

    // endregion

    // region Fotos

    /** "Agregar foto": prepara el archivo y le pide a la pantalla que abra la cámara. */
    fun onAddPhotoClick() {
        val item = currentItem()
        if (item == null || isReadOnly()) {
            return
        }

        when (val preparation = preparePhotoCapture()) {
            is Resource.Success -> {
                pendingPhoto = preparation.data
                pendingPhotoItemId = item.id
                sendEvent(ExecutionEvent.OpenCamera(preparation.data.contentUri))
            }

            is Resource.Error -> {
                sendEvent(ExecutionEvent.ShowError(preparation.message))
            }
        }
    }

    /** La cámara volvió: con foto se guarda como evidencia; si se canceló, se borra el archivo vacío. */
    fun onPhotoResult(success: Boolean) {
        val photo = pendingPhoto
        val itemId = pendingPhotoItemId
        pendingPhoto = null
        pendingPhotoItemId = null

        if (photo == null || itemId == null) {
            return
        }

        if (!success) {
            discardPhoto(photo.filePath)
            return
        }

        viewModelScope.launch {
            val saveResult = addEvidence(itemId, photo.filePath)
            when (saveResult) {
                is Resource.Success -> {
                    sendEvent(ExecutionEvent.PhotoSaved)
                }

                is Resource.Error -> {
                    discardPhoto(photo.filePath)
                    sendEvent(ExecutionEvent.ShowError(saveResult.message))
                }
            }
        }
    }

    /** No hay app de cámara: se borra el archivo preparado y se avisa. */
    fun onCameraUnavailable() {
        val photo = pendingPhoto
        pendingPhoto = null
        pendingPhotoItemId = null

        if (photo != null) {
            discardPhoto(photo.filePath)
        }
        sendEvent(ExecutionEvent.CameraUnavailable)
    }

    /** Mantener apretada una miniatura abre "¿Eliminar esta foto?". */
    fun onEvidenceLongClick(evidence: Evidence) {
        if (isReadOnly()) {
            return
        }
        evidencePendingDeletion = evidence
        publishContent()
    }

    fun onConfirmDeleteEvidence() {
        val evidence = evidencePendingDeletion
        evidencePendingDeletion = null
        publishContent()

        if (evidence == null) {
            return
        }

        viewModelScope.launch {
            val deleteResult = deleteEvidence(evidence.clientUuid)
            reportIfFailed(deleteResult)
        }
    }

    fun onDismissDeleteEvidence() {
        evidencePendingDeletion = null
        publishContent()
    }

    // endregion

    // region Armado del estado

    /** Vuelve a calcular lo que ve la pantalla a partir de la inspección y lo que hizo el usuario. */
    private fun publishContent() {
        val detail = latestDetail
        if (detail == null) {
            return
        }

        val item = detail.items[currentIndex]
        val content = ExecutionContent(
            siteName = detail.inspection.siteName,
            item = item,
            itemNumber = currentIndex + 1,
            totalItems = detail.items.size,
            noteDraft = noteDraft,
            showsEvidenceWarning = isMissingEvidence(item),
            isReadOnly = detail.inspection.isFinished(),
            evidencePendingDeletion = evidencePendingDeletion,
        )
        _uiState.value = UiState.Success(content)
    }

    /**
     * Regla R4, pero mirando la nota que el usuario está escribiendo y no solo la guardada:
     * así el aviso desaparece en cuanto empieza a escribir, sin esperar al guardado.
     */
    private fun isMissingEvidence(item: ItemResult): Boolean {
        val isNotComplies = item.result == ItemResultValue.NOT_COMPLIES
        val hasNote = noteDraft.isNotBlank()
        val hasPhotos = item.evidence.isNotEmpty()
        return isNotComplies && !hasNote && !hasPhotos
    }

    private fun currentItem(): ItemResult? {
        val detail = latestDetail
        if (detail == null) {
            return null
        }
        return detail.items[currentIndex]
    }

    private fun isReadOnly(): Boolean {
        val detail = latestDetail
        if (detail == null) {
            return true
        }
        return detail.inspection.isFinished()
    }

    private fun savedNoteOf(item: ItemResult): String {
        val note = item.note
        if (note == null) {
            return ""
        }
        return note
    }

    private fun findIndexOfPosition(items: List<ItemResult>, position: Int): Int {
        for (index in items.indices) {
            if (items[index].position == position) {
                return index
            }
        }
        return 0
    }

    private fun reportIfFailed(result: Resource<Unit>) {
        if (result is Resource.Error) {
            sendEvent(ExecutionEvent.ShowError(result.message))
        }
    }

    private fun sendEvent(event: ExecutionEvent) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }

    // endregion

    companion object {
        /** Pausa antes de guardar la nota: se espera a que el usuario deje de escribir. */
        const val NOTE_SAVE_DELAY_MILLIS = 500L

        /** Pausa antes de avanzar solo, para que se llegue a ver la selección. */
        const val AUTO_ADVANCE_DELAY_MILLIS = 400L

        private const val EVENT_BUFFER_SIZE = 8
        private const val MESSAGE_NOT_FOUND = "No encontramos esta inspección."
        private const val MESSAGE_NO_ITEMS = "Esta inspección no tiene ítems."

        /** Si la navegación no mandó posición, se usa "sin posición pedida" (R2 decide). */
        private fun readRequestedPosition(savedStateHandle: SavedStateHandle): Int {
            val position = savedStateHandle.get<Int>(NavArguments.START_POSITION)
            if (position == null) {
                return GetStartPositionUseCase.NO_REQUESTED_POSITION
            }
            return position
        }
    }
}
