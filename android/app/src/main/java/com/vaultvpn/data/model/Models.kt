package com.vaultvpn.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// ─── VPN Server ─────────────────────────────────────────────────────────────
@Parcelize
data class VpnServer(
    val id: String,
    val name: String,
    val country: String,
    val countryCode: String,      // ISO-2 e.g. "DE"
    val city: String,
    val ipAddress: String,
    val port: Int,
    val protocol: VpnProtocol,
    val pingMs: Int,
    val loadPercent: Int,         // 0-100
    val isFeatured: Boolean = false,
    val supportsBridge: Boolean = true,
    val supportsCloudflare: Boolean = true,
    val tier: ServerTier = ServerTier.FREE
) : Parcelable

enum class VpnProtocol { WIREGUARD, OPENVPN_UDP, OPENVPN_TCP, TOR, SHADOWSOCKS }
enum class ServerTier   { FREE, PRO, ULTRA }

// ─── Connection State ────────────────────────────────────────────────────────
sealed class VpnState {
    object Idle         : VpnState()
    object Connecting   : VpnState()
    object Connected    : VpnState()
    object Disconnecting: VpnState()
    data class Error(val message: String) : VpnState()
}

// ─── Bridge Config ───────────────────────────────────────────────────────────
data class BridgeConfig(
    val type: BridgeType,
    val address: String,
    val port: Int,
    val fingerprint: String? = null,
    val cert: String? = null
)

enum class BridgeType {
    NONE,
    OBFS4,         // Obfuscated - hard to detect/block
    MEEK_AZURE,    // Meek over Azure CDN
    MEEK_CLOUDFRONT,
    SNOWFLAKE,     // Peer-to-peer WebRTC based
    CLOUDFLARE_WARP // Cloudflare WARP tunnel
}

// ─── Session Stats ───────────────────────────────────────────────────────────
data class VpnSessionStats(
    val uploadBytes: Long   = 0L,
    val downloadBytes: Long = 0L,
    val sessionDurationSec: Long = 0L,
    val currentIp: String   = "",
    val originalIp: String  = "",
    val connectedServer: VpnServer? = null
)

// ─── User / Auth ─────────────────────────────────────────────────────────────
data class User(
    val id: String,
    val email: String,
    val username: String,
    val plan: ServerTier,
    val expiresAt: Long?,
    val dataUsedBytes: Long,
    val dataLimitBytes: Long
)

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String
)

// ─── API Responses ───────────────────────────────────────────────────────────
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?
)

data class ServersResponse(
    val servers: List<VpnServer>,
    val recommended: String
)

data class WireguardConfig(
    val privateKey: String,
    val publicKey: String,
    val serverPublicKey: String,
    val presharedKey: String,
    val allowedIPs: String,
    val dns: String,
    val endpoint: String
)
