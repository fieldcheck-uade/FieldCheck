package ar.edu.uade.fieldcheck.presentation.review

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.core.model.UiState
import ar.edu.uade.fieldcheck.core.navigation.NavArguments
import ar.edu.uade.fieldcheck.domain.model.ItemResult
import ar.edu.uade.fieldcheck.domain.model.ItemResultValue
import ar.edu.uade.fieldcheck.domain.model.ResultCounts
import ar.edu.uade.fieldcheck.domain.usecase.CheckFinalizationUseCase
import ar.edu.uade.fieldcheck.domain.usecase.FinalizeInspectionUseCase
import ar.edu.uade.fieldcheck.domain.usecase.GetInspectionDetailUseCase
import ar.edu.uade.fieldcheck.domain.usecase.ObserveConnectivityUseCase
import ar.edu.uade.fieldcheck.fakes.FakeInspectionRepository
import ar.edu.uade.fieldcheck.fakes.FakeNetworkMonitor
import ar.edu.uade.fieldcheck.fakes.createTestClock
import ar.edu.uade.fieldcheck.fakes.createTestDetail
import ar.edu.uade.fieldcheck.fakes.createTestEvidence
import ar.edu.uade.fieldcheck.fakes.createTestInspection
import ar.edu.uade.fieldcheck.fakes.createTestItemResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModelTest {

    private val inspectionId = "inspection-1"
    private val nowMillis = 50_000L

    /** `viewModelScope` usa el hilo principal de Android: en la JVM lo reemplazamos por uno de test. */
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `shows the counts and every item in order`() = runTest {
        val items = completeItems()
        val repository = createRepositoryWith(items)

        val viewModel = createViewModel(repository)

        val content = successContent(viewModel)
        val expectedCounts = ResultCounts(complies = 1, notComplies = 1, notApplicable = 1, pending = 0)
        assertEquals(expectedCounts, content.counts)
        assertEquals(listOf(0, 1, 2), content.items.map { reviewItem -> reviewItem.item.position })
        assertTrue(content.missingItems.isEmpty())
        assertEquals(null, content.items[1].problem)
    }

    @Test
    fun `flags the items that block finalization with their reason`() = runTest {
        val items = listOf(
            createTestItemResult(position = 0, result = ItemResultValue.COMPLIES),
            createTestItemResult(position = 1, result = ItemResultValue.NOT_COMPLIES),
            createTestItemResult(position = 2, result = null),
        )
        val repository = createRepositoryWith(items)

        val viewModel = createViewModel(repository)

        val content = successContent(viewModel)
        assertEquals(null, content.items[0].problem)
        assertEquals(ReviewProblem.NOT_COMPLIES_WITHOUT_EVIDENCE, content.items[1].problem)
        assertEquals(ReviewProblem.WITHOUT_RESULT, content.items[2].problem)
        assertEquals(listOf(1, 2), content.missingItems.map { reviewItem -> reviewItem.item.position })
        assertEquals("Faltan 2 ítems para poder finalizar.", content.missingMessage)
    }

    @Test
    fun `finalize with missing items shows them and does not call the repository`() = runTest {
        val items = listOf(
            createTestItemResult(position = 0, result = ItemResultValue.COMPLIES),
            createTestItemResult(position = 1, result = null),
        )
        val repository = createRepositoryWith(items)
        val viewModel = createViewModel(repository)

        viewModel.onFinalizeClick()

        val finalization = viewModel.finalizationState.value
        assertTrue(finalization.isShowingMissingItems)
        assertFalse(finalization.isConfirmDialogVisible)
        assertEquals("Falta 1 ítem para poder finalizar.", successContent(viewModel).missingMessage)
        assertTrue(repository.finalizedInspections.isEmpty())
    }

    @Test
    fun `finalize with everything complete asks for confirmation first`() = runTest {
        val repository = createRepositoryWith(completeItems())
        val viewModel = createViewModel(repository)

        viewModel.onFinalizeClick()

        val finalization = viewModel.finalizationState.value
        assertTrue(finalization.isConfirmDialogVisible)
        assertFalse(finalization.isShowingMissingItems)
        assertTrue(repository.finalizedInspections.isEmpty())
    }

    @Test
    fun `confirming finalizes once and emits the finalized event`() = runTest {
        val repository = createRepositoryWith(completeItems())
        val viewModel = createViewModel(repository)

        viewModel.events.test {
            viewModel.onFinalizeClick()
            viewModel.onConfirmFinalize()
            // Un segundo toque (por ejemplo, doble toque rápido) no debe finalizar de nuevo.
            viewModel.onConfirmFinalize()

            assertEquals(ReviewEvent.Finalized(inspectionId), awaitItem())
            expectNoEvents()
        }

        assertEquals(1, repository.finalizedInspections.size)
        assertEquals(inspectionId, repository.finalizedInspections[0].inspectionId)
        assertEquals(nowMillis, repository.finalizedInspections[0].now)
        assertFalse(viewModel.finalizationState.value.isConfirmDialogVisible)
    }

    @Test
    fun `confirming without opening the dialog does nothing`() = runTest {
        val repository = createRepositoryWith(completeItems())
        val viewModel = createViewModel(repository)

        viewModel.onConfirmFinalize()

        assertTrue(repository.finalizedInspections.isEmpty())
    }

    @Test
    fun `cancelling the confirmation closes the dialog and does not finalize`() = runTest {
        val repository = createRepositoryWith(completeItems())
        val viewModel = createViewModel(repository)

        viewModel.events.test {
            viewModel.onFinalizeClick()
            viewModel.onDismissConfirmDialog()

            expectNoEvents()
        }

        assertFalse(viewModel.finalizationState.value.isConfirmDialogVisible)
        assertTrue(repository.finalizedInspections.isEmpty())
    }

    @Test
    fun `a repository error while finalizing shows its message`() = runTest {
        val repository = createRepositoryWith(completeItems())
        repository.writeResult = Resource.Error("No pudimos finalizar la inspección.")
        val viewModel = createViewModel(repository)

        viewModel.events.test {
            viewModel.onFinalizeClick()
            viewModel.onConfirmFinalize()

            assertEquals(ReviewEvent.ShowMessage("No pudimos finalizar la inspección."), awaitItem())
            expectNoEvents()
        }

        assertFalse(viewModel.finalizationState.value.isFinalizing)
    }

    @Test
    fun `shows the error and retry reads the inspection again`() = runTest {
        val repository = FakeInspectionRepository()
        repository.emitDetail(inspectionId, Resource.Error("No pudimos leer la inspección."))
        val viewModel = createViewModel(repository)

        assertEquals(UiState.Error("No pudimos leer la inspección."), viewModel.uiState.value)

        repository.emitDetail(inspectionId, Resource.Success(detailWith(completeItems())))
        viewModel.loadInspection()

        assertTrue(viewModel.uiState.value is UiState.Success)
    }

    @Test
    fun `shows an error when the navigation argument is missing`() = runTest {
        val repository = createRepositoryWith(completeItems())

        val viewModel = createViewModel(repository, SavedStateHandle())

        assertEquals(UiState.Error("No encontramos esta inspección."), viewModel.uiState.value)
    }

    @Test
    fun `reports when the phone is offline`() = runTest {
        val repository = createRepositoryWith(completeItems())
        val networkMonitor = FakeNetworkMonitor(initiallyOnline = false)

        val viewModel = createViewModel(repository, networkMonitor = networkMonitor)

        assertFalse(viewModel.isOnline.value)
    }

    // --- Ayudas ---

    /** Tres ítems listos para finalizar: el "No cumple" tiene una foto como evidencia. */
    private fun completeItems(): List<ItemResult> {
        return listOf(
            createTestItemResult(position = 0, result = ItemResultValue.COMPLIES),
            createTestItemResult(
                position = 1,
                result = ItemResultValue.NOT_COMPLIES,
                evidence = listOf(createTestEvidence()),
            ),
            createTestItemResult(position = 2, result = ItemResultValue.NOT_APPLICABLE),
        )
    }

    private fun detailWith(items: List<ItemResult>) = createTestDetail(
        items = items,
        inspection = createTestInspection(inspectionId),
    )

    private fun createRepositoryWith(items: List<ItemResult>): FakeInspectionRepository {
        val repository = FakeInspectionRepository()
        repository.emitDetail(inspectionId, Resource.Success(detailWith(items)))
        return repository
    }

    private fun successContent(viewModel: ReviewViewModel): ReviewContent {
        val state = viewModel.uiState.value
        assertTrue("Se esperaba contenido y llegó $state", state is UiState.Success)
        return (state as UiState.Success).data
    }

    private fun createViewModel(
        repository: FakeInspectionRepository,
        savedStateHandle: SavedStateHandle = SavedStateHandle(mapOf(NavArguments.INSPECTION_ID to inspectionId)),
        networkMonitor: FakeNetworkMonitor = FakeNetworkMonitor(),
    ): ReviewViewModel {
        val checkFinalization = CheckFinalizationUseCase()
        return ReviewViewModel(
            savedStateHandle = savedStateHandle,
            getInspectionDetail = GetInspectionDetailUseCase(repository),
            checkFinalization = checkFinalization,
            finalizeInspection = FinalizeInspectionUseCase(repository, checkFinalization, createTestClock(nowMillis)),
            observeConnectivity = ObserveConnectivityUseCase(networkMonitor),
        )
    }
}
