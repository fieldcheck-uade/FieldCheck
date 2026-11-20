package ar.edu.uade.fieldcheck.presentation.execution

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.fieldcheck.core.model.UiState
import ar.edu.uade.fieldcheck.core.theme.FieldCheckTheme
import ar.edu.uade.fieldcheck.domain.model.Evidence
import ar.edu.uade.fieldcheck.domain.model.ItemResult
import ar.edu.uade.fieldcheck.domain.model.ItemResultValue
import ar.edu.uade.fieldcheck.presentation.common.components.ErrorState
import ar.edu.uade.fieldcheck.presentation.common.components.LoadingState
import ar.edu.uade.fieldcheck.presentation.common.components.OfflineBanner
import ar.edu.uade.fieldcheck.presentation.execution.components.DeleteEvidenceDialog
import ar.edu.uade.fieldcheck.presentation.execution.components.EvidenceRow
import ar.edu.uade.fieldcheck.presentation.execution.components.EvidenceWarningCard
import ar.edu.uade.fieldcheck.presentation.execution.components.ExecutionTopBar
import ar.edu.uade.fieldcheck.presentation.execution.components.ItemProgress
import ar.edu.uade.fieldcheck.presentation.execution.components.NoteField
import ar.edu.uade.fieldcheck.presentation.execution.components.PhotoViewerDialog
import ar.edu.uade.fieldcheck.presentation.execution.components.ReadOnlyNotice
import ar.edu.uade.fieldcheck.presentation.execution.components.SavedIndicator
import ar.edu.uade.fieldcheck.presentation.execution.components.ThumbZone

/**
 * Todas las acciones que la pantalla P3 avisa hacia afuera.
 *
 * Se agrupan en una clase para no tener una función con quince parámetros sueltos.
 */
data class ExecutionActions(
    val onBackClick: () -> Unit,
    val onSeeAllClick: () -> Unit,
    val onRetryClick: () -> Unit,
    val onResultClick: (ItemResultValue) -> Unit,
    val onNoteChange: (String) -> Unit,
    val onAddPhotoClick: () -> Unit,
    val onPhotoLongClick: (Evidence) -> Unit,
    val onConfirmDeletePhoto: () -> Unit,
    val onDismissDeletePhoto: () -> Unit,
    val onPreviousClick: () -> Unit,
    val onNextClick: () -> Unit,
)

/**
 * Pantalla P3 sin lógica: recibe el estado y avisa las acciones por callbacks.
 * Al no depender del ViewModel se puede ver en el Preview de Android Studio.
 */
@Composable
fun ExecutionScreen(
    uiState: UiState<ExecutionContent>,
    isOnline: Boolean,
    actions: ExecutionActions,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val content: ExecutionContent?
    if (uiState is UiState.Success) {
        content = uiState.data
    } else {
        content = null
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ExecutionTopBarFor(content = content, actions = actions)
        },
        bottomBar = {
            // La zona del pulgar solo tiene sentido cuando hay un ítem en pantalla.
            if (content != null) {
                ExecutionThumbZone(content = content, actions = actions)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (uiState) {
                is UiState.Loading -> {
                    LoadingState(modifier = Modifier.fillMaxSize())
                }

                is UiState.Error -> {
                    ErrorState(
                        message = uiState.message,
                        onRetryClick = actions.onRetryClick,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                is UiState.Success -> {
                    ItemProgress(
                        itemNumber = uiState.data.itemNumber,
                        totalItems = uiState.data.totalItems,
                        progress = uiState.data.progress(),
                    )
                    // El banner informa pero no deshabilita nada: todo se guarda en el teléfono.
                    if (!isOnline) {
                        OfflineBanner()
                    }
                    ItemReadingAndEvidence(
                        content = uiState.data,
                        actions = actions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExecutionTopBarFor(
    content: ExecutionContent?,
    actions: ExecutionActions,
) {
    if (content == null) {
        ExecutionTopBar(
            siteName = null,
            onBackClick = actions.onBackClick,
            onSeeAllClick = null,
        )
    } else {
        ExecutionTopBar(
            siteName = content.siteName,
            onBackClick = actions.onBackClick,
            onSeeAllClick = actions.onSeeAllClick,
        )
    }
}

/** Agrega la vibración corta al marcar un resultado: confirma sin tener que mirar la pantalla. */
@Composable
private fun ExecutionThumbZone(
    content: ExecutionContent,
    actions: ExecutionActions,
) {
    val haptics = LocalHapticFeedback.current

    ThumbZone(
        selectedResult = content.item.result,
        canEdit = !content.isReadOnly,
        isFirstItem = content.isFirstItem(),
        isLastItem = content.isLastItem(),
        onResultClick = { result ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            actions.onResultClick(result)
        },
        onPreviousClick = actions.onPreviousClick,
        onNextClick = actions.onNextClick,
    )
}

/**
 * Mitad superior: texto del ítem, "Guardado", nota, fotos y aviso de evidencia.
 * Se puede desplazar porque con el teclado abierto o muchas fotos no entra todo.
 */
@Composable
private fun ItemReadingAndEvidence(
    content: ExecutionContent,
    actions: ExecutionActions,
    modifier: Modifier = Modifier,
) {
    // La foto abierta a pantalla completa es estado de la vista: no afecta datos ni reglas.
    var photoInViewer by remember { mutableStateOf<Evidence?>(null) }
    val canEdit = !content.isReadOnly

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (content.isReadOnly) {
            ReadOnlyNotice()
        }

        Text(
            text = content.item.itemText,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (content.showsSavedIndicator()) {
            SavedIndicator()
        }

        NoteField(
            note = content.noteDraft,
            onNoteChange = actions.onNoteChange,
            enabled = canEdit,
        )

        EvidenceRow(
            evidence = content.item.evidence,
            itemNumber = content.itemNumber,
            canEdit = canEdit,
            onAddPhotoClick = actions.onAddPhotoClick,
            onPhotoClick = { photo -> photoInViewer = photo },
            onPhotoLongClick = actions.onPhotoLongClick,
        )

        if (content.showsEvidenceWarning) {
            EvidenceWarningCard()
        }
    }

    val photoToShow = photoInViewer
    if (photoToShow != null) {
        PhotoViewerDialog(
            evidence = photoToShow,
            onDismiss = { photoInViewer = null },
        )
    }

    if (content.evidencePendingDeletion != null) {
        DeleteEvidenceDialog(
            onConfirm = actions.onConfirmDeletePhoto,
            onDismiss = actions.onDismissDeletePhoto,
        )
    }
}

// region Previews

private fun previewActions(): ExecutionActions {
    return ExecutionActions(
        onBackClick = {},
        onSeeAllClick = {},
        onRetryClick = {},
        onResultClick = {},
        onNoteChange = {},
        onAddPhotoClick = {},
        onPhotoLongClick = {},
        onConfirmDeletePhoto = {},
        onDismissDeletePhoto = {},
        onPreviousClick = {},
        onNextClick = {},
    )
}

/** Figma 4:15: "No cumple" sin evidencia. */
@Preview(showBackground = true, heightDp = 800)
@Composable
private fun ExecutionNotCompliesWithoutEvidencePreview() {
    val item = ItemResult(
        id = 7L,
        position = 6,
        itemText = "Los matafuegos están señalizados y con la carga vigente",
        result = ItemResultValue.NOT_COMPLIES,
        note = null,
        evidence = emptyList(),
    )
    val content = ExecutionContent(
        siteName = "Obra Av. Siempreviva 742 — Subsuelo 2",
        item = item,
        itemNumber = 7,
        totalItems = 20,
        noteDraft = "",
        showsEvidenceWarning = true,
        isReadOnly = false,
        evidencePendingDeletion = null,
    )

    FieldCheckTheme {
        ExecutionScreen(
            uiState = UiState.Success(content),
            isOnline = true,
            actions = previewActions(),
        )
    }
}

/** Figma 4:76: modo oscuro, sin conexión y con una foto. */
@Preview(showBackground = true, heightDp = 800, backgroundColor = 0xFF121316)
@Composable
private fun ExecutionDarkOfflinePreview() {
    val photo = Evidence(
        clientUuid = "photo-1",
        filePath = "",
        capturedAt = 0L,
        latitude = -34.6,
        longitude = -58.4,
    )
    val item = ItemResult(
        id = 8L,
        position = 7,
        itemText = "Las vías de escape están libres y señalizadas",
        result = ItemResultValue.COMPLIES,
        note = "Cartelería fotoluminiscente en ambos accesos.",
        evidence = listOf(photo),
    )
    val content = ExecutionContent(
        siteName = "Obra Av. Siempreviva 742 — Subsuelo 2",
        item = item,
        itemNumber = 8,
        totalItems = 20,
        noteDraft = "Cartelería fotoluminiscente en ambos accesos.",
        showsEvidenceWarning = false,
        isReadOnly = false,
        evidencePendingDeletion = null,
    )

    FieldCheckTheme(darkTheme = true) {
        ExecutionScreen(
            uiState = UiState.Success(content),
            isOnline = false,
            actions = previewActions(),
        )
    }
}

// endregion
