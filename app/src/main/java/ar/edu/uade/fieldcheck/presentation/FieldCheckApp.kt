package ar.edu.uade.fieldcheck.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ar.edu.uade.fieldcheck.core.navigation.Screen
import ar.edu.uade.fieldcheck.presentation.history.HistoryRoute

/**
 * Raíz de la UI: define el grafo de navegación.
 *
 * Para sumar una pantalla nueva: agregar la ruta en [Screen] y un `composable<...>` acá.
 */
@Composable
fun FieldCheckApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.History,
    ) {
        composable<Screen.History> {
            HistoryRoute()
        }
    }
}
