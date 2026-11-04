package ar.edu.uade.fieldcheck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ar.edu.uade.fieldcheck.core.theme.FieldCheckTheme
import ar.edu.uade.fieldcheck.presentation.FieldCheckApp
import dagger.hilt.android.AndroidEntryPoint

/**
 * Única Activity de la app. Toda la UI está en Jetpack Compose.
 *
 * `@AndroidEntryPoint` permite que Hilt entregue los ViewModels a las pantallas.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FieldCheckTheme {
                FieldCheckApp()
            }
        }
    }
}
