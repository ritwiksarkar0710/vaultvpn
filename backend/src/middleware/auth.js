// ============================================================
// middleware/auth.js — JWT Verification Gatekeeper
// ============================================================
const jwt = require('jsonwebtoken');
const { User } = require('../models');

/**
 * authenticate: Middleware to protect private routes
 * It looks for a "Bearer <token>" in the Authorization header
 */
const authenticate = async (req, res, next) => {
    try {
        const authHeader = req.header('Authorization');
        
        if (!authHeader || !authHeader.startsWith('Bearer ')) {
            return res.status(401).json({ 
                success: false, 
                message: 'Access denied. No token provided.' 
            });
        }

        const token = authHeader.replace('Bearer ', '');
        
        // Verify the token using your secret key
        const decoded = jwt.verify(token, process.env.JWT_SECRET || 'vaultvpn_secret_2026');
        
        // Find the user and attach them to the "req" object so routes can use it
        const user = await User.findById(decoded.sub);
        
        if (!user || !user.isActive) {
            return res.status(401).json({ 
                success: false, 
                message: 'User is inactive or does not exist.' 
            });
        }

        req.user = user;
        req.token = token;
        next(); // Move to the actual route logic
    } catch (err) {
        res.status(401).json({ 
            success: false, 
            message: 'Invalid or expired token.' 
        });
    }
};

module.exports = { authenticate };
