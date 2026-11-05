package ar.edu.uade.fieldcheck.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.fieldcheck.R
import ar.edu.uade.fieldcheck.core.model.UiState
import ar.edu.uade.fieldcheck.core.theme.FieldCheckTheme
import ar.edu.uade.fieldcheck.domain.model.Evidence
import ar.edu.uade.fieldcheck.domain.model.Inspection
import ar.edu.uade.fieldcheck.domain.model.InspectionStatus
import ar.edu.uade.fieldcheck.domain.model.ItemResult
import ar.edu.uade.fieldcheck.domain.model.ItemResultValue
import ar.edu.uade.fieldcheck.domain.model.ResultCounts
import ar.edu.uade.fieldcheck.domain.model.SyncStatus
import ar.edu.uade.fieldcheck.presentation.common.components.ErrorState
import ar.edu.uade.fieldcheck.presentation.common.components.LoadingState
import ar.edu.uade.fieldcheck.presentation.common.components.ResultCountsRow
import ar.edu.uade.fieldcheck.presentation.detail.components.FullScreenPhotoDialog
import ar.edu.uade.fieldcheck.presentation.detail.components.NotCompliesItemCard
import ar.edu.uade.fieldcheck.presentation.detail.components.SyncStatusCard
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Formato de "Inicio" y "Fin": "28/09/2026 10:05", en la hora local del teléfono. */
private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

/** Foto que el usuario está viendo a pantalla completa, con su descripción para TalkBack. */
private data class SelectedPhoto(
    val evidence: Evidence,
    val description: String,
)

/**
 * Pantalla P5 sin lógica: recibe el estado y avisa las acciones por callbacks.
 * Al no depender del ViewModel se puede ver en el Preview de Android Studio.
 *
 * El único estado propio es qué foto está abierta a pantalla completa: es algo
 * puramente visual que no le importa al ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    uiState: UiState<DetailContent>,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onRetryLoadClick: () -> Unit,
    onRetrySyncClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    var selectedPhoto by remember { mutableStateOf<SelectedPhoto?>(null) }

    Scaffold(
        topBar = {
            DetailTopBar(
                title = chooseTitle(uiState),
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            // La zona del pulgar solo tiene sentido cuando hay algo para compartir.
            if (uiState is UiState.Success) {
                ShareThumbZone(onShareClick = onShareClick)
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when (uiState) {
            is UiState.Loading -> {
                LoadingState(modifier = contentModifier)
            }

            is UiState.Error -> {
                ErrorState(
                    message = uiState.message,
                    onRetryClick = onRetryLoadClick,
                    modifier = contentModifier,
                )
            }

            is UiState.Success -> {
                DetailContentList(
                    content = uiState.data,
                    onRetrySyncClick = onRetrySyncClick,
                    onPhotoClick = { evidence, description ->
                        selectedPhoto = SelectedPhoto(evidence = evidence, description = description)
                    },
                    modifier = contentModifier,
                )
            }
        }
    }

    val photo = selectedPhoto
    if (photo != null) {
        FullScreenPhotoDialog(
            evidence = photo.evidence,
            description = photo.description,
            onDismiss = { selectedPhoto = null },
        )
    }
}

/** En la barra va el nombre del sitio; mientras carga o si falló, el título genérico. */
@Composable
private fun chooseTitle(uiState: UiState<DetailContent>): String {
    if (uiState is UiState.Success) {
        return uiState.data.inspection.siteName
    }
    return stringResource(R.string.detail_title)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTopBar(
    title: String,
    onBackClick: () -> Unit,
) {
    TopAppBar(
        title = {
            // Los nombres de sitio suelen ser largos: una línea con "…" como en Figma.
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

/**
 * Contenido desplazable: tarjeta de sincronización, datos generales, conteos
 * y la sección "No cumple". Es una `LazyColumn` porque puede haber muchos ítems.
 */
@Composable
private fun DetailContentList(
    content: DetailContent,
    onRetrySyncClick: () -> Unit,
    onPhotoClick: (evidence: Evidence, description: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inspection = content.inspection

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SyncStatusCard(
                syncStatus = inspection.syncStatus,
                lastSyncError = content.lastSyncError,
                onRetryClick = onRetrySyncClick,
            )
        }

        item {
            GeneralData(inspection = inspection)
        }

        item {
            ResultCountsRow(counts = content.counts)
        }

        item {
            NotCompliesSectionTitle()
        }

        if (content.notCompliesItems.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.detail_no_not_complies),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        } else {
            items(
                items = content.notCompliesItems,
                key = { item -> item.id },
            ) { item ->
                NotCompliesItemCard(
                    item = item,
                    onPhotoClick = onPhotoClick,
                )
            }
        }
    }
}

/** Datos generales (Figma 5:176): etiqueta a la izquierda y valor a la derecha. */
@Composable
private fun GeneralData(inspection: Inspection) {
    val startedAtText = formatDateTime(inspection.startedAt)
    val finishedAtText = formatOptionalDateTime(inspection.finishedAt)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        GeneralDataRow(
            label = stringResource(R.string.detail_label_template),
            value = inspection.templateName,
        )
        GeneralDataRow(
            label = stringResource(R.string.detail_label_started_at),
            value = startedAtText,
        )
        GeneralDataRow(
            label = stringResource(R.string.detail_label_finished_at),
            value = finishedAtText,
        )
    }
}

@Composable
private fun GeneralDataRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // weight(1f) + alineado a la derecha: si el valor es largo, baja de línea sin pisar la etiqueta.
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun NotCompliesSectionTitle() {
    Text(
        text = stringResource(R.string.detail_section_not_complies),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(start = 4.dp)
            .semantics { heading() },
    )
}

/** Zona del pulgar (Figma 5:247): botón de ancho completo "Compartir resumen". */
@Composable
private fun ShareThumbZone(onShareClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Button(
                onClick = onShareClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .heightIn(min = 56.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.IosShare,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.detail_share_summary),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

private fun formatDateTime(epochMillis: Long): String {
    val instant = Instant.ofEpochMilli(epochMillis)
    val localDateTime = instant.atZone(ZoneId.systemDefault())
    return dateTimeFormatter.format(localDateTime)
}

/** Una inspección finalizada siempre tiene fin; si faltara, mostramos un guion en lugar de fallar. */
@Composable
private fun formatOptionalDateTime(epochMillis: Long?): String {
    if (epochMillis == null) {
        return stringResource(R.string.detail_no_date)
    }
    return formatDateTime(epochMillis)
}

// ---------- Previews ----------

private fun previewContent(syncStatus: SyncStatus, lastSyncError: String?): DetailContent {
    val inspection = Inspection(
        clientUuid = "preview",
        templateName = "Control semanal de obra",
        siteName = "Obra Av. Siempreviva 742 — Subsuelo 2",
        status = InspectionStatus.FINISHED,
        syncStatus = syncStatus,
        startedAt = 1_790_000_000_000L,
        finishedAt = 1_790_002_580_000L,
        totalItems = 20,
        completedItems = 20,
    )
    val firstItem = ItemResult(
        id = 7L,
        position = 6,
        itemText = "Matafuegos señalizados y con carga vigente",
        result = ItemResultValue.NOT_COMPLIES,
        note = "Falta señalización en el matafuego del palier.",
        evidence = emptyList(),
    )
    val secondItem = ItemResult(
        id = 12L,
        position = 11,
        itemText = "Tableros eléctricos con tapa",
        result = ItemResultValue.NOT_COMPLIES,
        note = "Tablero TS-3 sin tapa.",
        evidence = emptyList(),
    )

    return DetailContent(
        inspection = inspection,
        counts = ResultCounts(complies = 16, notComplies = 3, notApplicable = 1, pending = 0),
        notCompliesItems = listOf(firstItem, secondItem),
        lastSyncError = lastSyncError,
    )
}

@Preview(showBackground = true)
@Composable
private fun DetailScreenPendingPreview() {
    FieldCheckTheme {
        DetailScreen(
            uiState = UiState.Success(previewContent(SyncStatus.PENDING, null)),
            snackbarHostState = remember { SnackbarHostState() },
            onBackClick = {},
            onRetryLoadClick = {},
            onRetrySyncClick = {},
            onShareClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailScreenErrorSyncPreview() {
    FieldCheckTheme {
        DetailScreen(
            uiState = UiState.Success(
                previewContent(SyncStatus.FAILED, "El servidor no respondió después de varios intentos."),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onBackClick = {},
            onRetryLoadClick = {},
            onRetrySyncClick = {},
            onShareClick = {},
        )
    }
}
