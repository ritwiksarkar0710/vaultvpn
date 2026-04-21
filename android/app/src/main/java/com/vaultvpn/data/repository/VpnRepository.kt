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
            // Try API first, fallback to hardcoded servers for demo
            try {
                api.getServers().data?.servers ?: fallbackServers()
            } catch (e: Exception) {
                Timber.w(e, "API unavailable, using fallback servers")
                fallbackServers()
            }
        }
    }

    suspend fun getUser(): Result<User> = withContext(Dispatchers.IO) {
        runCatching {
            api.getUser().data ?: demoUser()
        }
    }

    private fun fallbackServers() = listOf(
        VpnServer("sg1", "Singapore #1",    "Singapore",    "SG", "Singapore",  "1.2.3.10",  51820, VpnProtocol.WIREGUARD,   18, 32, true,  true, true, ServerTier.FREE),
        VpnServer("us1", "US East #1",      "United States","US", "New York",   "1.2.3.20",  51820, VpnProtocol.WIREGUARD,   89, 55, true,  true, true, ServerTier.FREE),
        VpnServer("de1", "Germany #1",      "Germany",      "DE", "Frankfurt",  "1.2.3.30",  51820, VpnProtocol.WIREGUARD,   45, 20, false, true, true, ServerTier.FREE),
        VpnServer("jp1", "Japan #1",        "Japan",        "JP", "Tokyo",      "1.2.3.40",  51820, VpnProtocol.OPENVPN_UDP, 24, 40, true,  true, true, ServerTier.FREE),
        VpnServer("nl1", "Netherlands #1",  "Netherlands",  "NL", "Amsterdam",  "1.2.3.50",  443,   VpnProtocol.OPENVPN_TCP, 62, 70, false, true, true, ServerTier.PRO),
        VpnServer("uk1", "UK #1",           "United Kingdom","GB","London",     "1.2.3.60",  51820, VpnProtocol.WIREGUARD,   55, 48, false, true, true, ServerTier.PRO),
        VpnServer("tor1","Tor Exit (US)",    "United States","US","Anonymized", "127.0.0.1", 9050,  VpnProtocol.TOR,         950, 80, false, true, false, ServerTier.FREE),
        VpnServer("ca1", "Canada #1",        "Canada",       "CA","Toronto",    "1.2.3.80",  51820, VpnProtocol.WIREGUARD,   78, 30, false, true, true, ServerTier.FREE),
        VpnServer("au1", "Australia #1",     "Australia",    "AU","Sydney",     "1.2.3.90",  51820, VpnProtocol.WIREGUARD,   130, 25, false, true, true, ServerTier.PRO),
        VpnServer("in1", "India #1",         "India",        "IN","Mumbai",     "1.2.3.100", 51820, VpnProtocol.SHADOWSOCKS, 12, 45, false, true, true, ServerTier.FREE),
        VpnServer("fr1", "France #1",        "France",       "FR","Paris",      "1.2.3.110", 51820, VpnProtocol.WIREGUARD,   58, 35, false, true, true, ServerTier.PRO),
        VpnServer("br1", "Brazil #1",        "Brazil",       "BR","São Paulo",  "1.2.3.120", 51820, VpnProtocol.OPENVPN_UDP, 220, 60, false, true, true, ServerTier.FREE),
    )

    private fun demoUser() = User(
        id = "demo",
        email = "user@vaultvpn.com",
        username = "vault_user",
        plan = ServerTier.FREE,
        expiresAt = null,
        dataUsedBytes = 512 * 1024 * 1024L,
        dataLimitBytes = 10L * 1024 * 1024 * 1024
    )
}
