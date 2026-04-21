package com.vaultvpn

import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.*
import com.vaultvpn.ui.screens.*
import com.vaultvpn.ui.theme.*
import com.vaultvpn.ui.viewmodel.VpnViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Permission result handled - user can now tap connect
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Pre-request VPN permission so it is ready when user taps connect
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            vpnPermissionLauncher.launch(vpnIntent)
        }

        setContent {
            VaultVPNTheme {
                VaultVpnNavHost()
            }
        }
    }
}

@Composable
fun VaultVpnNavHost() {
    val navController = rememberNavController()
    val activity = LocalContext.current as ComponentActivity
    val vpnViewModel: VpnViewModel = hiltViewModel(activity)
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = Modifier.fillMaxSize().background(VaultBlack),
        enterTransition = { slideInHorizontally(tween(280)) { it } + fadeIn(tween(280)) },
        exitTransition = { slideOutHorizontally(tween(280)) { -it } + fadeOut(tween(280)) },
        popEnterTransition = { slideInHorizontally(tween(280)) { -it } + fadeIn(tween(280)) },
        popExitTransition = { slideOutHorizontally(tween(280)) { it } + fadeOut(tween(280)) }
    ) {
        composable("home") {
            HomeScreen(
                viewModel = vpnViewModel,
                onNavigateToServers = { navController.navigate("servers") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("servers") {
            ServersScreen(
                viewModel = vpnViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
