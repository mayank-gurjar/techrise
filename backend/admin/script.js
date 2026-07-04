// App State
let token = localStorage.getItem('crm_token') || null;
let userEmail = localStorage.getItem('crm_email') || null;
let isCoordinator = localStorage.getItem('crm_is_coordinator') === 'true';
let complaints = [];
let news = [];
let employees = [];
let banners = [];
let knownComplaintIds = null;
let pollingInterval = null;

// DOM Elements
const loginContainer = document.getElementById('login-container');
const dashboardContainer = document.getElementById('dashboard-container');
const loginForm = document.getElementById('login-form');
const btnLogin = document.getElementById('btn-login');
const userEmailEl = document.getElementById('user-email');
const btnLogout = document.getElementById('btn-logout');

const navItems = document.querySelectorAll('.nav-item');
const tabContents = document.querySelectorAll('.tab-content');
const tabTitle = document.getElementById('tab-title');
const tabSubtitle = document.getElementById('tab-subtitle');

// Complaints Elements
const complaintsList = document.getElementById('complaints-list');
const complaintsLoader = document.getElementById('complaints-loader');
const searchComplaints = document.getElementById('search-complaints');
const filterStatus = document.getElementById('filter-status');
const filterPriority = document.getElementById('filter-priority');

// Stats Elements
const statPending = document.getElementById('stat-pending');
const statProgress = document.getElementById('stat-progress');
const statResolved = document.getElementById('stat-resolved');

// News Elements
const newsForm = document.getElementById('news-form');
const newsList = document.getElementById('news-list');
const btnPublishNews = document.getElementById('btn-publish-news');

// Initialize App
document.addEventListener('DOMContentLoaded', () => {
    if (token) {
        showDashboard();
    } else {
        showLogin();
    }

    // Setup Event Listeners
    loginForm.addEventListener('submit', handleLogin);
    btnLogout.addEventListener('click', handleLogout);
    
    // Tab switching
    navItems.forEach(item => {
        item.addEventListener('click', () => {
            const tabId = item.getAttribute('data-tab');
            switchTab(tabId);
        });
    });

    // Filters & Search
    searchComplaints.addEventListener('input', renderComplaints);
    filterStatus.addEventListener('change', renderComplaints);
    filterPriority.addEventListener('change', renderComplaints);

    // News Form
    newsForm.addEventListener('submit', handlePublishNews);

    // Employee Registration Form
    const employeeForm = document.getElementById('employee-register-form');
    if (employeeForm) {
        employeeForm.addEventListener('submit', handleRegisterEmployee);
    }

    // Banner Upload Form
    const bannerUploadForm = document.getElementById('banner-upload-form');
    if (bannerUploadForm) {
        bannerUploadForm.addEventListener('submit', handleUploadBanner);
    }
});

// Toast System
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    toast.innerHTML = `
        <span>${message}</span>
        <span class="toast-close" style="cursor:pointer; margin-left: 10px; font-weight: bold;">×</span>
    `;

    toast.querySelector('.toast-close').addEventListener('click', () => {
        toast.remove();
    });

    container.appendChild(toast);

    // Auto-remove after 4 seconds
    setTimeout(() => {
        toast.style.animation = 'slideIn 0.3s ease reverse forwards';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

// Navigation & Screen States
function showLogin() {
    loginContainer.classList.remove('hidden');
    dashboardContainer.classList.add('hidden');
}

async function loadEmployees() {
    try {
        const response = await fetch('/api/auth/employees', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        if (response.ok) {
            employees = await response.json();
        }
    } catch (err) {
        console.error('Failed to load employees:', err);
    }
}

async function showDashboard() {
    loginContainer.classList.add('hidden');
    dashboardContainer.classList.remove('hidden');
    userEmailEl.textContent = userEmail;
    
    // Check if user is the main coordinator admin or manager
    const isAdmin = userEmail.toLowerCase() === 'admin@gmail.com' || isCoordinator;
    
    // Hide administrative tabs for normal employees
    navItems.forEach(item => {
        const tabId = item.getAttribute('data-tab');
        if (tabId !== 'tab-complaints' && !isAdmin) {
            item.style.display = 'none';
        } else {
            item.style.display = 'flex';
        }
    });

    // Make sure we start on the complaints tab
    switchTab('tab-complaints');

    // Load initial data
    if (isAdmin) {
        await loadEmployees();
        loadNews();
        loadBanners();
    } else {
        employees = [];
    }
    loadComplaints();
    
    // Start background updates and alarm polling
    startPolling();
}

async function switchTab(tabId) {
    // Nav Items Active state
    navItems.forEach(btn => {
        if (btn.getAttribute('data-tab') === tabId) {
            btn.classList.add('active');
        } else {
            btn.classList.remove('active');
        }
    });

    // Content Display
    tabContents.forEach(content => {
        if (content.id === tabId) {
            content.classList.remove('hidden');
        } else {
            content.classList.add('hidden');
        }
    });

    // Header Titles
    if (tabId === 'tab-complaints') {
        tabTitle.textContent = "Complaints Manager";
        tabSubtitle.textContent = "Track, assign, and resolve customer complaints";
        await loadEmployees();
        loadComplaints();
    } else if (tabId === 'tab-news') {
        tabTitle.textContent = "Publish News Feed";
        tabSubtitle.textContent = "Broadcast announcement updates to all mobile CRM feeds";
        loadNews();
    } else if (tabId === 'tab-employee') {
        tabTitle.textContent = "Add Employee";
        tabSubtitle.textContent = "Register a new secure staff account authorized to use this portal";
    } else if (tabId === 'tab-banners') {
        tabTitle.textContent = "Manage Banners";
        tabSubtitle.textContent = "Upload or remove banner slides displayed on the mobile app home screen";
        loadBanners();
    }
}

// Authentication Logic
async function handleLogin(e) {
    e.preventDefault();
    const email = document.getElementById('login-email').value;
    const password = document.getElementById('login-password').value;

    const spinner = btnLogin.querySelector('.spinner');
    const btnText = btnLogin.querySelector('span');

    spinner.classList.remove('hidden');
    btnText.classList.add('hidden');
    btnLogin.disabled = true;

    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || 'Login failed.');
        }

        // Verify it is an admin user
        if (data.user.role !== 'ADMIN') {
            throw new Error('Access denied. Only employee accounts are authorized to use this portal.');
        }

        // Save token and display
        token = data.token;
        userEmail = data.user.email;
        isCoordinator = data.user.isCoordinator || false;
        localStorage.setItem('crm_token', token);
        localStorage.setItem('crm_email', userEmail);
        localStorage.setItem('crm_is_coordinator', isCoordinator ? 'true' : 'false');

        showToast('Welcome to Tech Rise Portal!', 'success');
        showDashboard();

    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        spinner.classList.add('hidden');
        btnText.classList.remove('hidden');
        btnLogin.disabled = false;
    }
}

function playAlarmSound() {
    try {
        const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
        const playBeep = (time, duration, frequency) => {
            const osc = audioCtx.createOscillator();
            const gain = audioCtx.createGain();
            osc.connect(gain);
            gain.connect(audioCtx.destination);
            osc.type = 'sine';
            osc.frequency.value = frequency;
            gain.gain.setValueAtTime(0.3, time);
            gain.gain.exponentialRampToValueAtTime(0.01, time + duration - 0.05);
            osc.start(time);
            osc.stop(time + duration);
        };
        const now = audioCtx.currentTime;
        playBeep(now, 0.15, 880);
        playBeep(now + 0.2, 0.15, 880);
        playBeep(now + 0.5, 0.15, 880);
        playBeep(now + 0.7, 0.15, 880);
    } catch (err) {
        console.error('Failed to play synthesized alarm sound:', err);
    }
}

function startPolling() {
    if (pollingInterval) clearInterval(pollingInterval);
    pollingInterval = setInterval(() => {
        if (token) {
            loadComplaints(true);
        }
    }, 5000);
}

function stopPolling() {
    if (pollingInterval) {
        clearInterval(pollingInterval);
        pollingInterval = null;
    }
}

function handleLogout() {
    stopPolling();
    knownComplaintIds = null;
    token = null;
    userEmail = null;
    isCoordinator = false;
    localStorage.removeItem('crm_token');
    localStorage.removeItem('crm_email');
    localStorage.removeItem('crm_is_coordinator');
    showToast('Logged out successfully.');
    showLogin();
}

// Complaints Manager Logic
async function loadComplaints(isSilent = false) {
    if (!isSilent) {
        complaintsLoader.classList.remove('hidden');
    }
    try {
        const response = await fetch('/api/complaints', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.status === 401 || response.status === 403) {
            handleLogout();
            throw new Error('Session expired. Please sign in again.');
        }

        if (!response.ok) {
            throw new Error('Failed to retrieve complaints.');
        }

        const data = await response.json();
        
        let hasNew = false;
        if (knownComplaintIds === null) {
            knownComplaintIds = new Set(data.map(c => c.id));
        } else {
            data.forEach(c => {
                if (!knownComplaintIds.has(c.id)) {
                    knownComplaintIds.add(c.id);
                    hasNew = true;
                    showToast(`New complaint received: "${c.title}"`, 'info');
                }
            });
        }

        complaints = data;
        calculateStats();
        renderComplaints();

        if (hasNew) {
            playAlarmSound();
        }

    } catch (err) {
        if (!isSilent) {
            showToast(err.message, 'error');
        }
    } finally {
        if (!isSilent) {
            complaintsLoader.classList.add('hidden');
        }
    }
}

function calculateStats() {
    let pending = 0;
    let progress = 0;
    let resolved = 0;

    complaints.forEach(c => {
        if (c.status === 'PENDING') pending++;
        else if (c.status === 'IN_PROGRESS') progress++;
        else if (c.status === 'RESOLVED') resolved++;
    });

    statPending.textContent = pending;
    statProgress.textContent = progress;
    statResolved.textContent = resolved;
}

function renderComplaints() {
    complaintsList.innerHTML = '';
    
    const query = searchComplaints.value.toLowerCase().trim();
    const statusVal = filterStatus.value;
    const priorityVal = filterPriority.value;

    const filtered = complaints.filter(c => {
        // Status filter
        if (statusVal !== 'ALL' && c.status !== statusVal) return false;
        
        // Priority filter
        if (priorityVal !== 'ALL' && c.priority !== priorityVal) return false;

        // Search text
        if (query) {
            const title = (c.title || '').toLowerCase();
            const desc = (c.description || '').toLowerCase();
            const email = (c.customerEmail || '').toLowerCase();
            const name = (c.customerName || '').toLowerCase();
            const mobile = (c.customerMobile || '').toLowerCase();
            const id = (c.id || '').toLowerCase();
            
            return title.includes(query) || desc.includes(query) || email.includes(query) || name.includes(query) || mobile.includes(query) || id.includes(query);
        }

        return true;
    });

    if (filtered.length === 0) {
        complaintsList.innerHTML = `
            <div style="grid-column: 1 / -1; text-align: center; padding: 40px; color: var(--text-secondary);">
                No complaints found matching the criteria.
            </div>
        `;
        return;
    }

    filtered.forEach(c => {
        const card = document.createElement('div');
        card.className = 'complaint-card';

        // Date formatting helper
        let formattedDate = 'Just now';
        if (c.createdAt) {
            const timestamp = c.createdAt._seconds ? c.createdAt._seconds * 1000 : c.createdAt.seconds * 1000;
            formattedDate = new Date(timestamp).toLocaleString('en-US', {
                month: 'short',
                day: 'numeric',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });
        }

        const statusClass = `badge-${c.status.toLowerCase().replace('_', '-')}`;
        const priorityClass = `badge-priority-${c.priority.toLowerCase()}`;

        card.innerHTML = `
            <div class="card-row">
                <div class="card-title-group">
                    <h3>${escapeHtml(c.title)}</h3>
                    <span class="complaint-id">ID: ${c.id}</span>
                </div>
                <span class="badge ${statusClass}">${c.status}</span>
            </div>
            
            <p class="complaint-desc">${escapeHtml(c.description)}</p>
            
            ${c.imageBase64 ? `
                <div class="complaint-image-attachment" style="margin-top: 12px; margin-bottom: 12px; display: flex; justify-content: flex-start;">
                    <a href="${c.imageBase64}" target="_blank" title="Click to view full size">
                        <img src="${c.imageBase64}" alt="Complaint Attachment" style="max-width: 100%; max-height: 250px; border-radius: 8px; border: 1px solid var(--border-color); cursor: zoom-in;" />
                    </a>
                </div>
            ` : ''}
            
            <div class="metadata-list">
                <div class="meta-item">
                    <span class="meta-label">Customer:</span>
                    <span class="meta-value">${escapeHtml(c.customerName || 'No Name')} (${escapeHtml(c.customerEmail)})</span>
                </div>
                ${c.customerMobile ? `
                <div class="meta-item">
                    <span class="meta-label">Customer Mobile:</span>
                    <span class="meta-value">${escapeHtml(c.customerMobile)}</span>
                </div>
                ` : ''}
                <div class="meta-item">
                    <span class="meta-label">Priority:</span>
                    <span class="badge ${priorityClass}">${c.priority}</span>
                </div>
                <div class="meta-item">
                    <span class="meta-label">Registered:</span>
                    <span class="meta-value">${formattedDate}</span>
                </div>
                <div class="meta-item">
                    <span class="meta-label">Assigned To:</span>
                    <span class="meta-value">${c.assignedAdminEmail ? escapeHtml(c.assignedAdminEmail) : '<em style="color: var(--text-muted);">Unassigned</em>'}</span>
                </div>
                ${c.rating ? `
                <div class="meta-item feedback-section" style="margin-top: 8px; padding-top: 8px; border-top: 1px dashed var(--border-color); display: flex; align-items: center; gap: 8px;">
                    <span class="meta-label" style="font-weight: bold; color: var(--text-primary);">Feedback:</span>
                    <span class="meta-value" style="font-weight: bold; color: #e65100; font-size: 15px;">
                        ${'★'.repeat(c.rating)}${'☆'.repeat(5 - c.rating)} (${c.rating}/5)
                    </span>
                </div>
                ${c.feedbackComment ? `
                <div class="meta-item" style="margin-top: 4px;">
                    <span class="meta-label" style="font-weight: 500;">Comment:</span>
                    <span class="meta-value" style="font-style: italic; color: #444; background: #fff8f4; padding: 4px 8px; border-left: 3px solid #e65100; border-radius: 2px; width: 100%; display: block; box-sizing: border-box; margin-top: 4px;">
                        "${escapeHtml(c.feedbackComment)}"
                    </span>
                </div>
                ` : ''}
                ` : ''}
            </div>

            <div class="card-actions">
                <div style="display: flex; flex-direction: column; gap: 8px;">
                    <div class="action-form-row" style="margin-bottom: 4px;">
                        <div style="display: flex; flex-direction: column; flex: 1; gap: 4px;">
                            <label style="font-size: 11px; color: var(--text-secondary); font-weight: 500;">Status</label>
                            <select id="status-select-${c.id}" style="width: 100%;">
                                <option value="PENDING" ${c.status === 'PENDING' ? 'selected' : ''}>Pending</option>
                                <option value="IN_PROGRESS" ${c.status === 'IN_PROGRESS' ? 'selected' : ''}>In Progress</option>
                                <option value="RESOLVED" ${c.status === 'RESOLVED' ? 'selected' : ''}>Resolved</option>
                            </select>
                        </div>
                        ${(userEmail.toLowerCase() === 'admin@gmail.com' || isCoordinator) ? `
                        <div style="display: flex; flex-direction: column; flex: 1; gap: 4px;">
                            <label style="font-size: 11px; color: var(--text-secondary); font-weight: 500;">Priority</label>
                            <select id="priority-select-${c.id}" style="width: 100%;">
                                <option value="LOW" ${c.priority === 'LOW' ? 'selected' : ''}>Low</option>
                                <option value="MEDIUM" ${c.priority === 'MEDIUM' ? 'selected' : ''}>Medium</option>
                                <option value="HIGH" ${c.priority === 'HIGH' ? 'selected' : ''}>High</option>
                            </select>
                        </div>
                        ` : ''}
                    </div>
                    ${(userEmail.toLowerCase() === 'admin@gmail.com' || isCoordinator) ? `
                    <div class="action-form-row" style="margin-bottom: 4px;">
                        <div style="display: flex; flex-direction: column; flex: 1; gap: 4px;">
                            <label style="font-size: 11px; color: var(--text-secondary); font-weight: 500;">Assignee</label>
                            <select id="employee-select-${c.id}" style="width: 100%;">
                                <option value="">-- Unassigned --</option>
                                ${employees.map(emp => `
                                    <option value="${emp.id}" ${c.assignedAdminId === emp.id ? 'selected' : ''}>${escapeHtml(emp.email)}</option>
                                `).join('')}
                            </select>
                        </div>
                    </div>
                    ` : ''}
                    <button class="btn-update" data-id="${c.id}" style="width: 100%; margin-top: 4px;">Update Status</button>
                </div>
            </div>
        `;
        complaintsList.appendChild(card);

        // Bind update handler programmatically to comply with CSP script-src-attr 'none'
        const btn = card.querySelector(`button[data-id="${c.id}"]`);
        if (btn) {
            btn.addEventListener('click', () => {
                updateStatus(c.id);
            });
        }
    });
}

// Update Complaint Status API helper
async function updateStatus(complaintId) {
    const select = document.getElementById(`status-select-${complaintId}`);
    const newStatus = select.value;
    
    const empSelect = document.getElementById(`employee-select-${complaintId}`);
    const assignedAdminId = empSelect ? (empSelect.value || null) : undefined;
    
    const priSelect = document.getElementById(`priority-select-${complaintId}`);
    const newPriority = priSelect ? priSelect.value : undefined;

    try {
        const reqBody = { status: newStatus };
        if (assignedAdminId !== undefined) reqBody.assignedAdminId = assignedAdminId;
        if (newPriority !== undefined) reqBody.priority = newPriority;

        const response = await fetch(`/api/complaints/${complaintId}/status`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(reqBody)
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || 'Failed to update status.');
        }

        showToast('Complaint status updated successfully!', 'success');
        
        // Reload complaints list
        loadComplaints();

    } catch (err) {
        showToast(err.message, 'error');
    }
}

// News Announcements Logic
async function loadNews() {
    try {
        const response = await fetch('/api/news', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.ok) {
            news = await response.json();
            renderNewsHistory();
        }
    } catch (err) {
        console.error('Failed to load news history:', err);
    }
}

function renderNewsHistory() {
    newsList.innerHTML = '';
    
    if (news.length === 0) {
        newsList.innerHTML = '<p style="color: var(--text-secondary); text-align: center; padding: 20px;">No announcement history.</p>';
        return;
    }

    news.forEach(item => {
        let formattedDate = 'Recent';
        if (item.createdAt) {
            const timestamp = item.createdAt._seconds ? item.createdAt._seconds * 1000 : item.createdAt.seconds * 1000;
            formattedDate = new Date(timestamp).toLocaleDateString('en-US', {
                month: 'short',
                day: 'numeric',
                year: 'numeric'
            });
        }

        const card = document.createElement('div');
        card.className = 'news-item';
        card.innerHTML = `
            <div class="news-item-header">
                <h4>${escapeHtml(item.title)}</h4>
                <div style="display: flex; align-items: center; gap: 8px;">
                    <span class="news-item-date">${formattedDate}</span>
                    <button class="btn-delete-news" data-id="${item.id}" style="background: none; border: none; color: #ff4d4f; cursor: pointer; font-size: 14px; padding: 4px;" title="Delete Announcement">🗑️</button>
                </div>
            </div>
            <p style="margin-top: 8px; color: var(--text-secondary);">${escapeHtml(item.content)}</p>
        `;
        newsList.appendChild(card);

        const btnDelete = card.querySelector(`.btn-delete-news[data-id="${item.id}"]`);
        if (btnDelete) {
            btnDelete.addEventListener('click', () => {
                deleteNewsBulletin(item.id);
            });
        }
    });
}

async function deleteNewsBulletin(newsId) {
    if (!confirm('Are you sure you want to delete this announcement?')) return;
    
    try {
        const response = await fetch(`/api/news/${newsId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        const data = await response.json();
        
        if (!response.ok) {
            throw new Error(data.error || 'Failed to delete announcement.');
        }
        
        showToast('Announcement deleted successfully!', 'success');
        loadNews();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function handlePublishNews(e) {
    e.preventDefault();
    const title = document.getElementById('news-title').value;
    const content = document.getElementById('news-content').value;

    btnPublishNews.disabled = true;
    btnPublishNews.textContent = 'Publishing...';

    try {
        const response = await fetch('/api/news', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ title, content })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || 'Failed to publish news.');
        }

        showToast('Announcement published successfully!', 'success');
        newsForm.reset();
        loadNews();

    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        btnPublishNews.disabled = false;
        btnPublishNews.textContent = 'Publish Announcement';
    }
}

// XSS prevention helper
function escapeHtml(str) {
    if (!str) return '';
    return str
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

// Add Employee registration helper
async function handleRegisterEmployee(e) {
    e.preventDefault();
    const name = document.getElementById('emp-name').value;
    const email = document.getElementById('emp-email').value;
    const password = document.getElementById('emp-password').value;
    const mobile = document.getElementById('emp-mobile').value;
    const isCoordinatorCheck = document.getElementById('emp-is-coordinator').checked;
    const btnRegister = document.getElementById('btn-register-employee');

    btnRegister.disabled = true;
    btnRegister.textContent = 'Registering...';

    try {
        const response = await fetch('/api/auth/register', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                name: name.trim(),
                email: email.trim(),
                password: password,
                role: 'ADMIN', // Employee role
                mobile: mobile.trim(),
                isCoordinator: isCoordinatorCheck
            })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || 'Registration failed.');
        }

        showToast('Employee account created successfully!', 'success');
        document.getElementById('employee-register-form').reset();

    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        btnRegister.disabled = false;
        btnRegister.textContent = 'Create Employee Account';
    }
}

// Banners Manager logic
async function loadBanners() {
    const bannersList = document.getElementById('banners-list');
    if (!bannersList) return;

    try {
        const response = await fetch('/api/banners', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) {
            throw new Error('Failed to load banners.');
        }

        const data = await response.json();
        banners = data;
        renderBanners();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function renderBanners() {
    const bannersList = document.getElementById('banners-list');
    if (!bannersList) return;

    bannersList.innerHTML = '';

    if (banners.length === 0) {
        bannersList.innerHTML = `
            <div style="grid-column: 1 / -1; text-align: center; padding: 40px; color: var(--text-secondary); background: var(--bg-card); border-radius: 8px; border: 1px dashed rgba(255,255,255,0.1);">
                <p>No active banner slides found. Upload a banner above to display it on the mobile app.</p>
            </div>
        `;
        return;
    }

    banners.forEach(b => {
        const card = document.createElement('div');
        card.style.background = 'var(--bg-card)';
        card.style.borderRadius = '8px';
        card.style.overflow = 'hidden';
        card.style.border = '1px solid rgba(255,255,255,0.05)';
        card.style.display = 'flex';
        card.style.flexDirection = 'column';
        card.style.boxShadow = '0 4px 12px rgba(0,0,0,0.15)';
        card.style.padding = '12px';
        card.style.gap = '8px';

        card.innerHTML = `
            <div style="position: relative; width: 100%; padding-top: 56.25%; background: #000; border-radius: 4px; overflow: hidden;">
                <img src="${b.imageBase64}" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; object-fit: cover;" alt="Banner">
            </div>
            <div style="display: flex; flex-direction: column; justify-content: space-between; gap: 8px; flex: 1;">
                <div>
                    <h4 style="margin: 0; font-size: 14px; font-weight: 600; color: var(--text-primary); text-overflow: ellipsis; overflow: hidden; white-space: nowrap;">
                        ${escapeHtml(b.title) || 'Untitled Banner'}
                    </h4>
                    ${b.redirectUrl ? `
                    <div style="font-size: 11px; color: var(--text-secondary); margin-top: 4px; text-overflow: ellipsis; overflow: hidden; white-space: nowrap;">
                        Link: <a href="${escapeHtml(b.redirectUrl)}" target="_blank" style="color: #ff9100; text-decoration: underline;">${escapeHtml(b.redirectUrl)}</a>
                    </div>
                    ` : ''}
                </div>
                <button class="btn-secondary" data-id="${b.id}" style="width: 100%; padding: 6px 12px; font-size: 12px; background: rgba(239, 68, 68, 0.1); color: #ef4444; border: 1px solid rgba(239, 68, 68, 0.2); border-radius: 4px; cursor: pointer;">
                    Delete Banner
                </button>
            </div>
        `;

        bannersList.appendChild(card);

        // Bind delete action
        const delBtn = card.querySelector(`button[data-id="${b.id}"]`);
        if (delBtn) {
            delBtn.addEventListener('click', () => {
                deleteBanner(b.id);
            });
        }
    });
}

function compressImage(file, maxWidth, maxHeight, quality) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.readAsDataURL(file);
        reader.onload = function (event) {
            const img = new Image();
            img.src = event.target.result;
            img.onload = function () {
                const canvas = document.createElement('canvas');
                let width = img.width;
                let height = img.height;

                // Downscale if image exceeds max bounds
                if (width > maxWidth || height > maxHeight) {
                    if (width > height) {
                        height = Math.round((height * maxWidth) / width);
                        width = maxWidth;
                    } else {
                        width = Math.round((width * maxHeight) / height);
                        height = maxHeight;
                    }
                }

                canvas.width = width;
                canvas.height = height;

                const ctx = canvas.getContext('2d');
                ctx.drawImage(img, 0, 0, width, height);

                // Export to JPEG with quality compression
                const compressedBase64 = canvas.toDataURL('image/jpeg', quality);
                resolve(compressedBase64);
            };
            img.onerror = function (err) {
                reject(new Error('Failed to load image for compression.'));
            };
        };
        reader.onerror = function (err) {
            reject(new Error('Failed to read image file.'));
        };
    });
}

async function handleUploadBanner(e) {
    e.preventDefault();
    const titleInput = document.getElementById('banner-title');
    const urlInput = document.getElementById('banner-url');
    const fileInput = document.getElementById('banner-file');
    const btnUpload = document.getElementById('btn-upload-banner');

    if (!fileInput.files || fileInput.files.length === 0) {
        showToast('Please select an image file to upload.', 'error');
        return;
    }

    const file = fileInput.files[0];
    btnUpload.disabled = true;
    btnUpload.textContent = 'Uploading...';

    try {
        // Compress and resize image to 1280x720 max at 75% quality
        const base64String = await compressImage(file, 1280, 720, 0.75);

        const response = await fetch('/api/banners', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                title: titleInput.value,
                redirectUrl: urlInput.value,
                imageBase64: base64String
            })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || 'Failed to upload banner.');
        }

        showToast('Banner uploaded successfully!', 'success');
        document.getElementById('banner-upload-form').reset();
        loadBanners();
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        btnUpload.disabled = false;
        btnUpload.textContent = 'Upload Banner';
    }
}

async function deleteBanner(id) {
    if (!confirm('Are you sure you want to delete this banner?')) return;

    try {
        const response = await fetch(`/api/banners/${id}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || 'Failed to delete banner.');
        }

        showToast('Banner deleted successfully!', 'success');
        loadBanners();
    } catch (err) {
        showToast(err.message, 'error');
    }
}
