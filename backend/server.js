const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
require('dotenv').config();

const { db } = require('./config/firebase');

const authRoutes = require('./routes/authRoutes');
const complaintRoutes = require('./routes/complaintRoutes');
const newsRoutes = require('./routes/newsRoutes');
const bannerRoutes = require('./routes/bannerRoutes');

const app = express();
const PORT = process.env.PORT || 5000;

// Security Middlewares
app.use(helmet()); // Sets secure HTTP headers
app.use(cors({
  origin: '*', // In production, restrict this to your mobile application's origin/IP
  methods: ['GET', 'POST', 'PUT', 'DELETE'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));

app.use(express.json({ limit: '15mb' })); // Body parser for JSON payloads, supporting high-resolution image uploads
app.use(express.urlencoded({ limit: '15mb', extended: true }));

// Mount Routes
app.use('/api/auth', authRoutes);
app.use('/api/complaints', complaintRoutes);
app.use('/api/news', newsRoutes);
app.use('/api/banners', bannerRoutes);

// Serve Admin Web Portal
const path = require('path');
app.use('/admin', express.static(path.join(__dirname, 'admin')));

// Basic Health Check Route
app.get('/api/health', async (req, res) => {
  try {
    if (!db) {
      throw new Error('Firestore connection is not initialized. Please verify your serviceAccountKey.json is placed inside config/');
    }
    
    // Verify cloud connection by querying the users collection with limit 1
    await db.collection('users').limit(1).get();
    
    return res.status(200).json({
      status: 'healthy',
      timestamp: new Date(),
      database: 'connected',
      type: 'Firestore Cloud'
    });
  } catch (err) {
    return res.status(500).json({
      status: 'unhealthy',
      timestamp: new Date(),
      database: 'error',
      message: err.message
    });
  }
});

// Centralized Error Handling Middleware
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({
    error: 'Internal Server Error',
    message: process.env.NODE_ENV === 'development' ? err.message : undefined
  });
});

// Start the server
app.listen(PORT, () => {
  console.log(`Secure CRM server running on port ${PORT} in ${process.env.NODE_ENV || 'production'} mode.`);
  if (!db) {
    console.log('NOTICE: Server is running, but Firestore database is not connected yet. Follow the serviceAccountKey.json instructions in console.');
  }
});
