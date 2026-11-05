package ar.edu.uade.fieldcheck.presentation

import androidx.compose.runtime.Composable
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
