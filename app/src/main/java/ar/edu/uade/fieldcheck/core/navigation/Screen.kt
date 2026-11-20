package ar.edu.uade.fieldcheck.core.navigation

import kotlinx.serialization.Serializable

/**
 * Rutas de navegación de la app (rutas tipadas de Navigation Compose).
 *
 * Cada pantalla es un objeto o clase serializable. Las que necesitan parámetros
 * son `data class`: Navigation guarda sus propiedades en el `SavedStateHandle`
 * del ViewModel con el mismo nombre (ver [NavArguments]).
 */
sealed interface Screen {

    /** P1 · Historial: pantalla de inicio. */
    @Serializable
    data object History : Screen

    /** P2 · Nueva inspección: elegir plantilla e indicar el sitio. */
    @Serializable
    data object NewInspection : Screen

    /**
     * P3 · Ejecución de la inspección.
     *
     * @property startPosition ítem en el que abrir (posición desde 0). Con el valor por defecto
     *   [NO_START_POSITION] la pantalla abre en el primer ítem sin completar (regla R2).
     */
    @Serializable
    data class Execution(
        val inspectionId: String,
        val startPosition: Int = NO_START_POSITION,
    ) : Screen {

        companion object {
            const val NO_START_POSITION = -1
        }
    }

    /** P4 · Revisión y finalización. */
    @Serializable
    data class Review(val inspectionId: String) : Screen

    /** P5 · Detalle de una inspección finalizada (solo lectura). */
    @Serializable
    data class Detail(val inspectionId: String) : Screen
}

/**
 * Nombres de los argumentos tal como quedan en el `SavedStateHandle`.
 *
 * Los ViewModels leen así: `savedStateHandle.get<String>(NavArguments.INSPECTION_ID)`.
 * Leer el `SavedStateHandle` por nombre (y no con `toRoute()`) permite testear el ViewModel
 * con `SavedStateHandle(mapOf("inspectionId" to "abc"))`, sin Android.
 */
object NavArguments {
    const val INSPECTION_ID = "inspectionId"
    const val START_POSITION = "startPosition"
}
