package ar.edu.uade.fieldcheck.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Colores de estado que Material 3 no trae (Cumple, No cumple, N/A, banner offline)
@Immutable
data class StatusColors(
    val complies: Color,
    val onComplies: Color,
    val notComplies: Color,
    val onNotComplies: Color,
    val notApplicable: Color,
    val onNotApplicable: Color,
    val offlineBanner: Color,
    val onOfflineBanner: Color,
)

private val LightStatusColors = StatusColors(
    complies = CompliesLight,
    onComplies = OnCompliesLight,
    notComplies = NotCompliesLight,
    onNotComplies = OnNotCompliesLight,
    notApplicable = NotApplicableLight,
    onNotApplicable = OnNotApplicableLight,
    offlineBanner = OfflineBannerLight,
    onOfflineBanner = OnOfflineBannerLight,
)

private val DarkStatusColors = StatusColors(
    complies = CompliesDark,
    onComplies = OnCompliesDark,
    notComplies = NotCompliesDark,
    onNotComplies = OnNotCompliesDark,
    notApplicable = NotApplicableDark,
    onNotApplicable = OnNotApplicableDark,
    offlineBanner = OfflineBannerDark,
    onOfflineBanner = OnOfflineBannerDark,
)

private val LightColors = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    surfaceVariant = SurfaceVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    error = NotCompliesLight,
    onError = OnNotCompliesLight,
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = BackgroundDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    surfaceVariant = SurfaceVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    error = NotCompliesDark,
    onError = OnNotCompliesDark,
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

// Sin color dinámico a propósito: los colores de estado tienen que ser siempre los mismos
@Composable
fun FieldCheckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val statusColors = if (darkTheme) DarkStatusColors else LightStatusColors
    CompositionLocalProvider(LocalStatusColors provides statusColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = FieldCheckTypography,
            shapes = FieldCheckShapes,
            content = content,
        )
    }
}

// Acceso cómodo desde cualquier composable: MaterialTheme.statusColors.complies
val MaterialTheme.statusColors: StatusColors
    @Composable get() = LocalStatusColors.current
