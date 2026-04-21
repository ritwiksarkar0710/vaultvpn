package com.vaultvpn.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.vaultvpn.data.model.*
import com.vaultvpn.ui.theme.*
import com.vaultvpn.ui.viewmodel.VpnViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(viewModel: VpnViewModel, onBack: () -> Unit) {
    val servers        by viewModel.servers.collectAsState()
    val selectedServer by viewModel.selectedServer.collectAsState()
    var query          by remember { mutableStateOf("") }
    var filterProtocol by remember { mutableStateOf<VpnProtocol?>(null) }

    val filtered = remember(servers, query, filterProtocol) {
        servers.filter { s ->
            (query.isBlank() || s.name.contains(query, true) ||
             s.country.contains(query, true) || s.city.contains(query, true)) &&
            (filterProtocol == null || s.protocol == filterProtocol)
        }
    }

    Column(Modifier.fillMaxSize().background(VaultBlack).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = VaultGray) }
            Spacer(Modifier.width(8.dp))
            Text("SELECT SERVER", style = MaterialTheme.typography.headlineMedium, color = VaultWhite, letterSpacing = 2.sp)
        }

        Surface(Modifier.fillMaxWidth().padding(horizontal = 20.dp), RoundedCornerShape(14.dp),
            VaultCardBg, border = BorderStroke(1.dp, VaultBorder)) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Search, "Search", tint = VaultGray, modifier = Modifier.size(20.dp))
                BasicTextField(value = query, onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = VaultWhite),
                    cursorBrush = SolidColor(VaultCyan), singleLine = true,
                    decorationBox = { inner ->
                        if (query.isEmpty()) Text("Search countries, cities…",
                            style = MaterialTheme.typography.bodyLarge, color = VaultGray)
                        inner()
                    })
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val protocols = listOf(VpnProtocol.WIREGUARD to "WireGuard", VpnProtocol.OPENVPN_UDP to "OpenVPN",
                VpnProtocol.TOR to "Tor", VpnProtocol.SHADOWSOCKS to "Shadowsocks")
            items(protocols) { (proto, label) ->
                FilterChip(selected = filterProtocol == proto,
                    onClick = { filterProtocol = if (filterProtocol == proto) null else proto },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VaultPurple.copy(0.2f),
                        selectedLabelColor = Color(0xFFCC88FF),
                        containerColor = Color.Transparent, labelColor = VaultGray))
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val featured = filtered.filter { it.isFeatured }
            val rest = filtered.filter { !it.isFeatured }

            if (featured.isNotEmpty()) {
                item { Text("⚡ RECOMMENDED", style = MaterialTheme.typography.labelSmall,
                    color = VaultGray, letterSpacing = 2.sp, modifier = Modifier.padding(vertical = 4.dp)) }
                items(featured, key = { it.id }) { server ->
                    ServerItem(server, server.id == selectedServer?.id) { viewModel.selectServer(server); onBack() }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
            if (rest.isNotEmpty()) {
                item { Text("ALL SERVERS · ${rest.size}", style = MaterialTheme.typography.labelSmall,
                    color = VaultGray, letterSpacing = 2.sp, modifier = Modifier.padding(vertical = 4.dp)) }
                items(rest, key = { it.id }) { server ->
                    ServerItem(server, server.id == selectedServer?.id) { viewModel.selectServer(server); onBack() }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerItem(server: VpnServer, selected: Boolean, onSelect: () -> Unit) {
    Surface(onClick = onSelect, shape = RoundedCornerShape(14.dp),
        color = if (selected) VaultCyan.copy(0.06f) else VaultCardBg,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) VaultCyan else VaultBorder),
        modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(server.countryCode.toFlagEmoji(), fontSize = 28.sp)
                Column {
                    Text("${server.city}, ${server.country}", style = MaterialTheme.typography.titleMedium,
                        color = if (selected) VaultCyan else VaultWhite,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(server.protocol.name, style = MaterialTheme.typography.labelSmall, color = VaultGray)
                        Text("·", color = VaultGray)
                        val pc = when { server.pingMs < 80 -> VaultGreen; server.pingMs < 150 -> VaultOrange; else -> VaultRed }
                        Text("${server.pingMs}ms", style = MaterialTheme.typography.labelSmall, color = pc)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (selected) Icon(Icons.Default.CheckCircle, null, tint = VaultCyan, modifier = Modifier.size(20.dp))
                val lc = when { server.loadPercent < 50 -> VaultGreen; server.loadPercent < 80 -> VaultOrange; else -> VaultRed }
                Text("${server.loadPercent}%", style = MaterialTheme.typography.labelSmall, color = lc)
            }
        }
    }
}

private fun String.toFlagEmoji(): String {
    if (length != 2) return "🌐"
    return String(intArrayOf(codePointAt(0) + 127397, codePointAt(1) + 127397), 0, 2)
}
