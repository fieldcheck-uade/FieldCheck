package ar.edu.uade.fieldcheck.presentation.newinspection

import app.cash.turbine.test
import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.core.model.UiState
import ar.edu.uade.fieldcheck.domain.usecase.CreateInspectionUseCase
import ar.edu.uade.fieldcheck.domain.usecase.GetTemplatesUseCase
import ar.edu.uade.fieldcheck.domain.usecase.ObserveConnectivityUseCase
import ar.edu.uade.fieldcheck.fakes.FakeInspectionRepository
import ar.edu.uade.fieldcheck.fakes.FakeNetworkMonitor
import ar.edu.uade.fieldcheck.fakes.FakeTemplateRepository
import ar.edu.uade.fieldcheck.fakes.createTestClock
import ar.edu.uade.fieldcheck.fakes.createTestTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
class NewInspectionViewModelTest {

    private val now = 1_790_000_000_000L

    private lateinit var templateRepository: FakeTemplateRepository
    private lateinit var inspectionRepository: FakeInspectionRepository
    private lateinit var networkMonitor: FakeNetworkMonitor

    /** `viewModelScope` usa el hilo principal de Android, que no existe en la JVM de los tests. */
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        templateRepository = FakeTemplateRepository()
        inspectionRepository = FakeInspectionRepository()
        networkMonitor = FakeNetworkMonitor(initiallyOnline = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `shows the templates sorted by name`() = runTest {
        val sala = createTestTemplate(id = "2", name = "Sala técnica")
        val control = createTestTemplate(id = "1", name = "Control semanal de obra")
        templateRepository.emit(Resource.Success(listOf(sala, control)))

        val viewModel = createViewModel()

        assertEquals(UiState.Success(listOf(control, sala)), viewModel.templatesState.value)
    }

    @Test
    fun `shows an empty list when there are no templates`() = runTest {
        templateRepository.emit(Resource.Success(emptyList()))

        val viewModel = createViewModel()

        assertEquals(UiState.Success(emptyList<Any>()), viewModel.templatesState.value)
    }

    @Test
    fun `shows the error and retry reads the templates again`() = runTest {
        templateRepository.emit(Resource.Error("No pudimos descargar las plantillas."))
        val viewModel = createViewModel()
        assertEquals(UiState.Error("No pudimos descargar las plantillas."), viewModel.templatesState.value)

        val template = createTestTemplate(id = "1")
        templateRepository.emit(Resource.Success(listOf(template)))
        viewModel.loadTemplates()

        assertEquals(UiState.Success(listOf(template)), viewModel.templatesState.value)
    }

    @Test
    fun `reports when the phone goes offline`() = runTest {
        val viewModel = createViewModel()
        assertTrue(viewModel.isOnline.value)

        networkMonitor.setOnline(false)

        assertFalse(viewModel.isOnline.value)
    }

    @Test
    fun `selecting a template opens the sheet with an empty site`() = runTest {
        val template = createTestTemplate(id = "1")
        val viewModel = createViewModel()
        assertNull(viewModel.startSheetState.value)

        viewModel.onTemplateSelected(template)

        val expected = StartSheetState(
            template = template,
            siteName = "",
            siteError = null,
            creationError = null,
            isCreating = false,
        )
        assertEquals(expected, viewModel.startSheetState.value)
    }

    @Test
    fun `dismissing the sheet closes it`() = runTest {
        val viewModel = createViewModel()
        viewModel.onTemplateSelected(createTestTemplate(id = "1"))

        viewModel.onSheetDismissed()

        assertNull(viewModel.startSheetState.value)
    }

    @Test
    fun `blank site shows Obligatorio and does not create the inspection`() = runTest {
        val viewModel = createViewModel()
        viewModel.onTemplateSelected(createTestTemplate(id = "1"))
        viewModel.onSiteNameChange("   ")

        viewModel.onStartInspectionClick()

        assertEquals("Obligatorio", viewModel.startSheetState.value?.siteError)
        assertTrue(inspectionRepository.createdInspections.isEmpty())
    }

    @Test
    fun `typing the site clears the previous site error`() = runTest {
        val viewModel = createViewModel()
        viewModel.onTemplateSelected(createTestTemplate(id = "1"))
        viewModel.onStartInspectionClick()

        viewModel.onSiteNameChange("Obra")

        assertNull(viewModel.startSheetState.value?.siteError)
        assertEquals("Obra", viewModel.startSheetState.value?.siteName)
    }

    @Test
    fun `valid site creates the inspection and emits the started event with the id`() = runTest {
        inspectionRepository.createInspectionResult = Resource.Success("inspection-42")
        val template = createTestTemplate(id = "1")
        val viewModel = createViewModel()
        viewModel.onTemplateSelected(template)
        viewModel.onSiteNameChange("  Obra Av. Siempreviva 742  ")

        viewModel.events.test {
            viewModel.onStartInspectionClick()

            assertEquals(NewInspectionEvent.InspectionStarted("inspection-42"), awaitItem())
        }

        assertEquals(1, inspectionRepository.createdInspections.size)
        val created = inspectionRepository.createdInspections[0]
        assertEquals(template, created.template)
        assertEquals("Obra Av. Siempreviva 742", created.siteName)
        assertEquals(now, created.now)
        assertNull(viewModel.startSheetState.value)
    }

    @Test
    fun `repository failure keeps the sheet open with the error`() = runTest {
        inspectionRepository.createInspectionResult = Resource.Error("No pudimos crear la inspección.")
        val viewModel = createViewModel()
        viewModel.onTemplateSelected(createTestTemplate(id = "1"))
        viewModel.onSiteNameChange("Obra")

        viewModel.onStartInspectionClick()

        val sheet = viewModel.startSheetState.value
        assertEquals("No pudimos crear la inspección.", sheet?.creationError)
        assertNull(sheet?.siteError)
        assertEquals(false, sheet?.isCreating)
    }

    @Test
    fun `double tap on start creates the inspection only once`() = runTest {
        // Dispatcher que no ejecuta nada hasta que se lo pedimos: así el segundo toque llega
        // mientras la primera creación todavía está pendiente, como pasa en el teléfono.
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()
        viewModel.onTemplateSelected(createTestTemplate(id = "1"))
        viewModel.onSiteNameChange("Obra")

        viewModel.onStartInspectionClick()
        assertEquals(true, viewModel.startSheetState.value?.isCreating)
        viewModel.onStartInspectionClick()
        advanceUntilIdle()

        assertEquals(1, inspectionRepository.createdInspections.size)
    }

    private fun createViewModel(): NewInspectionViewModel {
        return NewInspectionViewModel(
            getTemplates = GetTemplatesUseCase(templateRepository),
            createInspection = CreateInspectionUseCase(inspectionRepository, createTestClock(now)),
            observeConnectivity = ObserveConnectivityUseCase(networkMonitor),
        )
    }
}
