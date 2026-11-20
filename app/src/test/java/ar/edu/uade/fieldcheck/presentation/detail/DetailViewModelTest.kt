package ar.edu.uade.fieldcheck.presentation.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.core.model.UiState
import ar.edu.uade.fieldcheck.core.navigation.NavArguments
import ar.edu.uade.fieldcheck.domain.model.InspectionDetail
import ar.edu.uade.fieldcheck.domain.model.InspectionStatus
import ar.edu.uade.fieldcheck.domain.model.ItemResult
import ar.edu.uade.fieldcheck.domain.model.ItemResultValue
import ar.edu.uade.fieldcheck.domain.model.ResultCounts
import ar.edu.uade.fieldcheck.domain.model.SyncStatus
import ar.edu.uade.fieldcheck.domain.usecase.BuildInspectionSummaryUseCase
import ar.edu.uade.fieldcheck.domain.usecase.GetInspectionDetailUseCase
import ar.edu.uade.fieldcheck.domain.usecase.RetrySyncUseCase
import ar.edu.uade.fieldcheck.fakes.FakeInspectionRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    private val inspectionId = "inspection-1"

    /** Reloj fijo en UTC: el resumen no depende de la zona horaria de la máquina que corre el test. */
    private val clock = createTestClock(nowMillis = 0L)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `shows the detail with only the not complies items in the section`() = runTest {
        val repository = FakeInspectionRepository()
        val photo = createTestEvidence(latitude = -34.6, longitude = -58.4)
        val compliesItem = createTestItemResult(position = 0, result = ItemResultValue.COMPLIES)
        val firstNotComplies = createTestItemResult(position = 1, result = ItemResultValue.NOT_COMPLIES, note = "Sin tapa")
        val notApplicableItem = createTestItemResult(position = 2, result = ItemResultValue.NOT_APPLICABLE)
        val secondNotComplies = createTestItemResult(
            position = 3,
            result = ItemResultValue.NOT_COMPLIES,
            evidence = listOf(photo),
        )
        val detail = createFinishedDetail(
            items = listOf(compliesItem, firstNotComplies, notApplicableItem, secondNotComplies),
        )
        repository.emitDetail(inspectionId, Resource.Success(detail))

        val viewModel = createViewModel(repository)

        val content = currentContent(viewModel)
        assertEquals(detail.inspection, content.inspection)
        assertEquals(ResultCounts(complies = 1, notComplies = 2, notApplicable = 1, pending = 0), content.counts)
        assertEquals(listOf(firstNotComplies, secondNotComplies), content.notCompliesItems)
    }

    @Test
    fun `shows the pending sync state without error`() = runTest {
        val repository = FakeInspectionRepository()
        repository.emitDetail(inspectionId, Resource.Success(createFinishedDetail(syncStatus = SyncStatus.PENDING)))

        val viewModel = createViewModel(repository)

        val content = currentContent(viewModel)
        assertEquals(SyncStatus.PENDING, content.inspection.syncStatus)
        assertNull(content.lastSyncError)
    }

    @Test
    fun `shows the synced state`() = runTest {
        val repository = FakeInspectionRepository()
        repository.emitDetail(inspectionId, Resource.Success(createFinishedDetail(syncStatus = SyncStatus.SYNCED)))

        val viewModel = createViewModel(repository)

        assertEquals(SyncStatus.SYNCED, currentContent(viewModel).inspection.syncStatus)
    }

    @Test
    fun `shows the failed state with the reason in plain language`() = runTest {
        val repository = FakeInspectionRepository()
        val detail = createFinishedDetail(
            syncStatus = SyncStatus.FAILED,
            lastSyncError = "El servidor no respondió después de varios intentos.",
        )
        repository.emitDetail(inspectionId, Resource.Success(detail))

        val viewModel = createViewModel(repository)

        val content = currentContent(viewModel)
        assertEquals(SyncStatus.FAILED, content.inspection.syncStatus)
        assertEquals("El servidor no respondió después de varios intentos.", content.lastSyncError)
    }

    @Test
    fun `retry calls the use case once and the card goes back to pending`() = runTest {
        val repository = FakeInspectionRepository()
        repository.emitDetail(inspectionId, Resource.Success(createFinishedDetail(syncStatus = SyncStatus.FAILED)))
        val viewModel = createViewModel(repository)

        viewModel.onRetrySyncClick()
        // Un segundo toque ya encuentra la inspección en PENDING: no se vuelve a encolar.
        viewModel.onRetrySyncClick()

        assertEquals(listOf(inspectionId), repository.retriedInspectionIds)
        assertEquals(SyncStatus.PENDING, currentContent(viewModel).inspection.syncStatus)
    }

    @Test
    fun `retry does nothing when the sync did not fail`() = runTest {
        val repository = FakeInspectionRepository()
        repository.emitDetail(inspectionId, Resource.Success(createFinishedDetail(syncStatus = SyncStatus.PENDING)))
        val viewModel = createViewModel(repository)

        viewModel.onRetrySyncClick()

        assertTrue(repository.retriedInspectionIds.isEmpty())
    }

    @Test
    fun `retry emits an error message when the database fails`() = runTest {
        val repository = FakeInspectionRepository()
        repository.emitDetail(inspectionId, Resource.Success(createFinishedDetail(syncStatus = SyncStatus.FAILED)))
        repository.writeResult = Resource.Error("No pudimos guardar el cambio.")
        val viewModel = createViewModel(repository)

        viewModel.effects.test {
            viewModel.onRetrySyncClick()

            assertEquals(DetailEffect.ShowMessage("No pudimos guardar el cambio."), awaitItem())
        }
    }

    @Test
    fun `share emits a one shot effect with the summary text`() = runTest {
        val repository = FakeInspectionRepository()
        val detail = createFinishedDetail(
            items = listOf(createTestItemResult(position = 0, result = ItemResultValue.NOT_COMPLIES, note = "Sin tapa")),
        )
        repository.emitDetail(inspectionId, Resource.Success(detail))
        val viewModel = createViewModel(repository)
        val expectedSummary = BuildInspectionSummaryUseCase(clock).invoke(detail)

        viewModel.effects.test {
            viewModel.onShareClick()

            assertEquals(DetailEffect.ShareSummary(expectedSummary), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `shows the error when the inspection cannot be read`() = runTest {
        val repository = FakeInspectionRepository()
        repository.emitDetail(inspectionId, Resource.Error("No pudimos leer la inspección."))

        val viewModel = createViewModel(repository)

        assertEquals(UiState.Error("No pudimos leer la inspección."), viewModel.uiState.value)
    }

    @Test
    fun `shows not found when the navigation argument is missing`() = runTest {
        val viewModel = createViewModel(FakeInspectionRepository(), SavedStateHandle())

        assertEquals(UiState.Error("No encontramos esta inspección."), viewModel.uiState.value)
    }

    @Test
    fun `retry loading reads the inspection again after an error`() = runTest {
        val repository = FakeInspectionRepository()
        repository.emitDetail(inspectionId, Resource.Error("Falla"))
        val viewModel = createViewModel(repository)

        val detail = createFinishedDetail()
        repository.emitDetail(inspectionId, Resource.Success(detail))
        viewModel.loadDetail()

        assertEquals(detail.inspection, currentContent(viewModel).inspection)
    }

    private fun createFinishedDetail(
        items: List<ItemResult> = listOf(
            createTestItemResult(position = 0, result = ItemResultValue.COMPLIES),
        ),
        syncStatus: SyncStatus = SyncStatus.PENDING,
        lastSyncError: String? = null,
    ): InspectionDetail {
        val inspection = createTestInspection(
            clientUuid = inspectionId,
            status = InspectionStatus.FINISHED,
            startedAt = 1_000L,
            finishedAt = 2_000L,
            syncStatus = syncStatus,
        )
        return createTestDetail(items = items, inspection = inspection, lastSyncError = lastSyncError)
    }

    /** Falla con un mensaje claro si la pantalla no está mostrando contenido. */
    private fun currentContent(viewModel: DetailViewModel): DetailContent {
        val state = viewModel.uiState.value
        assertTrue("Se esperaba contenido pero el estado es $state", state is UiState.Success)
        return (state as UiState.Success).data
    }

    private fun createViewModel(
        repository: FakeInspectionRepository,
        savedStateHandle: SavedStateHandle = SavedStateHandle(mapOf(NavArguments.INSPECTION_ID to inspectionId)),
    ): DetailViewModel {
        return DetailViewModel(
            savedStateHandle = savedStateHandle,
            getInspectionDetail = GetInspectionDetailUseCase(repository),
            retrySync = RetrySyncUseCase(repository),
            buildInspectionSummary = BuildInspectionSummaryUseCase(clock),
        )
    }
}
