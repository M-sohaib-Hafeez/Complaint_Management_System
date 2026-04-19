// admin.js — Admin Panel Logic

let allAdminComplaints = []; // Cache for client-side filtering

async function refreshDashboard() {
    showLoading();
    try {
        await Promise.all([loadStats(), loadAdminComplaints()]);
    } catch (err) {
        showToast('Error loading dashboard: ' + err.message, 'error');
    } finally {
        hideLoading();
    }
}

async function loadStats() {
    try {
        const sessionId = localStorage.getItem('adminSession');
        const res = await fetch(`${API_BASE_URL}/api/stats`, {
            headers: { 'Authorization': `Bearer ${sessionId}` }
        });
        if (!res.ok) throw new Error('Stats fetch failed');
        const data = await res.json();
        if (data.success && data.stats) {
            const s = data.stats;
            animateNumber('stat-total',    s.total      || 0);
            animateNumber('stat-pending',  s.pending    || 0);
            animateNumber('stat-progress', s.inProgress || 0);
            animateNumber('stat-resolved', s.resolved   || 0);
        }
    } catch { /* non-critical */ }
}

async function loadAdminComplaints() {
    const tbody = document.getElementById('admin-table-body');
    if (!tbody) return;

    try {
        const sessionId = localStorage.getItem('adminSession');
        const res = await fetch(`${API_BASE_URL}/api/complaints/all`, {
            headers: { 'Authorization': `Bearer ${sessionId}` }
        });

        // FIX: Handle 401/403 properly — redirect to login
        if (res.status === 401 || res.status === 403) {
            showToast('Session expired. Redirecting to login...', 'error');
            setTimeout(() => Auth.logout(), 1200);
            return;
        }
        if (!res.ok) throw new Error('Failed to fetch complaints');

        const data = await res.json();
        allAdminComplaints = data.complaints || [];

        const countEl = document.getElementById('admin-complaint-count');
        if (countEl) countEl.textContent = `${allAdminComplaints.length} total`;

        renderFilteredComplaints();
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="8" class="table-loading">
            Cannot load complaints. Check backend connection (localhost:8080).
        </td></tr>`;
    }
}

// FIX: Client-side filtering — was completely missing
function filterComplaints() {
    renderFilteredComplaints();
}

function renderFilteredComplaints() {
    const tbody = document.getElementById('admin-table-body');
    if (!tbody) return;

    const search   = (document.getElementById('admin-search')?.value || '').toLowerCase();
    const status   = document.getElementById('filter-status')?.value  || '';
    const priority = document.getElementById('filter-priority')?.value || '';

    let filtered = allAdminComplaints.filter(c => {
        const matchSearch = !search || [
            c.title, c.description, c.categoryName, c.category,
            String(c.complaintId), String(c.studentName || '')
        ].some(field => (field || '').toLowerCase().includes(search));

        const matchStatus   = !status   || c.status   === status;
        const matchPriority = !priority || c.priority === priority;

        return matchSearch && matchStatus && matchPriority;
    });

    const countEl = document.getElementById('admin-complaint-count');
    if (countEl) {
        countEl.textContent = filtered.length === allAdminComplaints.length
            ? `${allAdminComplaints.length} total`
            : `${filtered.length} of ${allAdminComplaints.length}`;
    }

    if (filtered.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" class="empty-state">
            <svg class="empty-state-icon" viewBox="0 0 24 24"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
            No complaints match your filters
        </td></tr>`;
    } else {
        tbody.innerHTML = filtered.map(c => createComplaintRow(c, true)).join('');
    }
}

// Smooth number animation
function animateNumber(elId, target) {
    const el = document.getElementById(elId);
    if (!el) return;
    const start = parseInt(el.textContent) || 0;
    if (start === target) { el.textContent = target; return; }
    const duration = 600;
    const startTime = performance.now();
    const tick = (now) => {
        const p = Math.min((now - startTime) / duration, 1);
        const ease = 1 - Math.pow(1 - p, 3);
        el.textContent = Math.round(start + (target - start) * ease);
        if (p < 1) requestAnimationFrame(tick);
    };
    requestAnimationFrame(tick);
}

// ---- Init ----
document.addEventListener('DOMContentLoaded', () => {
    const sessionId = localStorage.getItem('adminSession');
    if (!sessionId) {
        window.location.href = 'Adminlogin.html';
        return;
    }
    Auth.displayAdminInfo();
    refreshDashboard();
});
