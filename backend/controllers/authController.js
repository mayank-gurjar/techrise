const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const { admin, db } = require('../config/firebase');

// User Registration
const register = async (req, res) => {
  try {
    const { email, password, role, mobile, name } = req.body;
    const normalizedEmail = email.toLowerCase().trim();

    // Restrict employee registration to main admin or coordinators
    if (role.toUpperCase() === 'ADMIN') {
      const authHeader = req.headers['authorization'];
      const token = authHeader && authHeader.split(' ')[1];
      if (!token) {
        return res.status(401).json({ error: 'Access Denied: Admin authorization required to register new employees.' });
      }
      try {
        const verified = jwt.verify(token, process.env.JWT_SECRET);
        if (verified.email.toLowerCase() !== 'admin@gmail.com' && !verified.isCoordinator) {
          return res.status(403).json({ error: 'Access Denied: Only administrators can register new employees.' });
        }
      } catch (err) {
        return res.status(403).json({ error: 'Access Denied: Invalid admin token.' });
      }
    }

    // 1. Check if user already exists
    const userQuery = await db.collection('users').where('email', '==', normalizedEmail).limit(1).get();
    if (!userQuery.empty) {
      return res.status(409).json({ error: 'A user with this email already exists.' });
    }

    // 2. Hash Password using bcrypt
    const saltRounds = 10;
    const passwordHash = await bcrypt.hash(password, saltRounds);

    // Generate clean short ID
    const rolePrefix = role.toUpperCase() === 'ADMIN' ? 'EMP' : 'CUST';
    const snapshot = await db.collection('users').where('role', '==', role.toUpperCase()).get();
    const count = snapshot.size;
    const cleanId = `${rolePrefix}-${1001 + count}`;

    // 3. Store new user in Firestore
    await db.collection('users').doc(cleanId).set({
      email: normalizedEmail,
      passwordHash,
      role: role.toUpperCase(),
      mobile: mobile ? mobile.trim() : '',
      name: name ? name.trim() : '',
      isCoordinator: req.body.isCoordinator || false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return res.status(201).json({
      message: 'User registered successfully.',
      userId: cleanId,
      email: normalizedEmail,
      role: role.toUpperCase()
    });

  } catch (err) {
    console.error('Registration Error:', err);
    return res.status(500).json({ error: 'Server error during registration.' });
  }
};

// User Login
const login = async (req, res) => {
  try {
    const { email, password } = req.body;
    const normalizedEmail = email.toLowerCase().trim();

    // 1. Fetch user from Firestore
    const userQuery = await db.collection('users').where('email', '==', normalizedEmail).limit(1).get();
    if (userQuery.empty) {
      // Security Tip: Generic error message to prevent email enumeration attacks
      return res.status(401).json({ error: 'Invalid email or password.' });
    }

    const userDoc = userQuery.docs[0];
    const userData = userDoc.data();

    // 2. Verify password
    const isMatch = await bcrypt.compare(password, userData.passwordHash);
    if (!isMatch) {
      return res.status(401).json({ error: 'Invalid email or password.' });
    }

    // 3. PC/Web Browser Login Restriction: Only employees (ADMIN role) can log in from desktop web browsers.
    const userAgent = req.headers['user-agent'] || '';
    const origin = req.headers['origin'] || '';
    const isBrowser = req.headers['sec-ch-ua'] || origin || (userAgent.includes('Mozilla') && !userAgent.includes('Android') && !userAgent.includes('iPhone') && !userAgent.includes('iPad'));

    if (isBrowser && userData.role !== 'ADMIN') {
      return res.status(403).json({ error: 'Access denied. Customers are only permitted to log in via the mobile application.' });
    }

    // 4. Generate Secure JWT Token and Session ID
    const crypto = require('crypto');
    const sessionId = crypto.randomBytes(16).toString('hex');

    // Store the active session ID in Firestore
    await db.collection('users').doc(userDoc.id).update({
      activeSessionId: sessionId,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    const payload = {
      userId: userDoc.id,
      email: userData.email,
      role: userData.role,
      sessionId: sessionId,
      isCoordinator: userData.isCoordinator || false
    };

    const token = jwt.sign(payload, process.env.JWT_SECRET, {
      expiresIn: process.env.JWT_EXPIRES_IN || '1h'
    });

    return res.status(200).json({
      message: 'Login successful.',
      token,
      user: {
        id: userDoc.id,
        email: userData.email,
        role: userData.role,
        isCoordinator: userData.isCoordinator || false
      }
    });

  } catch (err) {
    console.error('Login Error:', err);
    return res.status(500).json({ error: 'Server error during login.' });
  }
};

const getEmployees = async (req, res) => {
  try {
    const { email, isCoordinator } = req.user;
    if (email.toLowerCase() !== 'admin@gmail.com' && !isCoordinator) {
      return res.status(403).json({ error: 'Access Denied: Only administrators can view the employee list.' });
    }
    const snapshot = await db.collection('users').where('role', '==', 'ADMIN').get();
    const employees = [];
    snapshot.forEach(doc => {
      const udata = doc.data();
      const displayName = udata.name && udata.name.trim() ? udata.name : udata.email;
      employees.push({ id: doc.id, email: displayName });
    });
    return res.status(200).json(employees);
  } catch (err) {
    console.error('Get Employees Error:', err);
    return res.status(500).json({ error: 'Server error while fetching employees.' });
  }
};

const getProfile = async (req, res) => {
  try {
    const userId = req.user.userId;
    const userDoc = await db.collection('users').doc(userId).get();
    if (!userDoc.exists) {
      return res.status(404).json({ error: 'User profile not found.' });
    }
    const userData = userDoc.data();
    return res.status(200).json({
      id: userDoc.id,
      name: userData.name || '',
      email: userData.email,
      mobile: userData.mobile || '',
      role: userData.role
    });
  } catch (err) {
    console.error('Get Profile Error:', err);
    return res.status(500).json({ error: 'Server error while fetching profile.' });
  }
};

const updateProfile = async (req, res) => {
  try {
    const userId = req.user.userId;
    const { name, mobile } = req.body;
    
    const updateData = {};
    if (name !== undefined) updateData.name = name.trim();
    if (mobile !== undefined) updateData.mobile = mobile.trim();
    updateData.updatedAt = admin.firestore.FieldValue.serverTimestamp();
    
    await db.collection('users').doc(userId).update(updateData);
    
    return res.status(200).json({
      message: 'Profile updated successfully.'
    });
  } catch (err) {
    console.error('Update Profile Error:', err);
    return res.status(500).json({ error: 'Server error while updating profile.' });
  }
};

module.exports = {
  register,
  login,
  getEmployees,
  getProfile,
  updateProfile
};
