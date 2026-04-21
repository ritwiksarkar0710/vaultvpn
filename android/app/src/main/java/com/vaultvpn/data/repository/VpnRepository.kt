package com.vaultvpn.data.repository

import com.vaultvpn.data.model.*
import com.vaultvpn.network.VpnApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnRepository @Inject constructor(
    private val api: VpnApiService
) {
    suspend fun getServers(): Result<List<VpnServer>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.getServers()
            val servers = response.data?.servers.orEmpty()
            if (response.success && servers.isNotEmpty()) {
                servers
            } else {
                fallbackServers()
            }
        }.recoverCatching { error ->
            Timber.w(error, "Falling back to bundled server inventory")
            fallbackServers()
        }
    }

    suspend fun getUser(): Result<User> = withContext(Dispatchers.IO) {
        runCatching { demoUser() }
    }

    private fun fallbackServers() = listOf(
        VpnServer("sg1", "Singapore #1",       "Singapore",      "SG", "Singapore",   "1.2.3.10",  51820, VpnProtocol.WIREGUARD,   18, 32, true,  true,  true,  ServerTier.FREE),
        VpnServer("us1", "US East #1",         "United States",  "US", "New York",    "1.2.3.20",  51820, VpnProtocol.WIREGUARD,   89, 55, true,  true,  true,  ServerTier.FREE),
        VpnServer("de1", "Germany #1",         "Germany",        "DE", "Frankfurt",   "1.2.3.30",  51820, VpnProtocol.WIREGUARD,   45, 20, false, true,  true,  ServerTier.FREE),
        VpnServer("jp1", "Japan #1",           "Japan",          "JP", "Tokyo",       "1.2.3.40",  51820, VpnProtocol.OPENVPN_UDP, 24, 40, true,  true,  true,  ServerTier.FREE),
        VpnServer("nl1", "Netherlands #1",     "Netherlands",    "NL", "Amsterdam",   "1.2.3.50",  443,   VpnProtocol.OPENVPN_TCP, 62, 70, false, true,  true,  ServerTier.PRO),
        VpnServer("uk1", "United Kingdom #1",  "United Kingdom", "GB", "London",      "1.2.3.60",  51820, VpnProtocol.WIREGUARD,   55, 48, false, true,  true,  ServerTier.PRO),
        VpnServer("ca1", "Canada #1",          "Canada",         "CA", "Toronto",     "1.2.3.70",  51820, VpnProtocol.WIREGUARD,   78, 30, false, true,  true,  ServerTier.FREE),
        VpnServer("au1", "Australia #1",       "Australia",      "AU", "Sydney",      "1.2.3.80",  51820, VpnProtocol.WIREGUARD,   130, 25, false, true,  true,  ServerTier.FREE),
        VpnServer("in1", "India #1",           "India",          "IN", "Mumbai",      "1.2.3.90",  51820, VpnProtocol.WIREGUARD,   12, 45, false, true,  true,  ServerTier.FREE),
        VpnServer("fr1", "France #1",          "France",         "FR", "Paris",       "1.2.3.100", 51820, VpnProtocol.WIREGUARD,   58, 35, false, true,  true,  ServerTier.FREE),
        VpnServer("br1", "Brazil #1",          "Brazil",         "BR", "Sao Paulo",   "1.2.3.110", 51820, VpnProtocol.OPENVPN_UDP, 220, 60, false, true,  true,  ServerTier.FREE),
        VpnServer("kr1", "South Korea #1",     "South Korea",    "KR", "Seoul",       "1.2.3.120", 51820, VpnProtocol.WIREGUARD,   32, 50, false, true,  true,  ServerTier.FREE),
        VpnServer("se1", "Sweden #1",          "Sweden",         "SE", "Stockholm",   "1.2.3.130", 51820, VpnProtocol.WIREGUARD,   52, 25, false, true,  true,  ServerTier.FREE),
        VpnServer("ch1", "Switzerland #1",     "Switzerland",    "CH", "Zurich",      "1.2.3.140", 51820, VpnProtocol.WIREGUARD,   48, 30, false, true,  true,  ServerTier.PRO),
        VpnServer("za1", "South Africa #1",    "South Africa",   "ZA", "Johannesburg","1.2.3.150", 51820, VpnProtocol.WIREGUARD,   165, 42, false, true,  true,  ServerTier.FREE),
        VpnServer("ae1", "UAE #1",             "United Arab Emirates", "AE", "Dubai", "1.2.3.160", 51820, VpnProtocol.WIREGUARD,   76, 38, false, true,  true,  ServerTier.PRO),
        VpnServer("mx1", "Mexico #1",          "Mexico",         "MX", "Mexico City", "1.2.3.170", 51820, VpnProtocol.WIREGUARD,   142, 44, false, true,  true,  ServerTier.FREE),
        VpnServer("no1", "Norway #1",          "Norway",         "NO", "Oslo",        "1.2.3.180", 51820, VpnProtocol.SHADOWSOCKS, 68,  28, false, true,  false, ServerTier.FREE),
    )

    private fun demoUser() = User(
        id = "demo", email = "user@vaultvpn.com", username = "vault_user",
        plan = ServerTier.FREE, expiresAt = null,
        dataUsedBytes = 512 * 1024 * 1024L,
        dataLimitBytes = 10L * 1024 * 1024 * 1024
    )
}
