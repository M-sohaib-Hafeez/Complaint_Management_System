// main.js — ComplaintHub v2.0
const API_BASE_URL = 'http://localhost:8080';

// ---- Navigation ----
function initNavigation() {
    document.querySelectorAll('.nav-link[data-page]').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            switchPage(link.dataset.page);
        });
    });

    // Mobile toggle
    const toggle = document.getElementById('navToggle');
    const links = document.querySelector('.nav-links');
    if (toggle && links) {
        toggle.addEventListener('click', () => links.classList.toggle('open'));
    }
}

function switchPage(pageName) {
    if (!pageName) return;
    document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.nav-link[data-page]').forEach(l => l.classList.remove('active'));
    const page = document.getElementById(`${pageName}-page`);
    const nav  = document.querySelector(`[data-page="${pageName}"]`);
    if (page) page.classList.add('active');
    if (nav)  nav.classList.add('active');

    // Trigger data load on page switch
    if (pageName === 'dashboard') {
        if (typeof refreshDashboard === 'function') refreshDashboard();
    }

    // Close mobile menu
    const links = document.querySelector('.nav-links');
    if (links) links.classList.remove('open');

    // Update URL
    const url = new URL(window.location);
    url.searchParams.set('page', pageName);
    window.history.pushState({}, '', url);
}

window.addEventListener('popstate', () => {
    const page = new URLSearchParams(window.location.search).get('page') || 'dashboard';
    switchPage(page);
});

// ---- Loading ----
function showLoading() {
    const el = document.getElementById('loading-overlay');
    if (el) el.style.display = 'flex';
}
function hideLoading() {
    const el = document.getElementById('loading-overlay');
    if (el) el.style.display = 'none';
}

// ---- Toast ----
let toastTimer;
function showToast(message, type = 'info') {
    const toast = document.getElementById('toast');
    if (!toast) return;
    clearTimeout(toastTimer);
    toast.textContent = message;
    toast.className = 'toast show';
    if (type === 'success') toast.classList.add('toast-success');
    if (type === 'error')   toast.classList.add('toast-error');
    toastTimer = setTimeout(() => { toast.classList.remove('show'); }, 3500);
}

// ---- Complaint Row Builder ----
function createComplaintRow(complaint, isAdminView = false) {
    const id       = complaint.complaintId || complaint.id || '—';
    const title    = escapeHtml(complaint.title || '—');
    const desc     = escapeHtml(complaint.description || '—');
    const category = escapeHtml(complaint.categoryName || complaint.category || '—');
    const priority = (complaint.priority || 'MEDIUM').toUpperCase();
    const status   = (complaint.status || 'PENDING').toUpperCase();
    const date     = complaint.submissionDate
        ? new Date(complaint.submissionDate).toLocaleDateString('en-GB', { day:'2-digit', month:'short', year:'numeric' })
        : '—';

    const priorityBadge = `<span class="badge badge-${priority.toLowerCase()}">${priority}</span>`;
    const statusBadge   = `<span class="badge badge-${status.toLowerCase().replace('_', '-')}">${status.replace('_', ' ')}</span>`;

    let actionsCell = '';
    if (isAdminView) {
        if (status === 'RESOLVED' || status === 'CLOSED') {
            actionsCell = `<td><span class="resolved-check">✓ Done</span></td>`;
        } else {
            actionsCell = `
            <td>
                <div class="action-group">
                    <select class="status-select-inline" onchange="updateComplaintStatus(${id}, this.value, this)">
                        <option value="PENDING"     ${status === 'PENDING'     ? 'selected' : ''}>Pending</option>
                        <option value="IN_PROGRESS" ${status === 'IN_PROGRESS' ? 'selected' : ''}>In Progress</option>
                        <option value="RESOLVED">Resolve</option>
                    </select>
                </div>
            </td>`;
        }
    }

    return `
        <tr>
            <td class="td-id">#${id}</td>
            <td class="td-title">${title}</td>
            <td class="td-desc" title="${desc}">${desc}</td>
            <td>${category}</td>
            <td>${priorityBadge}</td>
            <td>${statusBadge}</td>
            <td class="td-date">${date}</td>
            ${actionsCell}
        </tr>`;
}

// ---- Status Update (Admin) ----
async function updateComplaintStatus(complaintId, newStatus, selectEl) {
    if (!newStatus) return;

    const confirmMsg = newStatus === 'RESOLVED'
        ? `Mark complaint #${complaintId} as Resolved?`
        : `Change complaint #${complaintId} status to "${newStatus.replace('_', ' ')}"?`;

    if (!confirm(confirmMsg)) {
        // Reset select on cancel by refreshing
        if (typeof refreshDashboard === 'function') await refreshDashboard();
        return;
    }

    // Disable select during request
    if (selectEl) selectEl.disabled = true;
    showLoading();

    try {
        const sessionId = localStorage.getItem('adminSession');
        const res = await fetch(`${API_BASE_URL}/api/complaints/${complaintId}/status`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${sessionId}`
            },
            body: JSON.stringify({ status: newStatus })
        });
        const data = await res.json();
        if (data.success) {
            showToast(`Complaint #${complaintId} updated to ${newStatus.replace('_', ' ')}`, 'success');
            if (typeof refreshDashboard === 'function') await refreshDashboard();
        } else {
            showToast(data.message || 'Failed to update status', 'error');
            if (typeof refreshDashboard === 'function') await refreshDashboard();
        }
    } catch (err) {
        showToast('Connection error. Check backend is running.', 'error');
        if (typeof refreshDashboard === 'function') await refreshDashboard();
    } finally {
        hideLoading();
        if (selectEl) selectEl.disabled = false;
    }
}

// ---- Utility ----
function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

// ---- Init ----
window.addEventListener('load', () => {
    initNavigation();
    const page = new URLSearchParams(window.location.search).get('page') || 'dashboard';
    if (document.getElementById(`${page}-page`)) {
        switchPage(page);
    } else {
        switchPage('dashboard');
    }
});
