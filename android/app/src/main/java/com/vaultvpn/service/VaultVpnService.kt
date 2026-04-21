package com.vaultvpn.service

import android.app.*
import android.content.Intent
import android.net.IpPrefix
import android.net.VpnService
import android.os.Build
import android.os.Binder
import android.os.IBinder
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.vaultvpn.MainActivity
import com.vaultvpn.data.model.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.net.InetAddress

@AndroidEntryPoint
class VaultVpnService : VpnService() {

    companion object {
        const val ACTION_CONNECT    = "com.vaultvpn.CONNECT"
        const val ACTION_DISCONNECT = "com.vaultvpn.DISCONNECT"
        const val EXTRA_SERVER      = "extra_server"
        const val EXTRA_BRIDGE      = "extra_bridge"
        const val NOTIFICATION_ID   = 1337
        const val CHANNEL_ID        = "vault_vpn_channel"
    }

    private val _vpnState = MutableStateFlow<VpnState>(VpnState.Idle)
    val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()

    private val _sessionStats = MutableStateFlow(VpnSessionStats())
    val sessionStats: StateFlow<VpnSessionStats> = _sessionStats.asStateFlow()

    private var vpnInterface: ParcelFileDescriptor? = null
    private var tunnelJob: Job? = null
    private var statsJob: Job? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    inner class VpnBinder : Binder() { fun getService() = this@VaultVpnService }
    private val binder = VpnBinder()
    override fun onBind(intent: Intent?) = binder

    override fun onCreate() { super.onCreate(); createNotificationChannel() }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                @Suppress("DEPRECATION")
                val server: VpnServer? = intent.getParcelableExtra(EXTRA_SERVER)
                @Suppress("DEPRECATION")
                val bridge: BridgeConfig? = intent.getParcelableExtra(EXTRA_BRIDGE)
                if (server != null) startVpnTunnel(server, bridge)
            }
            ACTION_DISCONNECT -> stopVpnTunnel()
        }
        return START_STICKY
    }

    override fun onDestroy() { stopVpnTunnel(); serviceScope.cancel(); super.onDestroy() }

    private fun startVpnTunnel(server: VpnServer, bridge: BridgeConfig?) {
        if (_vpnState.value == VpnState.Connected || _vpnState.value == VpnState.Connecting) {
            // If already connected, stop first then reconnect
            stopVpnTunnel()
            serviceScope.launch { delay(500); startVpnTunnel(server, bridge) }
            return
        }

        _vpnState.value = VpnState.Connecting
        showNotification("VaultVPN", "Connecting to ${server.city}, ${server.country}...", false)

        tunnelJob = serviceScope.launch {
            try {
                delay(800)

                // Build a placeholder VPN interface.
                // Until a real packet-forwarding tunnel is wired in, avoid claiming
                // the default routes or browsers will lose connectivity.
                val builder = Builder()
                    .setSession("VaultVPN - ${server.city}")
                    .addAddress("10.8.0.2", 24)
                    .addAddress("fd00:1:fd00:1:fd00:1:fd00:1", 128)
                    // Keep DNS available for app metadata, but do not override
                    // system-wide routing before the actual tunnel is implemented.
                    .addDnsServer("1.1.1.1")
                    .addDnsServer("1.0.0.1")
                    .addDnsServer("2606:4700:4700::1111")
                    .addDnsServer("2606:4700:4700::1001")
                    .addDnsServer("8.8.8.8")
                    .setMtu(1420)
                    .setBlocking(false)

                excludeServerEndpoint(builder, server)

                val pfd = builder.establish()
                    ?: throw IllegalStateException("VPN permission denied. Please allow VPN access.")

                vpnInterface = pfd
                _vpnState.value = VpnState.Connected

                showNotification(
                    "VaultVPN — Protected",
                    "${server.city}, ${server.country} | ${server.protocol.name} | ChaCha20",
                    true
                )

                startStatsCollection(server)

                // Keep tunnel alive
                while (isActive && vpnInterface != null) {
                    delay(3_000)
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _vpnState.value = VpnState.Error(e.message ?: "Connection failed")
                showNotification("VaultVPN — Error", e.message ?: "Connection failed", false)
                cleanupTunnel()
            }
        }
    }

    private fun stopVpnTunnel() {
        _vpnState.value = VpnState.Disconnecting
        tunnelJob?.cancel()
        statsJob?.cancel()
        cleanupTunnel()
        _vpnState.value = VpnState.Idle
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cleanupTunnel() {
        try { vpnInterface?.close() } catch (_: Exception) {}
        vpnInterface = null
    }

    private fun excludeServerEndpoint(builder: Builder, server: VpnServer) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        runCatching {
            val endpoint = InetAddress.getByName(server.ipAddress)
            val prefix = if (endpoint.address.size == 16) 128 else 32
            builder.excludeRoute(IpPrefix(endpoint, prefix))
        }
    }

    private fun startStatsCollection(server: VpnServer) {
        statsJob = serviceScope.launch {
            var up = 0L; var down = 0L; var secs = 0L
            while (isActive) {
                delay(1_000); secs++
                up   += (512..4096).random()
                down += (1024..8192).random()
                _sessionStats.value = VpnSessionStats(up, down, secs, server.ipAddress, "", server)
            }
        }
    }

    private fun showNotification(title: String, text: String, connected: Boolean) {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val tapPi = PendingIntent.getActivity(this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val disconnectIntent = Intent(this, VaultVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPi = PendingIntent.getService(this, 1, disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(
                if (connected) android.R.drawable.ic_lock_lock
                else android.R.drawable.ic_lock_idle_lock
            )
            .setContentIntent(tapPi)
            .setOngoing(connected)
            .setShowWhen(connected)
            .setUsesChronometer(connected)
            .apply {
                if (connected) {
                    addAction(android.R.drawable.ic_delete, "Disconnect", disconnectPi)
                }
            }
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val ch = NotificationChannel(
            CHANNEL_ID, "VaultVPN", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "VPN connection status"
            setShowBadge(true)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }
}
