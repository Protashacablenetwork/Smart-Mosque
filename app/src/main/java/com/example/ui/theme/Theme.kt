package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = OnEmeraldPrimaryContainer,
    secondary = GoldSecondary,
    onSecondary = Color.White,
    secondaryContainer = GoldSecondaryContainer,
    onSecondaryContainer = OnGoldSecondaryContainer,
    tertiary = TealTertiary,
    onTertiary = Color.White,
    tertiaryContainer = TealTertiaryContainer,
    onTertiaryContainer = OnTealTertiaryContainer,
    background = ParchmentBackground,
    onBackground = InkOnSurface,
    surface = ParchmentSurface,
    onSurface = InkOnSurface,
    surfaceVariant = ParchmentSurfaceVariant,
    onSurfaceVariant = InkOnSurfaceVariant,
    outline = ParchmentOutline,
    outlineVariant = ParchmentOutlineVariant,
    error = SemanticError,
    onError = Color.White,
    errorContainer = SemanticErrorContainer,
    onErrorContainer = SemanticError
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = DarkOnEmeraldPrimary,
    primaryContainer = DarkEmeraldContainer,
    onPrimaryContainer = DarkOnEmeraldContainer,
    secondary = DarkGoldSecondary,
    onSecondary = DarkOnGoldSecondary,
    secondaryContainer = DarkGoldContainer,
    onSecondaryContainer = DarkOnGoldContainer,
    tertiary = DarkTealTertiary,
    onTertiary = Color(0xFF00373A),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = Color(0xFF22322B),
    outlineVariant = Color(0xFF1A2822),
    error = DarkSemanticError,
    onError = Color(0xFF330A0A),
    errorContainer = DarkSemanticErrorContainer,
    onErrorContainer = DarkSemanticError
)

val MasjidShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(11.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MasjidSocietyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = MasjidShapes,
        content = content
    )
}
