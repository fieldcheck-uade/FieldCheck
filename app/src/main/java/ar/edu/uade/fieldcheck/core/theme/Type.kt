package ar.edu.uade.fieldcheck.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Roboto del sistema. Tamaños en sp para respetar la escala de fuente del usuario (docs/pantallas.md 1.2).
// Los estilos chicos se suben a 14 sp: en campo no usamos texto menor a eso.
private val base = Typography()

val FieldCheckTypography = base.copy(
    headlineSmall = base.headlineSmall.copy(fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = base.titleLarge.copy(fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = base.titleMedium.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = base.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = base.bodySmall.copy(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = base.labelLarge.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
)
