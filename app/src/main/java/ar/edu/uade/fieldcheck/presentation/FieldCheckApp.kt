package ar.edu.uade.fieldcheck.presentation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ar.edu.uade.fieldcheck.core.navigation.Screen
import ar.edu.uade.fieldcheck.domain.model.Inspection
import ar.edu.uade.fieldcheck.presentation.detail.DetailRoute
import ar.edu.uade.fieldcheck.presentation.execution.ExecutionRoute
import ar.edu.uade.fieldcheck.presentation.history.HistoryRoute
import ar.edu.uade.fieldcheck.presentation.newinspection.NewInspectionRoute
import ar.edu.uade.fieldcheck.presentation.review.ReviewRoute

/**
 * Raíz de la UI: define el grafo de navegación.
 *
 * Para sumar una pantalla nueva: agregar la ruta en [Screen] y un `composable<...>` acá.
 *
 * Las pantallas no conocen al `NavController`: avisan con callbacks ("se tocó atrás",
 * "se creó la inspección") y acá se decide a dónde ir. Así las reglas de la pila de
 * navegación (qué pantalla sale al avanzar) quedan todas juntas en un solo lugar.
 */
@Composable
fun FieldCheckApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.History,
        // Transiciones tipo "slide" para todas las pantallas (ver funciones al final del archivo).
        enterTransition = { slideInFromRight() },
        exitTransition = { slideOutToLeft() },
        popEnterTransition = { slideInFromLeft() },
        popExitTransition = { slideOutToRight() },
    ) {
        composable<Screen.History> {
            HistoryRoute(
                onNewInspectionClick = {
                    navController.navigate(Screen.NewInspection)
                },
                onInspectionClick = { inspection ->
                    openInspection(navController, inspection)
                },
            )
        }

        composable<Screen.NewInspection> {
            NewInspectionRoute(
                onBackClick = {
                    navController.popBackStack()
                },
                onInspectionStarted = { inspectionId ->
                    // P2 sale de la pila: "atrás" desde P3 vuelve directo a P1.
                    navController.navigate(Screen.Execution(inspectionId = inspectionId)) {
                        popUpTo<Screen.NewInspection> {
                            inclusive = true
                        }
                    }
                },
            )
        }

        composable<Screen.Execution> {
            ExecutionRoute(
                onBackClick = {
                    navController.popBackStack()
                },
                onGoToReview = { inspectionId ->
                    navController.navigate(Screen.Review(inspectionId = inspectionId))
                },
            )
        }

        composable<Screen.Review> {
            ReviewRoute(
                onBackClick = {
                    navController.popBackStack()
                },
                onEditItem = { inspectionId, position ->
                    // Se reemplaza la ejecución anterior (y la revisión) por una nueva en ese ítem,
                    // para que la pila quede Historial → Ejecución y "atrás" vuelva a P1.
                    navController.navigate(Screen.Execution(inspectionId = inspectionId, startPosition = position)) {
                        popUpTo<Screen.Execution> {
                            inclusive = true
                        }
                    }
                },
                onFinalized = { inspectionId ->
                    // Una inspección finalizada no se edita más: se sacan P3 y P4 de la pila
                    // y "atrás" desde el detalle vuelve al historial.
                    navController.navigate(Screen.Detail(inspectionId = inspectionId)) {
                        popUpTo<Screen.History> {
                            inclusive = false
                        }
                    }
                },
            )
        }

        composable<Screen.Detail> {
            DetailRoute(
                onBackClick = {
                    navController.popBackStack()
                },
            )
        }
    }
}

/** Desde el historial: una inspección en curso se retoma (P3); una finalizada se consulta (P5). */
private fun openInspection(navController: NavHostController, inspection: Inspection) {
    if (inspection.isFinished()) {
        navController.navigate(Screen.Detail(inspectionId = inspection.clientUuid))
    } else {
        navController.navigate(Screen.Execution(inspectionId = inspection.clientUuid))
    }
}

// ----- Transiciones entre pantallas -----
//
// Al avanzar, la pantalla nueva entra desde la derecha y la anterior se va hacia la izquierda.
// Al volver ("atrás"), es al revés: la pantalla anterior entra desde la izquierda y la actual
// sale hacia la derecha. Así el usuario "ve" en qué dirección se mueve dentro de la app.

/** Duración de cada transición, en milisegundos. */
private const val TRANSITION_DURATION_MILLIS = 300

/** Avanzar: la pantalla nueva entra desde la derecha. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInFromRight(): EnterTransition {
    return slideIntoContainer(
        towards = SlideDirection.Start,
        animationSpec = tween(durationMillis = TRANSITION_DURATION_MILLIS),
    )
}

/** Avanzar: la pantalla actual se va hacia la izquierda. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutToLeft(): ExitTransition {
    return slideOutOfContainer(
        towards = SlideDirection.Start,
        animationSpec = tween(durationMillis = TRANSITION_DURATION_MILLIS),
    )
}

/** Volver: la pantalla anterior entra desde la izquierda. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInFromLeft(): EnterTransition {
    return slideIntoContainer(
        towards = SlideDirection.End,
        animationSpec = tween(durationMillis = TRANSITION_DURATION_MILLIS),
    )
}

/** Volver: la pantalla actual sale hacia la derecha. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutToRight(): ExitTransition {
    return slideOutOfContainer(
        towards = SlideDirection.End,
        animationSpec = tween(durationMillis = TRANSITION_DURATION_MILLIS),
    )
}
