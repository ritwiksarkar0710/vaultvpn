package com.vaultvpn.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultvpn.data.model.*
import com.vaultvpn.ui.theme.*
import com.vaultvpn.ui.viewmodel.VpnViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: VpnViewModel = hiltViewModel(),
    onNavigateToServers: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val vpnState       by viewModel.vpnState.collectAsState()
    val stats          by viewModel.sessionStats.collectAsState()
    val selectedServer by viewModel.selectedServer.collectAsState()
    val bridgeType     by viewModel.bridgeType.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(VaultBlack)) {
        AnimatedGrid(vpnState)
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            TopBar(onSettings = onNavigateToSettings)
            Spacer(Modifier.height(32.dp))
            PulsingShieldButton(state = vpnState, onToggle = { viewModel.toggleConnection() })
            Spacer(Modifier.height(24.dp))
            StatusLabel(vpnState)
            Spacer(Modifier.height(8.dp))
            if (vpnState is VpnState.Connected) {
                IpBadge(stats.currentIp)
                Spacer(Modifier.height(24.dp))
                SpeedCard(stats)
            } else {
                Spacer(Modifier.height(24.dp))
            }
            ServerCard(server = selectedServer, onClick = onNavigateToServers)
            Spacer(Modifier.height(12.dp))
            BridgeSelector(current = bridgeType, onChange = { viewModel.setBridge(it) })
            Spacer(Modifier.height(12.dp))
            if (vpnState is VpnState.Connected) {
                StatsRow(stats)
                Spacer(Modifier.height(16.dp))
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AnimatedGrid(state: VpnState) {
    val infiniteTransition = rememberInfiniteTransition(label = "grid")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 40f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "offset"
    )
    val alpha = if (state is VpnState.Connected) 0.12f else 0.05f
    val gridColor = if (state is VpnState.Connected) VaultGreen else VaultCyan
    Canvas(modifier = Modifier.fillMaxSize()) {
        val color = gridColor.copy(alpha = alpha)
        var y = -offset
        while (y < size.height) { drawLine(color, Offset(0f, y), Offset(size.width, y), 0.5f); y += 40f }
        var x = -offset % 40f
        while (x < size.width) { drawLine(color, Offset(x, 0f), Offset(x, size.height), 0.5f); x += 40f }
    }
}

@Composable
private fun TopBar(onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Column {
            Text("VAULT", style = MaterialTheme.typography.headlineLarge, color = VaultCyan,
                fontWeight = FontWeight.Black, letterSpacing = 6.sp)
            Text("VPN", style = MaterialTheme.typography.labelLarge, color = VaultGray, letterSpacing = 8.sp)
        }
        IconButton(onClick = onSettings,
            modifier = Modifier.size(44.dp).border(1.dp, VaultBorder, CircleShape).background(VaultCardBg, CircleShape)) {
            Icon(Icons.Default.Settings, "Settings", tint = VaultGray)
        }
    }
}

@Composable
private fun PulsingShieldButton(state: VpnState, onToggle: () -> Unit) {
    val isConnected     = state is VpnState.Connected
    val isConnecting    = state is VpnState.Connecting
    val isDisconnecting = state is VpnState.Disconnecting
    val inf = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by inf.animateFloat(0f, 1f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "pr")
    val rotation by inf.animateFloat(0f, 360f,
        infiniteRepeatable(tween(if (isConnecting) 1200 else 6000, easing = LinearEasing)), label = "rot")
    val primaryColor = when {
        isConnected -> VaultGreen
        isConnecting || isDisconnecting -> VaultOrange
        else -> VaultCyan
    }
    Box(Modifier.size(220.dp), Alignment.Center) {
        if (isConnected) repeat(3) { i ->
            val p = ((pulseRadius + i * 0.33f) % 1f)
            Canvas(Modifier.size(220.dp)) {
                drawCircle(VaultGreen.copy(alpha = (1f - p) * 0.3f),
                    (size.minDimension / 2) * (0.5f + p * 0.5f), style = Stroke(2f))
            }
        }
        if (isConnecting || isDisconnecting)
            Canvas(Modifier.size(200.dp).rotate(rotation)) {
                drawArc(VaultOrange, 0f, 270f, false, style = Stroke(3f, cap = StrokeCap.Round))
            }
        Canvas(Modifier.size(180.dp)) {
            drawCircle(primaryColor.copy(0.15f), size.minDimension / 2)
            drawCircle(primaryColor.copy(0.6f), size.minDimension / 2, style = Stroke(1.5f))
        }
        Button(
            onClick = { if (!isConnecting && !isDisconnecting) onToggle() },
            modifier = Modifier.size(140.dp), shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isConnected) VaultGreen.copy(0.15f) else VaultCardBg,
                contentColor = primaryColor),
            border = BorderStroke(2.dp, primaryColor),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(if (isConnected) Icons.Default.Lock else Icons.Default.LockOpen,
                    null, Modifier.size(40.dp), tint = primaryColor)
                Spacer(Modifier.height(4.dp))
                Text(when (state) {
                    is VpnState.Connected -> "ON"; is VpnState.Connecting -> "…"
                    is VpnState.Disconnecting -> "…"; else -> "OFF"
                }, style = MaterialTheme.typography.labelLarge, color = primaryColor)
            }
        }
    }
}

@Composable
private fun StatusLabel(state: VpnState) {
    val (text, color) = when (state) {
        is VpnState.Connected     -> "PROTECTED"     to VaultGreen
        is VpnState.Connecting    -> "CONNECTING"    to VaultOrange
        is VpnState.Disconnecting -> "DISCONNECTING" to VaultOrange
        is VpnState.Error         -> "ERROR"         to VaultRed
        else                      -> "NOT PROTECTED" to VaultRed
    }
    Text(text, style = MaterialTheme.typography.labelLarge, color = color, letterSpacing = 4.sp)
}

@Composable
private fun IpBadge(ip: String) {
    Surface(shape = RoundedCornerShape(20.dp), color = VaultCardBg, border = BorderStroke(1.dp, VaultBorder)) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(8.dp).background(VaultGreen, CircleShape))
            Text(ip.ifBlank { "Hidden" }, style = MaterialTheme.typography.labelLarge, color = VaultGrayLight)
        }
    }
}

@Composable
private fun SpeedCard(stats: VpnSessionStats) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), VaultCardBg, border = BorderStroke(1.dp, VaultBorder)) {
        Row(Modifier.padding(20.dp), Arrangement.SpaceEvenly) {
            SpeedItem("↑ UPLOAD",   formatBytes(stats.uploadBytes),   VaultCyan)
            Box(Modifier.width(1.dp).height(40.dp).background(VaultBorder))
            SpeedItem("↓ DOWNLOAD", formatBytes(stats.downloadBytes), VaultGreen)
            Box(Modifier.width(1.dp).height(40.dp).background(VaultBorder))
            SpeedItem("TIME", formatDuration(stats.sessionDurationSec), VaultGray)
        }
    }
}

@Composable
private fun SpeedItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = VaultGray)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerCard(server: VpnServer?, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp), color = VaultCardBg, border = BorderStroke(1.dp, VaultBorder)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(server?.countryCode?.toFlagEmoji() ?: "🌐", fontSize = 32.sp)
                Column {
                    Text(server?.name ?: "Select Server",
                        style = MaterialTheme.typography.titleMedium, color = VaultWhite)
                    if (server != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            val pingColor = when { server.pingMs < 80 -> VaultGreen; server.pingMs < 150 -> VaultOrange; else -> VaultRed }
                            Text("${server.pingMs}ms", style = MaterialTheme.typography.labelSmall, color = pingColor)
                        }
                    } else {
                        Text("Tap to select a server", style = MaterialTheme.typography.bodyMedium, color = VaultGray)
                    }
                }
            }
            Icon(Icons.Default.ChevronRight, "Select", tint = VaultGray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BridgeSelector(current: BridgeType, onChange: (BridgeType) -> Unit) {
    val bridges = listOf(
        BridgeType.NONE to "No Bridge", BridgeType.CLOUDFLARE_WARP to "Cloudflare",
        BridgeType.OBFS4 to "OBFS4", BridgeType.SNOWFLAKE to "Snowflake"
    )
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), VaultCardBg, border = BorderStroke(1.dp, VaultBorder)) {
        Column(Modifier.padding(16.dp)) {
            Text("SECURITY BRIDGE", style = MaterialTheme.typography.labelSmall, color = VaultGray, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                bridges.forEach { (type, label) ->
                    FilterChip(
                        selected = current == type, onClick = { onChange(type) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VaultCyan.copy(0.15f),
                            selectedLabelColor = VaultCyan,
                            containerColor = Color.Transparent,
                            labelColor = VaultGray),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsRow(stats: VpnSessionStats) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatChip("PROTOCOL", stats.connectedServer?.protocol?.name ?: "—", Modifier.weight(1f))
        StatChip("ENCRYPTION", "AES-256", Modifier.weight(1f))
    }
}

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, RoundedCornerShape(12.dp), VaultCardBg, border = BorderStroke(1.dp, VaultBorder)) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = VaultGray)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.labelLarge, color = VaultCyan)
        }
    }
}

private fun String.toFlagEmoji(): String {
    if (length != 2) return "🌐"
    return String(intArrayOf(codePointAt(0) + 127397, codePointAt(1) + 127397), 0, 2)
}
private fun formatBytes(bytes: Long) = when {
    bytes < 1024 -> "${bytes}B"
    bytes < 1048576 -> "${"%.1f".format(bytes/1024.0)}KB"
    bytes < 1073741824 -> "${"%.1f".format(bytes/1048576.0)}MB"
    else -> "${"%.2f".format(bytes/1073741824.0)}GB"
}
private fun formatDuration(s: Long) = if (s/3600 > 0) "%02d:%02d:%02d".format(s/3600,(s%3600)/60,s%60)
    else "%02d:%02d".format((s%3600)/60, s%60)
