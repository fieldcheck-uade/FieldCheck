package ar.edu.uade.fieldcheck.presentation.newinspection.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.fieldcheck.R
import ar.edu.uade.fieldcheck.core.theme.FieldCheckTheme
import ar.edu.uade.fieldcheck.domain.model.Template

/**
 * Tarjeta de una plantilla en P2 (Figma 3:60): nombre, "20 ítems · versión 3" y un ícono
 * de portapapeles. Toda la tarjeta es tocable, así el área de toque es grande (criterio A1).
 */
@Composable
fun TemplateCard(
    template: Template,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = templateDetailsText(template),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Decorativo: el nombre de la plantilla ya describe la tarjeta para TalkBack.
            Icon(
                imageVector = Icons.Outlined.AssignmentTurnedIn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** "20 ítems · versión 3". Se reutiliza en la hoja inferior para que ambos textos coincidan. */
@Composable
fun templateDetailsText(template: Template): String {
    return pluralStringResource(
        R.plurals.new_inspection_template_details,
        template.itemCount,
        template.itemCount,
        template.version,
    )
}

@Preview(showBackground = true)
@Composable
private fun TemplateCardPreview() {
    val template = Template(
        id = "1",
        name = "Control semanal de obra",
        version = 3,
        itemCount = 20,
        downloadedAt = 0L,
    )
    FieldCheckTheme {
        TemplateCard(template = template, onClick = {})
    }
}
