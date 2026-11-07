package ar.edu.uade.fieldcheck

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.fieldcheck.core.theme.FieldCheckTheme
import ar.edu.uade.fieldcheck.presentation.FieldCheckApp
import ar.edu.uade.fieldcheck.presentation.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Única Activity de la app. Toda la UI está en Jetpack Compose.
 *
 * `@AndroidEntryPoint` permite que Hilt entregue los ViewModels a las pantallas.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** El tema es de toda la app, por eso su ViewModel vive en la Activity. */
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val isDarkMode by themeViewModel.isDarkMode.collectAsStateWithLifecycle()

            // Los íconos de la barra de estado siguen el tema de la app y no el del sistema:
            // si no, con el teléfono en oscuro y la app en claro quedarían blancos sobre blanco.
            DisposableEffect(isDarkMode) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isDarkMode },
                    navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim) { isDarkMode },
                )
                onDispose {}
            }

            FieldCheckTheme(darkTheme = isDarkMode) {
                FieldCheckApp(
                    isDarkMode = isDarkMode,
                    onDarkModeChange = themeViewModel::onDarkModeChange,
                )
            }
        }
    }

    private companion object {
        // Los mismos velos que usa enableEdgeToEdge() por defecto en la barra de navegación.
        val LightScrim = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
        val DarkScrim = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}
