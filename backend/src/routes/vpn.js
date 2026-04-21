// ============================================================
// routes/vpn.js — Server Listing & VPN Management (2026)
// ============================================================
const router = require('express').Router();
const crypto = require('crypto');
const { Server, Session, WgPeer, User } = require('../../index');
const { authenticate } = require('../middleware/auth');

const fallbackServers = [
    { _id: 'sg1', name: 'Singapore #1', country: 'Singapore', countryCode: 'SG', city: 'Singapore', ipAddress: '1.2.3.10', port: 51820, protocol: 'WIREGUARD', pingMs: 18, loadPercent: 32, isFeatured: true, supportsBridge: true, supportsCloudflare: true, tier: 'FREE', isOnline: true, region: 'ASIA' },
    { _id: 'us1', name: 'US East #1', country: 'United States', countryCode: 'US', city: 'New York', ipAddress: '1.2.3.20', port: 51820, protocol: 'WIREGUARD', pingMs: 89, loadPercent: 55, isFeatured: true, supportsBridge: true, supportsCloudflare: true, tier: 'FREE', isOnline: true, region: 'AMERICAS' },
    { _id: 'de1', name: 'Germany #1', country: 'Germany', countryCode: 'DE', city: 'Frankfurt', ipAddress: '1.2.3.30', port: 51820, protocol: 'WIREGUARD', pingMs: 45, loadPercent: 20, isFeatured: false, supportsBridge: true, supportsCloudflare: true, tier: 'FREE', isOnline: true, region: 'EUROPE' },
    { _id: 'jp1', name: 'Japan #1', country: 'Japan', countryCode: 'JP', city: 'Tokyo', ipAddress: '1.2.3.40', port: 51820, protocol: 'OPENVPN_UDP', pingMs: 24, loadPercent: 40, isFeatured: true, supportsBridge: true, supportsCloudflare: true, tier: 'FREE', isOnline: true, region: 'ASIA' },
    { _id: 'nl1', name: 'Netherlands #1', country: 'Netherlands', countryCode: 'NL', city: 'Amsterdam', ipAddress: '1.2.3.50', port: 443, protocol: 'OPENVPN_TCP', pingMs: 62, loadPercent: 70, isFeatured: false, supportsBridge: true, supportsCloudflare: true, tier: 'PRO', isOnline: true, region: 'EUROPE' },
    { _id: 'uk1', name: 'United Kingdom #1', country: 'United Kingdom', countryCode: 'GB', city: 'London', ipAddress: '1.2.3.60', port: 51820, protocol: 'WIREGUARD', pingMs: 55, loadPercent: 48, isFeatured: false, supportsBridge: true, supportsCloudflare: true, tier: 'PRO', isOnline: true, region: 'EUROPE' },
    { _id: 'ca1', name: 'Canada #1', country: 'Canada', countryCode: 'CA', city: 'Toronto', ipAddress: '1.2.3.70', port: 51820, protocol: 'WIREGUARD', pingMs: 78, loadPercent: 30, isFeatured: false, supportsBridge: true, supportsCloudflare: true, tier: 'FREE', isOnline: true, region: 'AMERICAS' },
    { _id: 'au1', name: 'Australia #1', country: 'Australia', countryCode: 'AU', city: 'Sydney', ipAddress: '1.2.3.80', port: 51820, protocol: 'WIREGUARD', pingMs: 130, loadPercent: 25, isFeatured: false, supportsBridge: true, supportsCloudflare: true, tier: 'FREE', isOnline: true, region: 'OCEANIA' },
    { _id: 'in1', name: 'India #1', country: 'India', countryCode: 'IN', city: 'Mumbai', ipAddress: '1.2.3.90', port: 51820, protocol: 'WIREGUARD', pingMs: 12, loadPercent: 45, isFeatured: false, supportsBridge: true, supportsCloudflare: true, tier: 'FREE', isOnline: true, region: 'ASIA' },
    { _id: 'fr1', name: 'France #1', country: 'France', countryCode: 'FR', city: 'Paris', ipAddress: '1.2.3.100', port: 51820, protocol: 'WIREGUARD', pingMs: 58, loadPercent: 35, isFeatured: false, supportsBridge: true, supportsCloudflare: true, tier: 'FREE', isOnline: true, region: 'EUROPE' },
    { _id: 'br1', name: 'Brazil #1', country: 'Brazil', countryCode: 'BR', city: 'Sao Paulo', ipAddress: '1.2.3.110', port: 51820, protocol: 'OPENVPN_UDP', pingMs: 220, loadPercent: 60, isFeatured: false, supportsBridge: true, supportsCloudflare: true, tier: 'FREE', isOnline: true, region: 'AMERICAS' },
    { _id: 'kr1', name: 'South Korea #1', country: 'South Korea', countryCode: 'KR', city: 'Seoul', ipAddress: '1.2.3.120', port: 51820, protocol: 'WIREGUARD', pingMs: 32, loadPercent: 50, isFeatured: false, supportsBridge: true, supportsCloudflare: true, tier: 'FREE', isOnline: true, region: 'ASIA' },
    { _id: 'se1', name: 'Sweden #1', country: 'Sweden', countryCode: 'SE', city: 'Stockholm', ipAddress: '1.2.3.130', port: 51820, protocol: 'WIREGUARD', pingMs: 52, loadPercent: 25, isFeatured: false, supportsBridge: true, supportsCloudflare: true, tier: 'FREE', isOnline: true, region: 'EUROPE' },
    { _id: 'ch1', name: 'Switzerland #1', country: 'Switzerland', countryCode: 'CH', city: 'Zurich', ipAddress: '1.2.3.140', port: 51820, protocol: 'WIREGUARD', pingMs: 48, loadPercent: 30, isFeatured: false, supportsBridge: true, supportsCloudflare: true, tier: 'PRO', isOnline: true, region: 'EUROPE' },
    { _id: 'za1', name: 'South Africa #1', country: 'South Africa', countryCode: 'ZA', city: 'Johannesburg', ipAddress: '1.2.3.150', port: 51820, protocol: 'WIREGUARD', pingMs: 165, loadPercent: 42, isFeatured: false, supportsBridge: true, supportsCloudflare: true, tier: 'FREE', isOnline: true, region: 'AFRICA' },
    { _id: 'ae1', name: 'UAE #1', country: 'United Arab Emirates', countryCode: 'AE', city: 'Dubai', ipAddress: '1.2.3.160', port: 51820, protocol: 'WIREGUARD', pingMs: 76, loadPercent: 38, isFeatured: false, supportsBridge: true, supportsCloudflare: true, tier: 'PRO', isOnline: true, region: 'ASIA' },
    { _id: 'mx1', name: 'Mexico #1', country: 'Mexico', countryCode: 'MX', city: 'Mexico City', ipAddress: '1.2.3.170', port: 51820, protocol: 'WIREGUARD', pingMs: 142, loadPercent: 44, isFeatured: false, supportsBridge: true, supportsCloudflare: true, tier: 'FREE', isOnline: true, region: 'AMERICAS' },
    { _id: 'no1', name: 'Norway #1', country: 'Norway', countryCode: 'NO', city: 'Oslo', ipAddress: '1.2.3.180', port: 51820, protocol: 'SHADOWSOCKS', pingMs: 68, loadPercent: 28, isFeatured: false, supportsBridge: true, supportsCloudflare: false, tier: 'FREE', isOnline: true, region: 'EUROPE' }
];

function filterServers(servers, { protocol, tier, region }) {
    return servers.filter(server => {
        if (protocol && server.protocol !== protocol.toUpperCase()) return false;
        if (tier && server.tier !== tier.toUpperCase()) return false;
        if (region && server.region !== region.toUpperCase()) return false;
        return server.isOnline !== false;
    });
}

// ── GET SERVERS ─────────────────────────────────────────────────────────────
router.get('/servers', authenticate, async (req, res) => {
    try {
        const { protocol, tier, region } = req.query;
        const filter = { isOnline: true };

        if (protocol) filter.protocol = protocol.toUpperCase();
        if (tier)     filter.tier     = tier.toUpperCase();
        if (region)   filter.region   = region.toUpperCase();

        let servers = await Server.find(filter)
            .select('-publicKey') // Security: Only send public keys during connection
            .sort({ isFeatured: -1, pingMs: 1 })
            .lean();

        if (!servers.length) {
            servers = filterServers(fallbackServers, { protocol, tier, region })
                .sort((a, b) => Number(b.isFeatured) - Number(a.isFeatured) || a.pingMs - b.pingMs);
        }

        // Recommend the best server for this user (Lowest load + Free tier)
        const recommended = servers.find(s => s.tier === 'FREE' && s.loadPercent < 70) || servers[0];

        res.json({
            success: true,
            data: {
                servers,
                recommended: recommended?._id || null
            }
        });
    } catch (err) {
        console.error('[GET /servers]', err);
        res.status(500).json({ success: false, message: 'Failed to load servers' });
    }
});

// ── GET BRIDGES (Stealth Mode Options) ──────────────────────────────────────
router.get('/bridges', authenticate, async (req, res) => {
    const bridges = [
        {
            type: 'CLOUDFLARE_WARP',
            address: '162.159.193.1',
            port: 2408,
            description: 'Routes through Cloudflare global Anycast'
        },
        {
            type: 'OBFS4',
            address: process.env.OBFS4_HOST || 'bridge.vaultvpn.com',
            port: 443,
            description: 'Obfuscated traffic — best for restrictive firewalls'
        },
        {
            type: 'SNOWFLAKE',
            address: 'snowflake.torproject.net',
            port: 443,
            description: 'Tor-based P2P bridge'
        }
    ];
    res.json({ success: true, data: bridges });
});

// ── CONNECT ─────────────────────────────────────────────────────────────────
router.post('/connect', authenticate, async (req, res) => {
    const { serverId, protocol, bridgeType = 'NONE' } = req.body;

    try {
        const server = await Server.findById(serverId);
        if (!server || !server.isOnline) {
            return res.status(404).json({ success: false, message: 'Server is offline' });
        }

        const user = await User.findById(req.user._id);

        // Plan Guard
        if (server.tier !== 'FREE' && user.plan === 'FREE') {
            return res.status(403).json({ success: false, message: `${server.tier} requires upgrade` });
        }

        // Clean up old active sessions
        await Session.updateMany(
            { userId: user._id, isActive: true },
            { isActive: false, disconnectedAt: new Date() }
        );

        let wgConfig = null;
        if (protocol === 'WIREGUARD' || !protocol) {
            wgConfig = await provisionWireGuardPeer(user._id, server);
        }

        const session = await Session.create({
            userId: user._id,
            serverId: server._id,
            protocol: protocol || server.protocol,
            bridgeType,
            vpnIp: wgConfig?.clientIp || null,
            isActive: true
        });

        // Track load
        await Server.findByIdAndUpdate(serverId, { $inc: { activeConnections: 1 } });

        res.json({
            success: true,
            data: {
                sessionId: session._id,
                serverEndpoint: `${server.ipAddress}:${server.port}`,
                wireguard: wgConfig,
                bridge: bridgeType
            }
        });
    } catch (err) {
        console.error('[POST /connect]', err);
        res.status(500).json({ success: false, message: 'Provisioning failed' });
    }
});

// ── DISCONNECT ──────────────────────────────────────────────────────────────
router.post('/disconnect', authenticate, async (req, res) => {
    try {
        const session = await Session.findOneAndUpdate(
            { userId: req.user._id, isActive: true },
            { isActive: false, disconnectedAt: new Date() },
            { new: true }
        );

        if (session) {
            await Server.findByIdAndUpdate(session.serverId, { $inc: { activeConnections: -1 } });
            // Logic for data usage tracking could be added here
        }

        res.json({ success: true, message: 'Disconnected' });
    } catch (err) {
        res.status(500).json({ success: false, message: 'Error during disconnect' });
    }
});

// ── HELPERS ──────────────────────────────────────────────────────────────────
async function provisionWireGuardPeer(userId, server) {
    const privateKey   = crypto.randomBytes(32).toString('base64');
    const publicKey    = crypto.randomBytes(32).toString('base64');
    const presharedKey = crypto.randomBytes(32).toString('base64');

    const count = await WgPeer.countDocuments({ serverId: server._id });
    const assignedIp = `10.8.0.${(count % 250) + 2}/32`;

    await WgPeer.create({
        userId,
        serverId: server._id,
        privateKey,
        publicKey,
        presharedKey,
        assignedIp,
        isActive: true
    });

    return {
        privateKey,
        publicKey,
        serverPublicKey: server.publicKey || 'PLACEHOLDER',
        assignedIp,
        dns: '1.1.1.1, 8.8.8.8'
    };
}

module.exports = router;
