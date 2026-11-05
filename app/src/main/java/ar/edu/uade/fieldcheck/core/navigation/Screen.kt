package ar.edu.uade.fieldcheck.core.navigation

import kotlinx.serialization.Serializable

/**
 * Rutas de navegación de la app (rutas tipadas de Navigation Compose).
 *
 * Cada pantalla es un objeto o clase serializable. Las que necesitan parámetros
 * (por ejemplo el id de la inspección en P3 y P5) se agregan como `data class`.
 */
sealed interface Screen {

    /** P1 · Historial: pantalla de inicio. */
    @Serializable
    data object History : Screen
}
