package ar.edu.uade.fieldcheck.presentation.detail

import android.content.Context
import android.content.Intent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.fieldcheck.R

/**
 * Contenedor de la pantalla P5 · Detalle de inspección (patrón container / presentational).
 *
 * Se conecta con el [DetailViewModel]: lee el estado y le pasa todo a [DetailScreen],
 * que solo dibuja. Además escucha los eventos de una sola vez ([DetailEvent]):
 * abrir la hoja de compartir necesita un `Context`, y eso no puede vivir en el ViewModel.
 *
 * Mantiene la firma que usa `FieldCheckApp` para no tocar la navegación.
 */
@Composable
fun DetailRoute(
    onBackClick: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Se lanza una sola vez por ViewModel: cada evento se consume apenas llega.
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DetailEvent.ShareSummary -> {
                    openShareSheet(context = context, summary = event.text)
                }

                is DetailEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    DetailScreen(
        uiState = uiState,
        isOnline = isOnline,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onRetryLoadClick = viewModel::loadDetail,
        onRetrySyncClick = viewModel::onRetrySyncClick,
        onShareClick = viewModel::onShareClick,
    )
}

/**
 * Abre la hoja de compartir de Android con el resumen en texto plano (regla R7).
 *
 * Usamos `createChooser` para que el usuario elija la app cada vez (WhatsApp, mail...)
 * en lugar de que quede una app fija por defecto. Funciona sin conexión: solo pasa texto.
 */
private fun openShareSheet(context: Context, summary: String) {
    val sendIntent = Intent(Intent.ACTION_SEND)
    sendIntent.type = "text/plain"
    sendIntent.putExtra(Intent.EXTRA_TEXT, summary)

    val chooserTitle = context.getString(R.string.detail_share_chooser_title)
    val chooserIntent = Intent.createChooser(sendIntent, chooserTitle)
    context.startActivity(chooserIntent)
}
