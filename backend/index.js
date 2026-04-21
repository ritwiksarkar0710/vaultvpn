// ============================================================
// models/index.js — Optimized Mongoose models for VaultVPN (2026)
// ============================================================
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
const { Schema } = mongoose;

// ── USER SCHEMA ──────────────────────────────────────────────────────────────
const UserSchema = new Schema({
  username:      { type: String, required: true, unique: true, trim: true, minlength: 3, index: true },
  email:         { type: String, required: true, unique: true, lowercase: true, trim: true, index: true },
  passwordHash:  { type: String, required: true },
  plan:          { type: String, enum: ['FREE', 'PRO', 'ULTRA'], default: 'FREE' },
  planExpiresAt: { type: Date,   default: null },
  dataUsedBytes: { type: Number, default: 0 },
  dataLimitBytes: { type: Number, default: 10 * 1024 * 1024 * 1024 }, // 10 GB
  isActive:      { type: Boolean, default: true },
  lastLoginAt:   { type: Date },
}, { timestamps: true });

// Auto-hash password before saving
UserSchema.pre('save', async function(next) {
  if (!this.isModified('passwordHash')) return next();
  const salt = await bcrypt.genSalt(12);
  this.passwordHash = await bcrypt.hash(this.passwordHash, salt);
  next();
});

// Security: Hide passwordHash when converting to JSON (API responses)
UserSchema.set('toJSON', {
  transform: (doc, ret) => {
    delete ret.passwordHash;
    return ret;
  }
});

// ── SERVER SCHEMA ────────────────────────────────────────────────────────────
const ServerSchema = new Schema({
  name:               { type: String, required: true },
  country:            { type: String, required: true },
  countryCode:        { type: String, required: true, uppercase: true, minlength: 2, maxlength: 2 },
  city:               { type: String, required: true },
  ipAddress:          { type: String, required: true, unique: true, index: true },
  port:               { type: Number, required: true, default: 51820 },
  protocol:           { type: String, enum: ['WIREGUARD', 'OPENVPN_UDP', 'OPENVPN_TCP', 'TOR', 'SHADOWSOCKS'], default: 'WIREGUARD' },
  publicKey:          { type: String }, // WireGuard server public key
  pingMs:             { type: Number, default: 50 },
  loadPercent:        { type: Number, default: 0, min: 0, max: 100 },
  isFeatured:         { type: Boolean, default: false },
  supportsBridge:     { type: Boolean, default: true },
  supportsCloudflare: { type: Boolean, default: true },
  tier:               { type: String, enum: ['FREE', 'PRO', 'ULTRA'], default: 'FREE' },
  isOnline:           { type: Boolean, default: true },
  region:             { type: String, enum: ['ASIA', 'EUROPE', 'AMERICAS', 'OCEANIA', 'AFRICA'], required: true, index: true },
}, { timestamps: true });

// ── VPN SESSION SCHEMA ───────────────────────────────────────────────────────
const SessionSchema = new Schema({
  userId:          { type: Schema.Types.ObjectId, ref: 'User', required: true, index: true },
  serverId:        { type: Schema.Types.ObjectId, ref: 'Server', required: true },
  protocol:        { type: String, required: true },
  bridgeType:      { type: String, default: 'NONE' },
  connectedAt:     { type: Date, default: Date.now },
  disconnectedAt:  { type: Date, default: null },
  durationSec:     { type: Number, default: 0 },
  uploadBytes:     { type: Number, default: 0 },
  downloadBytes:   { type: Number, default: 0 },
  isActive:        { type: Boolean, default: true, index: true },
}, { timestamps: true });

// ── WIRE GUARD PEER SCHEMA ────────────────────────────────────────────────────
const WgPeerSchema = new Schema({
  userId:          { type: Schema.Types.ObjectId, ref: 'User', required: true, index: true },
  serverId:        { type: Schema.Types.ObjectId, ref: 'Server', required: true },
  privateKey:      { type: String, required: true }, // Should be encrypted in a real production environment
  publicKey:       { type: String, required: true },
  presharedKey:    { type: String, required: true },
  assignedIp:      { type: String, required: true }, 
  isActive:        { type: Boolean, default: true },
}, { timestamps: true });

// ── REFRESH TOKEN SCHEMA ──────────────────────────────────────────────────────
const RefreshTokenSchema = new Schema({
  userId:    { type: Schema.Types.ObjectId, ref: 'User', required: true },
  token:     { type: String, required: true, unique: true },
  expiresAt: { type: Date, required: true },
  isRevoked: { type: Boolean, default: false },
}, { timestamps: true });

// Auto-delete expired tokens from DB to save space
RefreshTokenSchema.index({ expiresAt: 1 }, { expireAfterSeconds: 0 });

// Exporting all models
module.exports = {
  User:         mongoose.model('User',         UserSchema),
  Server:       mongoose.model('Server',       ServerSchema),
  Session:      mongoose.model('Session',      SessionSchema),
  WgPeer:       mongoose.model('WgPeer',       WgPeerSchema),
  RefreshToken: mongoose.model('RefreshToken', RefreshTokenSchema),
};
