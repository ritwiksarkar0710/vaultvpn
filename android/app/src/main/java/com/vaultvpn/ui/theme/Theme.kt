package com.vaultvpn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

// ─── VaultVPN Color Palette ─────────────────────────────────────────────────
val VaultBlack       = Color(0xFF080C14)
val VaultDeepNavy    = Color(0xFF0D1525)
val VaultNavy        = Color(0xFF111E36)
val VaultCardBg      = Color(0xFF16213E)
val VaultBorder      = Color(0xFF1E3056)
val VaultCyan        = Color(0xFF00D4FF)
val VaultCyanDim     = Color(0xFF0099BB)
val VaultGreen       = Color(0xFF00FF87)
val VaultGreenDim    = Color(0xFF00C96A)
val VaultRed         = Color(0xFFFF4D6D)
val VaultOrange      = Color(0xFFFF8C42)
val VaultPurple      = Color(0xFF7B2FBE)
val VaultGray        = Color(0xFF8899BB)
val VaultGrayLight   = Color(0xFFAABBCC)
val VaultWhite       = Color(0xFFEEF4FF)

// Connected / Disconnected / Connecting
val StatusConnected    = VaultGreen
val StatusConnecting   = VaultOrange
val StatusDisconnected = VaultRed
val StatusIdle         = VaultGray

private val DarkColorScheme = darkColorScheme(
    primary          = VaultCyan,
    onPrimary        = VaultBlack,
    primaryContainer = VaultNavy,
    secondary        = VaultGreen,
    onSecondary      = VaultBlack,
    tertiary         = VaultPurple,
    background       = VaultBlack,
    onBackground     = VaultWhite,
    surface          = VaultCardBg,
    onSurface        = VaultWhite,
    surfaceVariant   = VaultNavy,
    outline          = VaultBorder,
    error            = VaultRed,
)

@Composable
fun VaultVPNTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = VaultTypography,
        content     = content
    )
}
