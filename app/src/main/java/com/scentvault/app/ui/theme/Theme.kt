package com.scentvault.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VaultColorScheme = darkColorScheme(
    primary = VaultGold,
    onPrimary = VaultOnGold,
    primaryContainer = VaultGoldContainer,
    onPrimaryContainer = VaultGold,
    secondary = VaultRose,
    onSecondary = VaultOnGold,
    background = VaultBackground,
    onBackground = VaultOnBackground,
    surface = VaultSurface,
    onSurface = VaultOnBackground,
    surfaceVariant = VaultSurfaceVariant,
    onSurfaceVariant = VaultOnSurfaceVariant,
    outline = VaultOutline,
    error = VaultError,
    onError = VaultOnError
)

/** ScentVault always renders in a dark, low-glare palette regardless of system theme. */
@Composable
fun ScentVaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VaultColorScheme,
        typography = VaultTypography,
        content = content
    )
}
