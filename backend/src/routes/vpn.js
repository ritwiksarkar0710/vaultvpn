// ============================================================
// routes/vpn.js — Server Listing & VPN Management (2026)
// ============================================================
const router = require('express').Router();
const crypto = require('crypto');
const { Server, Session, WgPeer, User } = require('../models');
const { authenticate } = require('../middleware/auth');

// ── GET SERVERS ─────────────────────────────────────────────────────────────
router.get('/servers', authenticate, async (req, res) => {
    try {
        const { protocol, tier, region } = req.query;
        const filter = { isOnline: true };

        if (protocol) filter.protocol = protocol.toUpperCase();
        if (tier)     filter.tier     = tier.toUpperCase();
        if (region)   filter.region   = region.toUpperCase();

        const servers = await Server.find(filter)
            .select('-publicKey') // Security: Only send public keys during connection
            .sort({ isFeatured: -1, pingMs: 1 })
            .lean();

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
