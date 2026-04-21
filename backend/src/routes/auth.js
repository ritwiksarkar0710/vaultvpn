// ============================================================
// routes/auth.js — Secure Authentication Endpoints (2026)
// ============================================================
const router = require('express').Router();
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { v4: uuidv4 } = require('uuid');
const { body, validationResult } = require('express-validator');
const { User, RefreshToken } = require('../models');
const { authenticate } = require('../middleware/auth');

const ACCESS_TOKEN_TTL = '15m';
const REFRESH_TOKEN_TTL = 30 * 24 * 60 * 60 * 1000; // 30 Days

// ── HELPERS ──────────────────────────────────────────────────────────────────

const generateTokenResponse = async (user) => {
  const accessToken = jwt.sign(
    { sub: user._id, email: user.email, plan: user.plan },
    process.env.JWT_SECRET || 'vaultvpn_secret_2026',
    { expiresIn: ACCESS_TOKEN_TTL }
  );

  const refreshToken = uuidv4();
  await RefreshToken.create({
    userId: user._id,
    token: refreshToken,
    expiresAt: new Date(Date.now() + REFRESH_TOKEN_TTL)
  });

  return { accessToken, refreshToken };
};

// ── REGISTER ─────────────────────────────────────────────────────────────────
router.post('/register', [
  body('username').trim().isLength({ min: 3, max: 30 }).isAlphanumeric(),
  body('email').isEmail().normalizeEmail(),
  body('password').isLength({ min: 8 })
], async (req, res) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.status(400).json({ success: false, errors: errors.array() });
  }

  try {
    const { username, email, password } = req.body;

    const existing = await User.findOne({ $or: [{ email }, { username }] });
    if (existing) {
      return res.status(409).json({ success: false, message: 'Username or Email already taken' });
    }

    // passwordHash mapping - Note: Pre-save hook in models/index.js hashes this!
    const user = await User.create({ username, email, passwordHash: password });

    const tokens = await generateTokenResponse(user);
    res.status(201).json({ success: true, ...tokens });
  } catch (err) {
    console.error('Registration Error:', err);
    res.status(500).json({ success: false, message: 'Server error during registration' });
  }
});

// ── LOGIN ────────────────────────────────────────────────────────────────────
router.post('/login', [
  body('email').isEmail().normalizeEmail(),
  body('password').notEmpty()
], async (req, res) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) return res.status(400).json({ success: false, message: 'Invalid input' });

  try {
    const { email, password } = req.body;
    const user = await User.findOne({ email });

    if (!user || !user.isActive) {
      return res.status(401).json({ success: false, message: 'Invalid credentials' });
    }

    const isMatch = await bcrypt.compare(password, user.passwordHash);
    if (!isMatch) return res.status(401).json({ success: false, message: 'Invalid credentials' });

    user.lastLoginAt = new Date();
    await user.save();

    const tokens = await generateTokenResponse(user);
    res.json({ success: true, ...tokens });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Login failed' });
  }
});

// ── REFRESH TOKEN ────────────────────────────────────────────────────────────
router.post('/refresh', async (req, res) => {
  const { refreshToken } = req.body;
  if (!refreshToken) return res.status(400).json({ success: false, message: 'Token required' });

  try {
    const record = await RefreshToken.findOne({ token: refreshToken, isRevoked: false });
    if (!record || record.expiresAt < new Date()) {
      return res.status(401).json({ success: false, message: 'Expired/Invalid refresh token' });
    }

    const user = await User.findById(record.userId);
    if (!user) return res.status(401).json({ success: false, message: 'User not found' });

    // Revoke old token (Rotation)
    record.isRevoked = true;
    await record.save();

    const tokens = await generateTokenResponse(user);
    res.json({ success: true, ...tokens });
  } catch (err) {
    res.status(500).json({ success: false, message: 'Refresh failed' });
  }
});

// ── LOGOUT ───────────────────────────────────────────────────────────────────
router.post('/logout', authenticate, async (req, res) => {
  const { refreshToken } = req.body;
  if (refreshToken) {
    await RefreshToken.findOneAndUpdate({ token: refreshToken }, { isRevoked: true });
  }
  res.json({ success: true, message: 'Logged out successfully' });
});

module.exports = router;
