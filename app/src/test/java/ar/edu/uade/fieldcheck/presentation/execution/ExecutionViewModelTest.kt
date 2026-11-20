package ar.edu.uade.fieldcheck.presentation.execution

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.core.model.UiState
import ar.edu.uade.fieldcheck.core.navigation.NavArguments
import ar.edu.uade.fieldcheck.domain.model.InspectionStatus
import ar.edu.uade.fieldcheck.domain.model.ItemResult
import ar.edu.uade.fieldcheck.domain.model.ItemResultValue
import ar.edu.uade.fieldcheck.domain.usecase.AddEvidenceUseCase
import ar.edu.uade.fieldcheck.domain.usecase.DeleteEvidenceUseCase
import ar.edu.uade.fieldcheck.domain.usecase.DiscardPhotoUseCase
import ar.edu.uade.fieldcheck.domain.usecase.GetInspectionDetailUseCase
import ar.edu.uade.fieldcheck.domain.usecase.GetStartPositionUseCase
import ar.edu.uade.fieldcheck.domain.usecase.ObserveConnectivityUseCase
import ar.edu.uade.fieldcheck.domain.usecase.PreparePhotoCaptureUseCase
import ar.edu.uade.fieldcheck.domain.usecase.SaveItemResultUseCase
import ar.edu.uade.fieldcheck.fakes.FakeInspectionRepository
import ar.edu.uade.fieldcheck.fakes.FakeNetworkMonitor
import ar.edu.uade.fieldcheck.fakes.FakePhotoStorage
import ar.edu.uade.fieldcheck.fakes.SavedItemResult
import ar.edu.uade.fieldcheck.fakes.createTestClock
import ar.edu.uade.fieldcheck.fakes.createTestDetail
import ar.edu.uade.fieldcheck.fakes.createTestEvidence
import ar.edu.uade.fieldcheck.fakes.createTestInspection
import ar.edu.uade.fieldcheck.fakes.createTestItemResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExecutionViewModelTest {

    private val inspectionId = "inspection-1"
    private val now = 5_000L

    private lateinit var repository: FakeInspectionRepository
    private lateinit var photoStorage: FakePhotoStorage
    private lateinit var networkMonitor: FakeNetworkMonitor

    /**
     * El hilo principal se reemplaza por un dispatcher de test. `runTest` comparte su reloj
     * virtual, así `advanceTimeBy` controla las esperas del ViewModel (nota y avance automático).
     */
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeInspectionRepository()
        photoStorage = FakePhotoStorage()
        networkMonitor = FakeNetworkMonitor()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Inicio (R2) ---

    @Test
    fun `starts at the first item without result`() = runTest {
        givenItems(
            createTestItemResult(position = 0, result = ItemResultValue.COMPLIES),
            createTestItemResult(position = 1, result = ItemResultValue.COMPLIES),
            createTestItemResult(position = 2),
            createTestItemResult(position = 3),
        )

        val viewModel = createViewModel()

        val content = currentContent(viewModel)
        assertEquals(2, content.item.position)
        assertEquals(3, content.itemNumber)
        assertEquals(4, content.totalItems)
    }

    @Test
    fun `starts at the requested position when navigation asks for one`() = runTest {
        givenItems(
            createTestItemResult(position = 0),
            createTestItemResult(position = 1),
            createTestItemResult(position = 2),
        )

        val viewModel = createViewModel(startPosition = 2)

        assertEquals(2, currentContent(viewModel).item.position)
    }

    // --- Navegación ---

    @Test
    fun `next and previous move between items and stop at the bounds`() = runTest {
        givenItems(
            createTestItemResult(position = 0),
            createTestItemResult(position = 1),
        )
        val viewModel = createViewModel()
        assertTrue(currentContent(viewModel).isFirstItem())

        viewModel.onPreviousClick()
        assertEquals(0, currentContent(viewModel).item.position)

        viewModel.onNextClick()
        assertEquals(1, currentContent(viewModel).item.position)
        assertTrue(currentContent(viewModel).isLastItem())

        viewModel.onPreviousClick()
        assertEquals(0, currentContent(viewModel).item.position)
    }

    @Test
    fun `next on the last item goes to review`() = runTest {
        givenItems(
            createTestItemResult(position = 0, result = ItemResultValue.COMPLIES),
            createTestItemResult(position = 1, result = ItemResultValue.COMPLIES),
        )
        val viewModel = createViewModel()
        assertTrue(currentContent(viewModel).isLastItem())

        viewModel.events.test {
            viewModel.onNextClick()

            assertEquals(ExecutionEvent.NavigateToReview(inspectionId), awaitItem())
        }
        assertEquals(1, currentContent(viewModel).item.position)
    }

    @Test
    fun `the list icon goes to review`() = runTest {
        givenItems(createTestItemResult(position = 0))
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onReviewClick()

            assertEquals(ExecutionEvent.NavigateToReview(inspectionId), awaitItem())
        }
    }

    // --- Resultado (R3) ---

    @Test
    fun `selecting a result saves it and shows the saved indicator`() = runTest {
        givenItems(createTestItemResult(position = 0), createTestItemResult(position = 1))
        val viewModel = createViewModel()
        assertFalse(currentContent(viewModel).showsSavedIndicator())

        viewModel.onResultClick(ItemResultValue.NOT_COMPLIES)

        val expected = SavedItemResult(itemResultId = 1L, result = ItemResultValue.NOT_COMPLIES, note = null, now = now)
        assertEquals(listOf(expected), repository.savedItemResults)
        assertTrue(currentContent(viewModel).showsSavedIndicator())
    }

    @Test
    fun `tapping the selected result again clears it`() = runTest {
        givenItems(createTestItemResult(position = 0, result = ItemResultValue.NOT_COMPLIES, note = "Falta cartel"))
        val viewModel = createViewModel()

        viewModel.onResultClick(ItemResultValue.NOT_COMPLIES)

        val saved = repository.savedItemResults.last()
        assertNull(saved.result)
        assertEquals("Falta cartel", saved.note)
        assertNull(currentContent(viewModel).item.result)
    }

    @Test
    fun `complies moves to the next item after a short pause`() = runTest {
        givenItems(createTestItemResult(position = 0), createTestItemResult(position = 1))
        val viewModel = createViewModel()

        viewModel.onResultClick(ItemResultValue.COMPLIES)
        assertEquals(0, currentContent(viewModel).item.position)

        advanceTimeBy(ExecutionViewModel.AUTO_ADVANCE_DELAY_MILLIS + 1)
        assertEquals(1, currentContent(viewModel).item.position)
    }

    @Test
    fun `not complies stays on the item to add evidence`() = runTest {
        givenItems(createTestItemResult(position = 0), createTestItemResult(position = 1))
        val viewModel = createViewModel()

        viewModel.onResultClick(ItemResultValue.NOT_COMPLIES)
        advanceTimeBy(ExecutionViewModel.AUTO_ADVANCE_DELAY_MILLIS + 1)

        assertEquals(0, currentContent(viewModel).item.position)
    }

    @Test
    fun `a failed save is reported to the user`() = runTest {
        givenItems(createTestItemResult(position = 0))
        repository.writeResult = Resource.Error("No se pudo guardar.")
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onResultClick(ItemResultValue.NOT_COMPLIES)

            assertEquals(ExecutionEvent.ShowError("No se pudo guardar."), awaitItem())
        }
    }

    // --- Nota ---

    @Test
    fun `the note is saved when the user stops typing and blank notes are saved as null`() = runTest {
        givenItems(createTestItemResult(position = 0, result = ItemResultValue.COMPLIES, note = "Vieja"))
        val viewModel = createViewModel()

        viewModel.onNoteChange("Cartel roto")
        assertEquals("Cartel roto", currentContent(viewModel).noteDraft)
        assertTrue(repository.savedItemResults.isEmpty())

        advanceTimeBy(ExecutionViewModel.NOTE_SAVE_DELAY_MILLIS + 1)
        val savedNote = repository.savedItemResults.last()
        assertEquals("Cartel roto", savedNote.note)
        assertEquals(ItemResultValue.COMPLIES, savedNote.result)

        viewModel.onNoteChange("   ")
        advanceTimeBy(ExecutionViewModel.NOTE_SAVE_DELAY_MILLIS + 1)
        assertNull(repository.savedItemResults.last().note)
    }

    @Test
    fun `a pending note is saved right away when changing item`() = runTest {
        givenItems(createTestItemResult(position = 0), createTestItemResult(position = 1))
        val viewModel = createViewModel()

        viewModel.onNoteChange("Observación")
        viewModel.onNextClick()

        val saved = repository.savedItemResults.single()
        assertEquals(1L, saved.itemResultId)
        assertEquals("Observación", saved.note)
        assertEquals("", currentContent(viewModel).noteDraft)
    }

    @Test
    fun `a pending note is saved right away when leaving the screen`() = runTest {
        givenItems(createTestItemResult(position = 0))
        val viewModel = createViewModel()

        viewModel.onNoteChange("Observación")
        viewModel.onLeaveScreen()

        assertEquals("Observación", repository.savedItemResults.single().note)
    }

    // --- Aviso de evidencia (R4) ---

    @Test
    fun `not complies without evidence shows the warning until a note is written`() = runTest {
        givenItems(createTestItemResult(position = 0))
        val viewModel = createViewModel()
        assertFalse(currentContent(viewModel).showsEvidenceWarning)

        viewModel.onResultClick(ItemResultValue.NOT_COMPLIES)
        assertTrue(currentContent(viewModel).showsEvidenceWarning)

        viewModel.onNoteChange("Matafuego vencido")
        assertFalse(currentContent(viewModel).showsEvidenceWarning)
    }

    @Test
    fun `not complies with a photo does not show the warning`() = runTest {
        val photo = createTestEvidence()
        givenItems(createTestItemResult(position = 0, result = ItemResultValue.NOT_COMPLIES, evidence = listOf(photo)))

        val viewModel = createViewModel()

        assertFalse(currentContent(viewModel).showsEvidenceWarning)
    }

    // --- Fotos ---

    @Test
    fun `a captured photo is added as evidence of the current item`() = runTest {
        givenItems(createTestItemResult(position = 0))
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onAddPhotoClick()
            val openCamera = awaitItem()
            assertEquals(ExecutionEvent.OpenCamera(photoStorage.contentUriOfCreatedPhoto()), openCamera)

            viewModel.onPhotoResult(success = true)
            assertEquals(ExecutionEvent.PhotoSaved, awaitItem())
        }

        val added = repository.addedEvidence.single()
        assertEquals(1L, added.itemResultId)
        assertEquals("/files/evidence/photo-1.jpg", added.filePath)
        assertEquals(1, currentContent(viewModel).item.evidence.size)
        assertTrue(photoStorage.deletedFilePaths.isEmpty())
    }

    @Test
    fun `a cancelled camera discards the prepared photo file`() = runTest {
        givenItems(createTestItemResult(position = 0))
        val viewModel = createViewModel()

        viewModel.onAddPhotoClick()
        viewModel.onPhotoResult(success = false)

        assertEquals(listOf("/files/evidence/photo-1.jpg"), photoStorage.deletedFilePaths)
        assertTrue(repository.addedEvidence.isEmpty())
    }

    @Test
    fun `no camera app discards the photo file and tells the user`() = runTest {
        givenItems(createTestItemResult(position = 0))
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onAddPhotoClick()
            awaitItem()

            viewModel.onCameraUnavailable()
            assertEquals(ExecutionEvent.CameraUnavailable, awaitItem())
        }
        assertEquals(listOf("/files/evidence/photo-1.jpg"), photoStorage.deletedFilePaths)
    }

    @Test
    fun `deleting a photo asks for confirmation first`() = runTest {
        val photo = createTestEvidence(clientUuid = "photo-a")
        givenItems(createTestItemResult(position = 0, evidence = listOf(photo)))
        val viewModel = createViewModel()

        viewModel.onEvidenceLongClick(photo)
        assertEquals(photo, currentContent(viewModel).evidencePendingDeletion)

        viewModel.onConfirmDeleteEvidence()

        assertEquals(listOf("photo-a"), repository.deletedEvidenceIds)
        assertNull(currentContent(viewModel).evidencePendingDeletion)
    }

    @Test
    fun `cancelling the delete dialog keeps the photo`() = runTest {
        val photo = createTestEvidence(clientUuid = "photo-a")
        givenItems(createTestItemResult(position = 0, evidence = listOf(photo)))
        val viewModel = createViewModel()

        viewModel.onEvidenceLongClick(photo)
        viewModel.onDismissDeleteEvidence()

        assertTrue(repository.deletedEvidenceIds.isEmpty())
        assertNull(currentContent(viewModel).evidencePendingDeletion)
    }

    // --- Conexión, errores y solo lectura ---

    @Test
    fun `reports when the phone goes offline`() = runTest {
        givenItems(createTestItemResult(position = 0))
        val viewModel = createViewModel()
        assertTrue(viewModel.isOnline.value)

        networkMonitor.setOnline(false)

        assertFalse(viewModel.isOnline.value)
    }

    @Test
    fun `shows an error and retry reads the inspection again`() = runTest {
        repository.emitDetail(inspectionId, Resource.Error("No pudimos leer la inspección."))
        val viewModel = createViewModel()
        assertEquals(UiState.Error("No pudimos leer la inspección."), viewModel.uiState.value)

        givenItems(createTestItemResult(position = 0))
        viewModel.loadInspection()

        assertEquals(0, currentContent(viewModel).item.position)
    }

    @Test
    fun `a finished inspection is read only and ignores changes`() = runTest {
        val finished = createTestInspection(inspectionId, status = InspectionStatus.FINISHED)
        val items = listOf(createTestItemResult(position = 0, result = ItemResultValue.COMPLIES))
        repository.emitDetail(inspectionId, Resource.Success(createTestDetail(items = items, inspection = finished)))
        val viewModel = createViewModel()

        viewModel.onResultClick(ItemResultValue.NOT_COMPLIES)
        viewModel.onNoteChange("Algo")
        viewModel.onAddPhotoClick()
        advanceTimeBy(ExecutionViewModel.NOTE_SAVE_DELAY_MILLIS + 1)

        assertTrue(currentContent(viewModel).isReadOnly)
        assertTrue(repository.savedItemResults.isEmpty())
        assertEquals("", currentContent(viewModel).noteDraft)
    }

    // --- Ayudas ---

    private fun givenItems(vararg items: ItemResult) {
        val inspection = createTestInspection(inspectionId, siteName = "Obra Av. Siempreviva 742")
        val detail = createTestDetail(items = items.toList(), inspection = inspection)
        repository.emitDetail(inspectionId, Resource.Success(detail))
    }

    private fun currentContent(viewModel: ExecutionViewModel): ExecutionContent {
        val state = viewModel.uiState.value
        if (state is UiState.Success) {
            return state.data
        }
        throw AssertionError("Se esperaba contenido, pero el estado es $state")
    }

    private fun FakePhotoStorage.contentUriOfCreatedPhoto(): String {
        val result = createResult
        if (result is Resource.Success) {
            return result.data.contentUri
        }
        throw AssertionError("El almacenamiento falso no tiene una foto preparada")
    }

    private fun createViewModel(startPosition: Int? = null): ExecutionViewModel {
        val arguments = mutableMapOf<String, Any?>(NavArguments.INSPECTION_ID to inspectionId)
        if (startPosition != null) {
            arguments[NavArguments.START_POSITION] = startPosition
        }
        val clock = createTestClock(now)

        return ExecutionViewModel(
            savedStateHandle = SavedStateHandle(arguments),
            getInspectionDetail = GetInspectionDetailUseCase(repository),
            getStartPosition = GetStartPositionUseCase(),
            saveItemResult = SaveItemResultUseCase(repository, clock),
            addEvidence = AddEvidenceUseCase(repository, clock),
            deleteEvidence = DeleteEvidenceUseCase(repository),
            preparePhotoCapture = PreparePhotoCaptureUseCase(photoStorage),
            discardPhoto = DiscardPhotoUseCase(photoStorage),
            observeConnectivity = ObserveConnectivityUseCase(networkMonitor),
        )
    }
}
