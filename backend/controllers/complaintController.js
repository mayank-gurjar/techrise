const { admin, db } = require('../config/firebase');

// 1. Create a new complaint (Customers only)
const createComplaint = async (req, res) => {
  try {
    const { title, description, priority, imageBase64 } = req.body;
    const customerId = req.user.userId;

    if (!title || !title.trim() || !description || !description.trim()) {
      return res.status(400).json({ error: 'Title and description are required.' });
    }

    const validPriorities = ['LOW', 'MEDIUM', 'HIGH'];
    const selectedPriority = priority && validPriorities.includes(priority.toUpperCase()) 
      ? priority.toUpperCase() 
      : 'MEDIUM';

    // Generate clean short Complaint ID
    const snapshot = await db.collection('complaints').get();
    const count = snapshot.size;
    const cleanId = `TKT-${1001 + count}`;

    // Create complaint document
    const complaintData = {
      title: title.trim(),
      description: description.trim(),
      status: 'PENDING',
      priority: selectedPriority,
      customerId: customerId,
      assignedAdminId: null,
      imageBase64: imageBase64 || null,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    };

    await db.collection('complaints').doc(cleanId).set(complaintData);

    // Create audit log record
    await db.collection('complaintLogs').add({
      complaintId: cleanId,
      actionBy: customerId,
      oldStatus: 'NONE',
      newStatus: 'PENDING',
      comment: 'Complaint raised by customer.',
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    // Retrieve all employee mobile numbers and simulate sending notifications
    try {
      const employeeSnapshot = await db.collection('users').where('role', '==', 'ADMIN').get();
      const notifications = [];
      employeeSnapshot.forEach(doc => {
        const u = doc.data();
        if (u.mobile) {
          notifications.push({
            id: doc.id,
            email: u.email,
            mobile: u.mobile
          });
        }
      });
      
      console.log(`\n🔔 [NOTIFICATION SYSTEM] Triggered for Complaint ${cleanId} "${title.trim()}"`);
      notifications.forEach(emp => {
        console.log(`   📨 Sending SMS to Employee ${emp.id} (${emp.email}) at number ${emp.mobile}...`);
        console.log(`   💬 "Hi employee, a new customer complaint has been registered. Complaint ID: ${cleanId}, Title: ${title.trim()}."`);
      });
      console.log(`🔔 [NOTIFICATION SYSTEM] Successfully notified ${notifications.length} employee(s) via SMS simulation.\n`);
    } catch (notificationErr) {
      console.error('Failed to notify employees:', notificationErr);
    }

    return res.status(201).json({
      message: 'Complaint raised successfully.',
      complaintId: cleanId,
      ...complaintData
    });

  } catch (err) {
    console.error('Create Complaint Error:', err);
    return res.status(500).json({ error: 'Server error while creating complaint.' });
  }
};

// 2. Get complaints list (Admins see all, Customers see only theirs)
const getComplaints = async (req, res) => {
  try {
    const { role, userId, email } = req.user;
    let query;

    if (role.toUpperCase() === 'ADMIN') {
      if (email.toLowerCase() === 'admin@gmail.com') {
        // Admins see all complaints
        query = db.collection('complaints');
      } else {
        // Regular employees only see complaints assigned to them
        query = db.collection('complaints').where('assignedAdminId', '==', userId);
      }
    } else {
      // Customers only see their own complaints
      query = db.collection('complaints').where('customerId', '==', userId);
    }

    const snapshot = await query.get();
    const complaints = [];
    const userIds = new Set();
    
    snapshot.forEach(doc => {
      const data = doc.data();
      if (data.customerId) userIds.add(data.customerId);
      if (data.assignedAdminId) userIds.add(data.assignedAdminId);
      complaints.push({ id: doc.id, ...data });
    });

    // Bulk resolve emails/names/mobile
    const usersInfo = {};
    if (userIds.size > 0) {
      const userDocs = await db.collection('users').where(admin.firestore.FieldPath.documentId(), 'in', Array.from(userIds)).get();
      userDocs.forEach(udoc => {
        const udata = udoc.data();
        usersInfo[udoc.id] = {
          name: udata.name || '',
          email: udata.email || '',
          mobile: udata.mobile || '',
          role: udata.role || ''
        };
      });
    }

    const resolvedComplaints = complaints.map(c => {
      const cust = usersInfo[c.customerId] || {};
      const staff = usersInfo[c.assignedAdminId] || {};
      return {
        ...c,
        customerName: cust.name || '',
        customerEmail: cust.email || 'Unknown Customer',
        customerMobile: cust.mobile || '',
        assignedAdminEmail: staff.email || (c.assignedAdminId ? 'Unknown Staff' : null)
      };
    });

    // Sort in-memory by createdAt descending
    resolvedComplaints.sort((a, b) => {
      const timeA = a.createdAt ? (a.createdAt.seconds || 0) : 0;
      const timeB = b.createdAt ? (b.createdAt.seconds || 0) : 0;
      return timeB - timeA;
    });

    return res.status(200).json(resolvedComplaints);
  } catch (err) {
    console.error('Get Complaints Error:', err);
    return res.status(500).json({ error: 'Server error while fetching complaints.' });
  }
};

// 3. Get single complaint details (Access restricted to owner customer or admin)
const getComplaintById = async (req, res) => {
  try {
    const { id } = req.params;
    const { role, userId, email } = req.user;

    const docRef = db.collection('complaints').doc(id);
    const doc = await docRef.get();

    if (!doc.exists) {
      return res.status(404).json({ error: 'Complaint not found.' });
    }

    const complaintData = doc.data();

    // Security Check: Customers can only view their own complaints
    if (role.toUpperCase() === 'CUSTOMER' && complaintData.customerId !== userId) {
      return res.status(403).json({ error: 'Access Denied: You cannot view this complaint.' });
    }

    // Security Check: Employees can only view complaints assigned to them
    if (role.toUpperCase() === 'ADMIN' && email.toLowerCase() !== 'admin@gmail.com' && complaintData.assignedAdminId !== userId) {
      return res.status(403).json({ error: 'Access Denied: You are not assigned to this complaint.' });
    }

    // Resolve emails/names/mobile
    const customerDoc = await db.collection('users').doc(complaintData.customerId).get();
    let customerName = '';
    let customerEmail = 'Unknown Customer';
    let customerMobile = '';
    
    if (customerDoc.exists) {
      const cdata = customerDoc.data();
      customerName = cdata.name || '';
      customerEmail = cdata.email || '';
      customerMobile = cdata.mobile || '';
    }

    let assignedAdminEmail = null;
    if (complaintData.assignedAdminId) {
      const adminDoc = await db.collection('users').doc(complaintData.assignedAdminId).get();
      assignedAdminEmail = adminDoc.exists 
        ? (adminDoc.data().name && adminDoc.data().name.trim() ? adminDoc.data().name : adminDoc.data().email)
        : null;
    }

    return res.status(200).json({
      id: doc.id,
      ...complaintData,
      customerName,
      customerEmail,
      customerMobile,
      assignedAdminEmail
    });
  } catch (err) {
    console.error('Get Complaint By ID Error:', err);
    return res.status(500).json({ error: 'Server error while fetching complaint details.' });
  }
};

// 4. Update complaint status (Admins only)
const updateComplaintStatus = async (req, res) => {
  try {
    const { id } = req.params;
    const { status, comment, priority } = req.body;
    const { role, userId, email } = req.user;

    if (!status || !['PENDING', 'IN_PROGRESS', 'RESOLVED'].includes(status.toUpperCase())) {
      return res.status(400).json({ error: 'Invalid status. Choose PENDING, IN_PROGRESS, or RESOLVED.' });
    }

    const docRef = db.collection('complaints').doc(id);
    const doc = await docRef.get();

    if (!doc.exists) {
      return res.status(404).json({ error: 'Complaint not found.' });
    }

    const complaintData = doc.data();

    // Security Check: Employees can only update complaints assigned to them
    if (email.toLowerCase() !== 'admin@gmail.com' && complaintData.assignedAdminId !== userId) {
      return res.status(403).json({ error: 'Access Denied: You are not assigned to this complaint.' });
    }

    const oldStatus = complaintData.status;
    const newStatus = status.toUpperCase();

    const updates = {
      status: newStatus,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    };

    // Only main Admin can assign or change priority
    if (email.toLowerCase() === 'admin@gmail.com') {
      if (req.body.assignedAdminId !== undefined) {
        updates.assignedAdminId = req.body.assignedAdminId;
      }
      if (priority && ['LOW', 'MEDIUM', 'HIGH'].includes(priority.toUpperCase())) {
        updates.priority = priority.toUpperCase();
      }
    }

    // Update complaint record
    await docRef.update(updates);

    // Write transition to logs collection
    await db.collection('complaintLogs').add({
      complaintId: id,
      actionBy: adminId,
      oldStatus: oldStatus,
      newStatus: newStatus,
      comment: comment || `Status updated to ${newStatus}.`,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return res.status(200).json({
      message: 'Complaint status updated successfully.',
      complaintId: id,
      oldStatus,
      newStatus,
      assignedAdminId: adminId
    });

  } catch (err) {
    console.error('Update Status Error:', err);
    return res.status(500).json({ error: 'Server error while updating complaint status.' });
  }
};

// 5. Get audit logs of a complaint (Owner customer or admin only)
const getComplaintLogs = async (req, res) => {
  try {
    const { id } = req.params;
    const { role, userId, email } = req.user;

    // Verify complaint existence and access permission first
    const complaintDoc = await db.collection('complaints').doc(id).get();
    if (!complaintDoc.exists) {
      return res.status(404).json({ error: 'Complaint not found.' });
    }

    const complaintData = complaintDoc.data();
    if (role.toUpperCase() === 'CUSTOMER' && complaintData.customerId !== userId) {
      return res.status(403).json({ error: 'Access Denied: You cannot view this audit trail.' });
    }

    // Security Check: Employees can only view logs of complaints assigned to them
    if (role.toUpperCase() === 'ADMIN' && email.toLowerCase() !== 'admin@gmail.com' && complaintData.assignedAdminId !== userId) {
      return res.status(403).json({ error: 'Access Denied: You cannot view this audit trail.' });
    }

    const logsSnapshot = await db.collection('complaintLogs')
      .where('complaintId', '==', id)
      .get();

    const logs = [];
    const actorIds = new Set();
    logsSnapshot.forEach(doc => {
      const data = doc.data();
      if (data.actionBy) actorIds.add(data.actionBy);
      logs.push({ id: doc.id, ...data });
    });

    // Bulk resolve actor emails/names
    const actorNames = {};
    if (actorIds.size > 0) {
      const userDocs = await db.collection('users').where(admin.firestore.FieldPath.documentId(), 'in', Array.from(actorIds)).get();
      userDocs.forEach(udoc => {
        const udata = udoc.data();
        actorNames[udoc.id] = udata.name && udata.name.trim() ? udata.name : udata.email;
      });
    }

    const resolvedLogs = logs.map(l => ({
      ...l,
      actionByEmail: actorNames[l.actionBy] || l.actionBy
    }));

    // Sort in-memory by createdAt ascending
    resolvedLogs.sort((a, b) => {
      const timeA = a.createdAt ? (a.createdAt.seconds || 0) : 0;
      const timeB = b.createdAt ? (b.createdAt.seconds || 0) : 0;
      return timeA - timeB;
    });

    return res.status(200).json(resolvedLogs);
  } catch (err) {
    console.error('Get Logs Error:', err);
    return res.status(500).json({ error: 'Server error while fetching logs.' });
  }
};

// Submit feedback for a complaint (Customer only)
const submitFeedback = async (req, res) => {
  try {
    const { id } = req.params;
    const { rating, comment } = req.body;
    const customerId = req.user.userId;

    if (rating === undefined || rating < 1 || rating > 5) {
      return res.status(400).json({ error: 'Rating is required and must be between 1 and 5.' });
    }

    const docRef = db.collection('complaints').doc(id);
    const doc = await docRef.get();

    if (!doc.exists) {
      return res.status(404).json({ error: 'Complaint not found.' });
    }

    const complaintData = doc.data();

    // Security check: Only the customer who raised the complaint can review it
    if (complaintData.customerId !== customerId) {
      return res.status(403).json({ error: 'Access Denied: You cannot submit feedback for this complaint.' });
    }

    // Update the complaint document with rating and feedback comment
    await docRef.update({
      rating: parseInt(rating, 10),
      feedbackComment: (comment || '').trim(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    // Log the feedback action
    await db.collection('complaintLogs').add({
      complaintId: id,
      actionBy: customerId,
      oldStatus: complaintData.status,
      newStatus: complaintData.status,
      comment: `Customer submitted feedback. Rating: ${rating}/5. Comment: ${(comment || '').trim()}`,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return res.status(200).json({
      message: 'Feedback submitted successfully.'
    });

  } catch (err) {
    console.error('Submit Feedback Error:', err);
    return res.status(500).json({ error: 'Server error while submitting feedback.' });
  }
};

module.exports = {
  createComplaint,
  getComplaints,
  getComplaintById,
  updateComplaintStatus,
  getComplaintLogs,
  submitFeedback
};
