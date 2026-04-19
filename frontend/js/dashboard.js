// dashboard.js — Student Dashboard

async function refreshDashboard() {
    showLoading();
    try {
        await Promise.all([loadStats(), loadDashboardComplaints()]);
    } catch (err) {
        showToast('Error loading dashboard. Is the backend running?', 'error');
    } finally {
        hideLoading();
    }
}

async function loadStats() {
    try {
        const res = await fetch(`${API_BASE_URL}/api/stats`);
        if (!res.ok) throw new Error('Stats fetch failed');
        const data = await res.json();
        if (data.success && data.stats) {
            animateNumber('stat-total',    data.stats.total      || 0);
            animateNumber('stat-pending',  data.stats.pending    || 0);
            animateNumber('stat-progress', data.stats.inProgress || 0);
            animateNumber('stat-resolved', data.stats.resolved   || 0);
        }
    } catch { /* silently fail — stats not critical */ }
}

async function loadDashboardComplaints() {
    const tbody = document.getElementById('complaints-table-body');
    if (!tbody) return;
    try {
        const res = await fetch(`${API_BASE_URL}/api/complaints/all`);
        if (!res.ok) throw new Error('Failed to fetch');
        const data = await res.json();
        const count = document.getElementById('complaint-count-badge');
        if (count) count.textContent = `${data.complaints?.length || 0} total`;

        if (data.success && data.complaints && data.complaints.length > 0) {
            tbody.innerHTML = data.complaints.map(c => createComplaintRow(c, false)).join('');
        } else {
            tbody.innerHTML = `<tr><td colspan="7" class="empty-state">
                <svg class="empty-state-icon" viewBox="0 0 24 24"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
                No complaints found
            </td></tr>`;
        }
    } catch {
        tbody.innerHTML = `<tr><td colspan="7" class="table-loading">
            Cannot connect to backend (localhost:8080). Start the Java server first.
        </td></tr>`;
    }
}

// Smooth number animation
function animateNumber(elId, target) {
    const el = document.getElementById(elId);
    if (!el) return;
    const start = parseInt(el.textContent) || 0;
    const duration = 600;
    const startTime = performance.now();
    const update = (now) => {
        const progress = Math.min((now - startTime) / duration, 1);
        const eased = 1 - Math.pow(1 - progress, 3);
        el.textContent = Math.round(start + (target - start) * eased);
        if (progress < 1) requestAnimationFrame(update);
    };
    requestAnimationFrame(update);
}

document.addEventListener('DOMContentLoaded', () => {
    if (document.querySelector('#dashboard-page.active')) {
        refreshDashboard();
    }
});
