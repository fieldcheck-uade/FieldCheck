package ar.edu.uade.fieldcheck.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.fieldcheck.domain.usecase.ObserveDarkModeUseCase
import ar.edu.uade.fieldcheck.domain.usecase.SetDarkModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel del tema de toda la app (claro u oscuro).
 *
 * Vive en la [ar.edu.uade.fieldcheck.MainActivity] y no en una pantalla, porque el tema
 * envuelve a todas las pantallas y tiene que sobrevivir a la navegación.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    observeDarkMode: ObserveDarkModeUseCase,
    private val setDarkMode: SetDarkModeUseCase,
) : ViewModel() {

    /**
     * `true` si la app se tiene que ver en modo oscuro.
     *
     * Arranca en `false` (modo claro) mientras se lee la preferencia guardada.
     * `Eagerly` empieza a leerla apenas se crea el ViewModel, antes de dibujar la primera pantalla.
     */
    val isDarkMode: StateFlow<Boolean> = observeDarkMode().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false,
    )

    /** Lo llama el botón de la barra superior. */
    fun onDarkModeChange(enabled: Boolean) {
        viewModelScope.launch {
            setDarkMode(enabled)
        }
    }
}
