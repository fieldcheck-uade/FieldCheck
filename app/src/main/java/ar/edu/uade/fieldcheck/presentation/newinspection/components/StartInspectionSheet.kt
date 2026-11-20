package ar.edu.uade.fieldcheck.presentation.newinspection.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.fieldcheck.R
import ar.edu.uade.fieldcheck.core.theme.FieldCheckTheme
import ar.edu.uade.fieldcheck.domain.model.Template
import ar.edu.uade.fieldcheck.presentation.newinspection.StartSheetState

/**
 * Hoja inferior para empezar la inspección (Figma 3:93).
 *
 * Se usa una hoja y no otra pantalla porque deja el campo y el botón en la zona del pulgar
 * y le ahorra al inspector un paso (docs/pantallas.md, P2).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartInspectionSheet(
    state: StartSheetState,
    onSiteNameChange: (String) -> Unit,
    onStartClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Abierta del todo: con el teclado arriba, una hoja a medio abrir taparía el botón.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        StartInspectionSheetContent(
            state = state,
            onSiteNameChange = onSiteNameChange,
            onStartClick = onStartClick,
            requestFocusOnOpen = true,
        )
    }
}

/**
 * Contenido de la hoja, separado de [ModalBottomSheet] para poder verlo en el Preview.
 *
 * @param requestFocusOnOpen en la app abre el teclado solo; en el Preview se apaga porque
 *   ahí no hay ventana a la que darle el foco.
 */
@Composable
fun StartInspectionSheetContent(
    state: StartSheetState,
    onSiteNameChange: (String) -> Unit,
    onStartClick: () -> Unit,
    requestFocusOnOpen: Boolean,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    if (requestFocusOnOpen) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SheetTemplateHeader(template = state.template)

        SiteField(
            siteName = state.siteName,
            siteError = state.siteError,
            enabled = !state.isCreating,
            onSiteNameChange = onSiteNameChange,
            onDone = onStartClick,
            modifier = Modifier.focusRequester(focusRequester),
        )

        val creationError = state.creationError
        if (creationError != null) {
            Text(
                text = creationError,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        StartButton(
            isCreating = state.isCreating,
            onClick = onStartClick,
        )
    }
}

@Composable
private fun SheetTemplateHeader(template: Template) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = template.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = templateDetailsText(template),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Campo "Sitio *". Debajo siempre se lee "Obligatorio": en gris como ayuda y en el color
 * de error cuando el caso de uso rechazó el sitio vacío (regla R1).
 */
@Composable
private fun SiteField(
    siteName: String,
    siteError: String?,
    enabled: Boolean,
    onSiteNameChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasError = siteError != null
    val supportingText: String
    if (siteError != null) {
        supportingText = siteError
    } else {
        supportingText = stringResource(R.string.new_inspection_site_required)
    }

    OutlinedTextField(
        value = siteName,
        onValueChange = onSiteNameChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(text = stringResource(R.string.new_inspection_site_label)) },
        placeholder = { Text(text = stringResource(R.string.new_inspection_site_placeholder)) },
        supportingText = { Text(text = supportingText) },
        isError = hasError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        // "Listo" en el teclado hace lo mismo que el botón, para no tener que bajar el teclado.
        keyboardActions = KeyboardActions(onDone = { onDone() }),
    )
}

/** Botón de ancho completo y 56 dp de alto (Figma 3:105). Mientras guarda, muestra un indicador. */
@Composable
private fun StartButton(
    isCreating: Boolean,
    onClick: () -> Unit,
) {
    val creatingDescription = stringResource(R.string.new_inspection_creating)

    Button(
        onClick = onClick,
        enabled = !isCreating,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
    ) {
        if (isCreating) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(24.dp)
                    .semantics { contentDescription = creatingDescription },
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = stringResource(R.string.new_inspection_start),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ----- Previews -----

private fun previewSheetState(
    siteName: String = "Obra Av. Siempreviva 742 — Subsuelo 2",
    siteError: String? = null,
    isCreating: Boolean = false,
): StartSheetState {
    val template = Template(
        id = "1",
        name = "Control semanal de obra",
        version = 3,
        itemCount = 20,
        downloadedAt = 0L,
    )
    return StartSheetState(
        template = template,
        siteName = siteName,
        siteError = siteError,
        creationError = null,
        isCreating = isCreating,
    )
}

@Preview(showBackground = true)
@Composable
private fun StartInspectionSheetContentPreview() {
    FieldCheckTheme {
        Surface {
            StartInspectionSheetContent(
                state = previewSheetState(),
                onSiteNameChange = {},
                onStartClick = {},
                requestFocusOnOpen = false,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StartInspectionSheetContentErrorPreview() {
    FieldCheckTheme {
        Surface {
            StartInspectionSheetContent(
                state = previewSheetState(siteName = "", siteError = "Obligatorio"),
                onSiteNameChange = {},
                onStartClick = {},
                requestFocusOnOpen = false,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StartInspectionSheetContentCreatingPreview() {
    FieldCheckTheme {
        Surface {
            StartInspectionSheetContent(
                state = previewSheetState(isCreating = true),
                onSiteNameChange = {},
                onStartClick = {},
                requestFocusOnOpen = false,
            )
        }
    }
}
