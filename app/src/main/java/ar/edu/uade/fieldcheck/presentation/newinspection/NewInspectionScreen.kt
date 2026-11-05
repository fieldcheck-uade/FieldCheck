package ar.edu.uade.fieldcheck.presentation.newinspection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.fieldcheck.R
import ar.edu.uade.fieldcheck.core.model.UiState
import ar.edu.uade.fieldcheck.core.theme.FieldCheckTheme
import ar.edu.uade.fieldcheck.domain.model.Template
import ar.edu.uade.fieldcheck.presentation.common.components.EmptyState
import ar.edu.uade.fieldcheck.presentation.common.components.ErrorState
import ar.edu.uade.fieldcheck.presentation.common.components.OfflineBanner
import ar.edu.uade.fieldcheck.presentation.newinspection.components.StartInspectionSheet
import ar.edu.uade.fieldcheck.presentation.newinspection.components.TemplateCard
import java.time.ZoneId

/**
 * Pantalla P2 sin lógica: recibe el estado y avisa las acciones por callbacks.
 * Al no depender del ViewModel se puede ver en el Preview de Android Studio.
 *
 * @param startSheetState `null` con la hoja inferior cerrada.
 * @param nowMillis "ahora", para calcular la antigüedad de las plantillas; los previews pasan uno fijo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewInspectionScreen(
    templatesState: UiState<List<Template>>,
    isOnline: Boolean,
    startSheetState: StartSheetState?,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onTemplateClick: (Template) -> Unit,
    onSiteNameChange: (String) -> Unit,
    onStartInspectionClick: () -> Unit,
    onSheetDismiss: () -> Unit,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.new_inspection_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                // Igual que en P1: la barra no tiene fondo propio en Figma.
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // El banner informa pero no bloquea: las plantillas guardadas se usan igual sin red.
            if (!isOnline) {
                OfflineBanner()
            }

            NewInspectionContent(
                templatesState = templatesState,
                isOnline = isOnline,
                onRetryClick = onRetryClick,
                onTemplateClick = onTemplateClick,
                nowMillis = nowMillis,
                zoneId = zoneId,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }

    if (startSheetState != null) {
        StartInspectionSheet(
            state = startSheetState,
            onSiteNameChange = onSiteNameChange,
            onStartClick = onStartInspectionClick,
            onDismiss = onSheetDismiss,
        )
    }
}

@Composable
private fun NewInspectionContent(
    templatesState: UiState<List<Template>>,
    isOnline: Boolean,
    onRetryClick: () -> Unit,
    onTemplateClick: (Template) -> Unit,
    nowMillis: Long,
    zoneId: ZoneId,
    modifier: Modifier = Modifier,
) {
    when (templatesState) {
        is UiState.Loading -> {
            TemplatesLoading(modifier = modifier)
        }

        is UiState.Error -> {
            ErrorState(
                message = templatesState.message,
                onRetryClick = onRetryClick,
                modifier = modifier,
            )
        }

        is UiState.Success -> {
            val templates = templatesState.data
            if (templates.isEmpty()) {
                TemplatesEmpty(
                    isOnline = isOnline,
                    onRetryClick = onRetryClick,
                    modifier = modifier,
                )
            } else {
                val age = TemplatesAgeCalculator.calculate(templates, nowMillis, zoneId)
                TemplateList(
                    templates = templates,
                    age = age,
                    isOnline = isOnline,
                    onTemplateClick = onTemplateClick,
                    modifier = modifier,
                )
            }
        }
    }
}

/** Primera descarga: indicador circular con texto, para que se entienda qué se espera. */
@Composable
private fun TemplatesLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.new_inspection_loading),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Sin plantillas. Sin conexión el motivo más probable es que nunca se descargaron
 * (Figma 46:51); con conexión, que el servidor no tiene ninguna cargada.
 */
@Composable
private fun TemplatesEmpty(
    isOnline: Boolean,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val retryButton: @Composable () -> Unit = {
        RetryOutlinedButton(onClick = onRetryClick)
    }

    if (isOnline) {
        EmptyState(
            title = stringResource(R.string.new_inspection_empty_title),
            description = stringResource(R.string.new_inspection_empty_description),
            icon = Icons.Outlined.Inventory2,
            action = retryButton,
            modifier = modifier,
        )
    } else {
        EmptyState(
            title = stringResource(R.string.new_inspection_empty_offline_title),
            description = stringResource(R.string.new_inspection_empty_offline_description),
            icon = Icons.Outlined.CloudOff,
            action = retryButton,
            modifier = modifier,
        )
    }
}

/** Botón delineado "Reintentar" con ícono, como en Figma 46:51. */
@Composable
private fun RetryOutlinedButton(onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.action_retry),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun TemplateList(
    templates: List<Template>,
    age: TemplatesAge?,
    isOnline: Boolean,
    onTemplateClick: (Template) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (age != null) {
            item(key = "templates-age") {
                Text(
                    text = templatesAgeText(age = age, isOnline = isOnline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
            }
        }
        items(
            items = templates,
            key = { template -> template.id },
        ) { template ->
            TemplateCard(
                template = template,
                onClick = { onTemplateClick(template) },
            )
        }
    }
}

/**
 * "Plantillas actualizadas hoy, 09:14". Sin conexión se agrega que se van a actualizar
 * cuando vuelva la red (Figma 46:50), para que el inspector sepa que puede usarlas igual.
 */
@Composable
private fun templatesAgeText(age: TemplatesAge, isOnline: Boolean): String {
    val whenText: String
    when (age) {
        is TemplatesAge.Today -> {
            whenText = stringResource(R.string.new_inspection_age_today, age.time)
        }

        is TemplatesAge.Yesterday -> {
            whenText = stringResource(R.string.new_inspection_age_yesterday, age.time)
        }

        is TemplatesAge.DaysAgo -> {
            whenText = pluralStringResource(R.plurals.new_inspection_age_days, age.days, age.days)
        }
    }

    if (isOnline) {
        return stringResource(R.string.new_inspection_templates_age, whenText)
    }
    return stringResource(R.string.new_inspection_templates_age_offline, whenText)
}

// ----- Previews: se ven en Android Studio sin correr la app -----

/** "Ahora" fijo de los previews: 25/09/2026 18:00 UTC. */
private const val PREVIEW_NOW = 1_790_362_800_000L
private const val ONE_HOUR = 60L * 60L * 1000L
private val PREVIEW_ZONE: ZoneId = ZoneId.of("UTC")

private fun previewTemplates(hoursAgo: Long): List<Template> {
    val downloadedAt = PREVIEW_NOW - hoursAgo * ONE_HOUR
    return listOf(
        Template(id = "1", name = "Control semanal de obra", version = 3, itemCount = 20, downloadedAt = downloadedAt),
        Template(id = "2", name = "Sala técnica", version = 2, itemCount = 14, downloadedAt = downloadedAt),
        Template(id = "3", name = "Seguridad e higiene", version = 5, itemCount = 32, downloadedAt = downloadedAt),
        Template(id = "4", name = "Tableros eléctricos", version = 1, itemCount = 12, downloadedAt = downloadedAt),
    )
}

@Composable
private fun NewInspectionScreenPreviewTemplate(
    templatesState: UiState<List<Template>>,
    isOnline: Boolean,
    startSheetState: StartSheetState? = null,
) {
    FieldCheckTheme {
        NewInspectionScreen(
            templatesState = templatesState,
            isOnline = isOnline,
            startSheetState = startSheetState,
            onBackClick = {},
            onRetryClick = {},
            onTemplateClick = {},
            onSiteNameChange = {},
            onStartInspectionClick = {},
            onSheetDismiss = {},
            nowMillis = PREVIEW_NOW,
            zoneId = PREVIEW_ZONE,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NewInspectionContentPreview() {
    NewInspectionScreenPreviewTemplate(
        templatesState = UiState.Success(previewTemplates(hoursAgo = 9)),
        isOnline = true,
    )
}

@Preview(showBackground = true)
@Composable
private fun NewInspectionSheetOpenPreview() {
    val templates = previewTemplates(hoursAgo = 9)
    val sheet = StartSheetState(
        template = templates[0],
        siteName = "Obra Av. Siempreviva 742 — Subsuelo 2",
        siteError = null,
        creationError = null,
        isCreating = false,
    )
    NewInspectionScreenPreviewTemplate(
        templatesState = UiState.Success(templates),
        isOnline = true,
        startSheetState = sheet,
    )
}

@Preview(showBackground = true)
@Composable
private fun NewInspectionOutdatedOfflinePreview() {
    NewInspectionScreenPreviewTemplate(
        templatesState = UiState.Success(previewTemplates(hoursAgo = 72)),
        isOnline = false,
    )
}

@Preview(showBackground = true)
@Composable
private fun NewInspectionEmptyOfflinePreview() {
    NewInspectionScreenPreviewTemplate(
        templatesState = UiState.Success(emptyList()),
        isOnline = false,
    )
}

@Preview(showBackground = true)
@Composable
private fun NewInspectionEmptyOnlinePreview() {
    NewInspectionScreenPreviewTemplate(
        templatesState = UiState.Success(emptyList()),
        isOnline = true,
    )
}

@Preview(showBackground = true)
@Composable
private fun NewInspectionLoadingPreview() {
    NewInspectionScreenPreviewTemplate(
        templatesState = UiState.Loading,
        isOnline = true,
    )
}

@Preview(showBackground = true)
@Composable
private fun NewInspectionErrorPreview() {
    NewInspectionScreenPreviewTemplate(
        templatesState = UiState.Error("No pudimos descargar las plantillas. Probá de nuevo en unos minutos."),
        isOnline = true,
    )
}
