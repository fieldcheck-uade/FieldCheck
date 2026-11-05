package ar.edu.uade.fieldcheck.presentation.execution

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.fieldcheck.R
import kotlinx.coroutines.launch

/**
 * Contenedor de la pantalla P3 · Ejecución de la inspección (patrón container / presentational).
 *
 * Se conecta con el [ExecutionViewModel] y resuelve lo que necesita Android:
 * abrir la cámara del sistema, mostrar Snackbars y navegar. [ExecutionScreen] solo dibuja.
 *
 * @param onBackClick vuelve a P1. No hace falta confirmar: todo ya quedó guardado.
 * @param onGoToReview abre P4 · Revisión y finalización.
 */
@Composable
fun ExecutionRoute(
    onBackClick: () -> Unit,
    onGoToReview: (inspectionId: String) -> Unit,
) {
    val viewModel: ExecutionViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // La cámara del sistema escribe la foto en el archivo que preparó el ViewModel.
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { photoTaken ->
        viewModel.onPhotoResult(success = photoTaken)
    }

    // Si la app pasa a segundo plano (o se sale de la pantalla), la nota pendiente se guarda ya.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.onLeaveScreen()
    }

    ExecutionEventsEffect(
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
        cameraLauncher = cameraLauncher,
        onGoToReview = onGoToReview,
    )

    val actions = ExecutionActions(
        onBackClick = {
            viewModel.onLeaveScreen()
            onBackClick()
        },
        onSeeAllClick = viewModel::onReviewClick,
        onRetryClick = viewModel::loadInspection,
        onResultClick = viewModel::onResultClick,
        onNoteChange = viewModel::onNoteChange,
        onAddPhotoClick = viewModel::onAddPhotoClick,
        onPhotoLongClick = viewModel::onEvidenceLongClick,
        onConfirmDeletePhoto = viewModel::onConfirmDeleteEvidence,
        onDismissDeletePhoto = viewModel::onDismissDeleteEvidence,
        onPreviousClick = viewModel::onPreviousClick,
        onNextClick = viewModel::onNextClick,
    )

    ExecutionScreen(
        uiState = uiState,
        isOnline = isOnline,
        actions = actions,
        snackbarHostState = snackbarHostState,
    )
}

/** Escucha los eventos de una sola vez del ViewModel y los resuelve con APIs de Android. */
@Composable
private fun ExecutionEventsEffect(
    viewModel: ExecutionViewModel,
    snackbarHostState: SnackbarHostState,
    cameraLauncher: ActivityResultLauncher<Uri>,
    onGoToReview: (inspectionId: String) -> Unit,
) {
    val photoSavedMessage = stringResource(R.string.execution_photo_saved)
    val noCameraMessage = stringResource(R.string.execution_no_camera)

    // Los Snackbars se muestran en su propia corrutina: `showSnackbar` espera a que se cierre,
    // y no queremos que eso demore otros eventos (por ejemplo, ir a P4).
    val snackbarScope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ExecutionEvent.NavigateToReview -> {
                    onGoToReview(event.inspectionId)
                }

                is ExecutionEvent.OpenCamera -> {
                    openCamera(
                        cameraLauncher = cameraLauncher,
                        contentUri = event.contentUri,
                        onCameraUnavailable = viewModel::onCameraUnavailable,
                    )
                }

                is ExecutionEvent.PhotoSaved -> {
                    snackbarScope.launch {
                        snackbarHostState.showSnackbar(photoSavedMessage)
                    }
                }

                is ExecutionEvent.CameraUnavailable -> {
                    snackbarScope.launch {
                        snackbarHostState.showSnackbar(noCameraMessage)
                    }
                }

                is ExecutionEvent.ShowError -> {
                    snackbarScope.launch {
                        snackbarHostState.showSnackbar(event.message)
                    }
                }
            }
        }
    }
}

/**
 * Abre la cámara del sistema. Si el teléfono no tiene app de cámara, Android lanza
 * [ActivityNotFoundException]: se atrapa para avisar en vez de cerrar la app.
 */
private fun openCamera(
    cameraLauncher: ActivityResultLauncher<Uri>,
    contentUri: String,
    onCameraUnavailable: () -> Unit,
) {
    val uri = Uri.parse(contentUri)
    try {
        cameraLauncher.launch(uri)
    } catch (exception: ActivityNotFoundException) {
        onCameraUnavailable()
    }
}
