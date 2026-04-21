package com.vaultvpn.ui.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultvpn.data.model.*
import com.vaultvpn.data.repository.VpnRepository
import com.vaultvpn.service.VaultVpnService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class VpnViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: VpnRepository
) : ViewModel() {

    private val _vpnState = MutableStateFlow<VpnState>(VpnState.Idle)
    val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()

    private val _sessionStats = MutableStateFlow(VpnSessionStats())
    val sessionStats: StateFlow<VpnSessionStats> = _sessionStats.asStateFlow()

    private val _selectedServer = MutableStateFlow<VpnServer?>(null)
    val selectedServer: StateFlow<VpnServer?> = _selectedServer.asStateFlow()

    private val _servers = MutableStateFlow<List<VpnServer>>(emptyList())
    val servers: StateFlow<List<VpnServer>> = _servers.asStateFlow()

    private val _bridgeType = MutableStateFlow(BridgeType.NONE)
    val bridgeType: StateFlow<BridgeType> = _bridgeType.asStateFlow()

    private var vpnService: VaultVpnService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as VaultVpnService.VpnBinder
            vpnService = binder.getService()
            viewModelScope.launch {
                vpnService?.vpnState?.collect { _vpnState.value = it }
            }
            viewModelScope.launch {
                vpnService?.sessionStats?.collect { _sessionStats.value = it }
            }
        }
        override fun onServiceDisconnected(name: ComponentName) {
            vpnService = null
        }
    }

    init {
        bindService()
        loadServers()
    }

    private fun bindService() {
        try {
            val intent = Intent(context, VaultVpnService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Timber.e(e, "bindService failed")
        }
    }

    fun toggleConnection() {
        when (_vpnState.value) {
            is VpnState.Connected, is VpnState.Connecting -> disconnect()
            else -> connect()
        }
    }

    fun connect() {
        val server = _selectedServer.value ?: _servers.value.firstOrNull() ?: return
        val bridge = buildBridgeConfig(_bridgeType.value, server)
        try {
            val intent = Intent(context, VaultVpnService::class.java).also {
                it.action = VaultVpnService.ACTION_CONNECT
                it.putExtra(VaultVpnService.EXTRA_SERVER, server as android.os.Parcelable)
                if (bridge != null) it.putExtra(VaultVpnService.EXTRA_BRIDGE, bridge as android.os.Parcelable)
            }
            context.startForegroundService(intent)
        } catch (e: Exception) {
            Timber.e(e, "connect failed")
            _vpnState.value = VpnState.Error(e.message ?: "Failed to start VPN")
        }
    }

    fun disconnect() {
        try {
            val intent = Intent(context, VaultVpnService::class.java).also {
                it.action = VaultVpnService.ACTION_DISCONNECT
            }
            context.startService(intent)
        } catch (e: Exception) {
            Timber.e(e, "disconnect failed")
            _vpnState.value = VpnState.Idle
        }
    }

    fun selectServer(server: VpnServer) {
        val wasConnected = _vpnState.value is VpnState.Connected
        // Update selected server immediately so UI reflects it
        _selectedServer.value = server
        if (wasConnected) {
            // Reconnect to new server smoothly
            viewModelScope.launch {
                disconnect()
                delay(1200)
                connect()
            }
        }
        // If NOT connected, just update selection — do NOT disconnect/connect
    }

    fun setBridge(type: BridgeType) {
        _bridgeType.value = type
    }

    private fun loadServers() {
        viewModelScope.launch {
            repository.getServers()
                .onSuccess { list ->
                    _servers.value = list
                    if (_selectedServer.value == null) {
                        _selectedServer.value = list.firstOrNull { it.isFeatured }
                            ?: list.firstOrNull()
                    }
                }
                .onFailure { Timber.e(it, "Failed to load servers") }
        }
    }

    private fun buildBridgeConfig(type: BridgeType, server: VpnServer): BridgeConfig? = when (type) {
        BridgeType.NONE            -> null
        BridgeType.CLOUDFLARE_WARP -> BridgeConfig(type, "162.159.193.1", 2408)
        BridgeType.OBFS4           -> BridgeConfig(type, server.ipAddress, 443, "OBFS4_FP")
        BridgeType.SNOWFLAKE       -> BridgeConfig(type, "snowflake.torproject.net", 443)
        BridgeType.MEEK_AZURE      -> BridgeConfig(type, "meek.azureedge.net", 443)
        else                       -> null
    }

    override fun onCleared() {
        super.onCleared()
        try { context.unbindService(serviceConnection) } catch (_: Exception) {}
    }
}
