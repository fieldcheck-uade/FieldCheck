package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.fakes.FakeSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DarkModeUseCasesTest {

    @Test
    fun `dark mode is off by default`() = runTest {
        val observeDarkMode = ObserveDarkModeUseCase(FakeSettingsRepository())

        assertFalse(observeDarkMode().first())
    }

    @Test
    fun `enabling dark mode is saved`() = runTest {
        val repository = FakeSettingsRepository()
        val setDarkMode = SetDarkModeUseCase(repository)
        val observeDarkMode = ObserveDarkModeUseCase(repository)

        setDarkMode(true)

        assertTrue(observeDarkMode().first())
    }

    @Test
    fun `disabling dark mode goes back to light mode`() = runTest {
        val repository = FakeSettingsRepository(initialDarkMode = true)
        val setDarkMode = SetDarkModeUseCase(repository)
        val observeDarkMode = ObserveDarkModeUseCase(repository)

        setDarkMode(false)

        assertFalse(observeDarkMode().first())
    }
}
