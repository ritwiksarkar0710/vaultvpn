package com.vaultvpn.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultvpn.ui.theme.*
import com.vaultvpn.ui.viewmodel.VpnViewModel

@Composable
fun SettingsScreen(
    viewModel: VpnViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    var killSwitch       by remember { mutableStateOf(true) }
    var autoConnect      by remember { mutableStateOf(false) }
    var dnsLeak          by remember { mutableStateOf(true) }
    var ipv6Block        by remember { mutableStateOf(true) }
    var splitTunneling   by remember { mutableStateOf(false) }
    var customDns        by remember { mutableStateOf("1.1.1.1") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = VaultGray)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "SETTINGS",
                style = MaterialTheme.typography.headlineMedium,
                color = VaultWhite,
                letterSpacing = 2.sp
            )
        }

        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Security ──────────────────────────────────────────────────
            SectionTitle("SECURITY")

            ToggleSetting(
                icon    = Icons.Default.Shield,
                title   = "Kill Switch",
                subtitle = "Block all traffic if VPN drops",
                checked  = killSwitch,
                onToggle = { killSwitch = it }
            )
            ToggleSetting(
                icon    = Icons.Default.WifiOff,
                title   = "DNS Leak Protection",
                subtitle = "Force DNS through VPN tunnel",
                checked  = dnsLeak,
                onToggle = { dnsLeak = it }
            )
            ToggleSetting(
                icon    = Icons.Default.Block,
                title   = "Block IPv6",
                subtitle = "Prevent IPv6 leaks",
                checked  = ipv6Block,
                onToggle = { ipv6Block = it }
            )

            Spacer(Modifier.height(8.dp))
            // ── Connection ────────────────────────────────────────────────
            SectionTitle("CONNECTION")

            ToggleSetting(
                icon    = Icons.Default.PowerSettingsNew,
                title   = "Auto-Connect",
                subtitle = "Connect on untrusted Wi-Fi",
                checked  = autoConnect,
                onToggle = { autoConnect = it }
            )
            ToggleSetting(
                icon    = Icons.Default.CallSplit,
                title   = "Split Tunneling",
                subtitle = "Choose apps to bypass VPN",
                checked  = splitTunneling,
                onToggle = { splitTunneling = it }
            )

            Spacer(Modifier.height(8.dp))
            SectionTitle("DNS")
            DnsInput(value = customDns, onChange = { customDns = it })

            Spacer(Modifier.height(8.dp))
            SectionTitle("ABOUT")

            InfoRow("Version", "1.0.0")
            InfoRow("Protocol", "WireGuard / OpenVPN")
            InfoRow("Encryption", "AES-256 / ChaCha20")
            InfoRow("License", "Open Source (GPLv3)")

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelSmall,
        color = VaultGray,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun ToggleSetting(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = VaultCardBg,
        border = BorderStroke(1.dp, VaultBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(icon, null, tint = VaultCyan, modifier = Modifier.size(22.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = VaultWhite)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = VaultGray)
                }
            }
            Switch(
                checked  = checked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor  = VaultBlack,
                    checkedTrackColor  = VaultCyan,
                    uncheckedThumbColor = VaultGray,
                    uncheckedTrackColor = VaultNavy
                )
            )
        }
    }
}

@Composable
private fun DnsInput(value: String, onChange: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = VaultCardBg,
        border = BorderStroke(1.dp, VaultBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(Icons.Default.Dns, null, tint = VaultCyan, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
                Text("Custom DNS", style = MaterialTheme.typography.titleMedium, color = VaultWhite)
                OutlinedTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.labelLarge.copy(color = VaultCyan),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = VaultCyan,
                        unfocusedBorderColor = VaultBorder,
                        cursorColor          = VaultCyan
                    )
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = VaultCardBg,
        border = BorderStroke(1.dp, VaultBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = VaultGray)
            Text(value, style = MaterialTheme.typography.labelLarge, color = VaultWhite)
        }
    }
}
