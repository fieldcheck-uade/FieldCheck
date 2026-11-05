package ar.edu.uade.fieldcheck.presentation.execution

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ar.edu.uade.fieldcheck.R

/**
 * Contenedor de la pantalla P3 · Ejecución de la inspección.
 *
 * TEMPORAL: pantalla en construcción. La reemplaza la implementación real de P3,
 * que mantiene esta misma firma para no tocar la navegación (`FieldCheckApp`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecutionRoute(
    onBackClick: () -> Unit,
    onGoToReview: (inspectionId: String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.execution_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.under_construction),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
