const { admin, db } = require('../config/firebase');

// Create a sliding banner (Admin only)
const createBanner = async (req, res) => {
  try {
    const { title, imageBase64, redirectUrl } = req.body;
    const authorId = req.user.userId;

    if (!imageBase64 || !imageBase64.trim()) {
      return res.status(400).json({ error: 'Banner image is required.' });
    }

    const bannerRef = await db.collection('banners').add({
      title: title ? title.trim() : '',
      imageBase64: imageBase64.trim(),
      redirectUrl: redirectUrl ? redirectUrl.trim() : '',
      authorId,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return res.status(201).json({
      message: 'Banner uploaded successfully.',
      id: bannerRef.id,
      title: title ? title.trim() : '',
      redirectUrl: redirectUrl ? redirectUrl.trim() : ''
    });

  } catch (err) {
    console.error('Create Banner Error:', err);
    return res.status(500).json({ error: 'Server error while uploading banner.' });
  }
};

// Get all sliding banners
const getBanners = async (req, res) => {
  try {
    const bannersSnapshot = await db.collection('banners').get();
    
    const bannersList = [];
    bannersSnapshot.forEach(doc => {
      const data = doc.data();
      bannersList.push({
        id: doc.id,
        title: data.title || '',
        imageBase64: data.imageBase64,
        redirectUrl: data.redirectUrl || '',
        authorId: data.authorId,
        createdAt: data.createdAt
      });
    });

    // Sort in memory (descending order by date)
    bannersList.sort((a, b) => {
      const secA = a.createdAt ? (a.createdAt._seconds || a.createdAt.seconds || 0) : 0;
      const secB = b.createdAt ? (b.createdAt._seconds || b.createdAt.seconds || 0) : 0;
      return secB - secA;
    });

    return res.status(200).json(bannersList);

  } catch (err) {
    console.error('Get Banners Error:', err);
    return res.status(500).json({ error: 'Server error while fetching banners.' });
  }
};

// Delete a sliding banner (Admin only)
const deleteBanner = async (req, res) => {
  try {
    const { id } = req.params;

    const bannerDoc = await db.collection('banners').doc(id).get();
    if (!bannerDoc.exists) {
      return res.status(404).json({ error: 'Banner not found.' });
    }

    await db.collection('banners').doc(id).delete();

    return res.status(200).json({ message: 'Banner deleted successfully.' });

  } catch (err) {
    console.error('Delete Banner Error:', err);
    return res.status(500).json({ error: 'Server error while deleting banner.' });
  }
};

module.exports = {
  createBanner,
  getBanners,
  deleteBanner
};
