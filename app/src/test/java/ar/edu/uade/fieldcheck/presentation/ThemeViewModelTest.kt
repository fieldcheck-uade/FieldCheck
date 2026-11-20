package ar.edu.uade.fieldcheck.presentation

import ar.edu.uade.fieldcheck.domain.usecase.ObserveDarkModeUseCase
import ar.edu.uade.fieldcheck.domain.usecase.SetDarkModeUseCase
import ar.edu.uade.fieldcheck.fakes.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts in light mode by default`() = runTest {
        val viewModel = createViewModel(FakeSettingsRepository())

        assertFalse(viewModel.isDarkMode.value)
    }

    @Test
    fun `restores dark mode if the user chose it before`() = runTest {
        val viewModel = createViewModel(FakeSettingsRepository(initialDarkMode = true))

        assertTrue(viewModel.isDarkMode.value)
    }

    @Test
    fun `tapping the button switches to dark mode and back`() = runTest {
        val viewModel = createViewModel(FakeSettingsRepository())

        viewModel.onDarkModeChange(true)
        assertTrue(viewModel.isDarkMode.value)

        viewModel.onDarkModeChange(false)
        assertFalse(viewModel.isDarkMode.value)
    }

    private fun createViewModel(repository: FakeSettingsRepository): ThemeViewModel {
        return ThemeViewModel(
            observeDarkMode = ObserveDarkModeUseCase(repository),
            setDarkMode = SetDarkModeUseCase(repository),
        )
    }
}
