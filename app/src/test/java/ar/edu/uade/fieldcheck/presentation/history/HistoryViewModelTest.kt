package ar.edu.uade.fieldcheck.presentation.history

import app.cash.turbine.test
import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.core.model.UiState
import ar.edu.uade.fieldcheck.domain.model.InspectionHistory
import ar.edu.uade.fieldcheck.domain.model.InspectionStatus
import ar.edu.uade.fieldcheck.domain.usecase.GetInspectionHistoryUseCase
import ar.edu.uade.fieldcheck.fakes.FakeInspectionRepository
import ar.edu.uade.fieldcheck.fakes.createTestInspection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    /**
     * `viewModelScope` usa el hilo principal de Android, que no existe en un test de JVM.
     * Lo reemplazamos por un dispatcher de test antes de cada prueba.
     */
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `shows the history when the repository has inspections`() = runTest {
        val inspection = createTestInspection("abc", status = InspectionStatus.IN_PROGRESS)
        val repository = FakeInspectionRepository(Resource.Success(listOf(inspection)))

        val viewModel = createViewModel(repository)

        val expected = UiState.Success(InspectionHistory(inProgress = listOf(inspection), finished = emptyList()))
        assertEquals(expected, viewModel.uiState.value)
    }

    @Test
    fun `shows the error message when the repository fails`() = runTest {
        val repository = FakeInspectionRepository(Resource.Error("No pudimos leer las inspecciones guardadas."))

        val viewModel = createViewModel(repository)

        assertEquals(UiState.Error("No pudimos leer las inspecciones guardadas."), viewModel.uiState.value)
    }

    @Test
    fun `updates the screen by itself when the saved data changes`() = runTest {
        val repository = FakeInspectionRepository(Resource.Success(emptyList()))
        val viewModel = createViewModel(repository)

        val newInspection = createTestInspection("new")
        repository.emit(Resource.Success(listOf(newInspection)))

        val expected = UiState.Success(InspectionHistory(inProgress = listOf(newInspection), finished = emptyList()))
        assertEquals(expected, viewModel.uiState.value)
    }

    @Test
    fun `retry reads the history again after an error`() = runTest {
        val repository = FakeInspectionRepository(Resource.Error("Falla"))
        val viewModel = createViewModel(repository)

        repository.emit(Resource.Success(emptyList()))
        viewModel.loadHistory()

        val expected = UiState.Success(InspectionHistory(inProgress = emptyList(), finished = emptyList()))
        assertEquals(expected, viewModel.uiState.value)
    }

    @Test
    fun `tapping new inspection emits a one-shot snackbar`() = runTest {
        val viewModel = createViewModel(FakeInspectionRepository())

        viewModel.effects.test {
            viewModel.onNewInspectionClick()

            val effect = awaitItem()
            assertTrue(effect is HistoryUiEffect.ShowSnackbar)
        }
    }

    @Test
    fun `tapping a finished inspection mentions the detail screen`() = runTest {
        val viewModel = createViewModel(FakeInspectionRepository())
        val finished = createTestInspection("done", status = InspectionStatus.FINISHED, finishedAt = 2_000L)

        viewModel.effects.test {
            viewModel.onInspectionClick(finished)

            val effect = awaitItem() as HistoryUiEffect.ShowSnackbar
            assertTrue(effect.message.contains("el detalle"))
        }
    }

    @Test
    fun `tapping an in progress inspection mentions the execution screen`() = runTest {
        val viewModel = createViewModel(FakeInspectionRepository())
        val inProgress = createTestInspection("doing", status = InspectionStatus.IN_PROGRESS)

        viewModel.effects.test {
            viewModel.onInspectionClick(inProgress)

            val effect = awaitItem() as HistoryUiEffect.ShowSnackbar
            assertTrue(effect.message.contains("la ejecución"))
        }
    }

    private fun createViewModel(repository: FakeInspectionRepository): HistoryViewModel {
        val useCase = GetInspectionHistoryUseCase(repository)
        return HistoryViewModel(useCase)
    }
}
