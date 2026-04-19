// track.js — Complaint Tracker

document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('track-form');
    if (form) form.addEventListener('submit', handleTrackSubmit);
});

async function handleTrackSubmit(e) {
    e.preventDefault();
    const raw = document.getElementById('track-id')?.value.trim() || '';
    // FIX: Strip non-numeric chars (user might paste "#1042")
    const complaintId = raw.replace(/\D/g, '');

    if (!complaintId) {
        showToast('Please enter a valid Complaint ID', 'error');
        return;
    }

    showLoading();
    clearTrackResult();

    try {
        const res = await fetch(`${API_BASE_URL}/api/complaints/${complaintId}`);
        const data = await res.json();

        if (data.success && data.complaint) {
            renderComplaintStatus(data.complaint);
        } else {
            showTrackEmpty(complaintId);
        }
    } catch {
        showToast('Cannot connect to server. Make sure the backend is running.', 'error');
    } finally {
        hideLoading();
    }
}

function renderComplaintStatus(c) {
    const container = document.getElementById('complaint-status-details');
    if (!container) return;

    const id       = c.complaintId || c.id || '—';
    const title    = escapeHtml(c.title || '—');
    const desc     = escapeHtml(c.description || '—');
    const category = escapeHtml(c.categoryName || c.category || '—');
    const priority = (c.priority || 'MEDIUM').toUpperCase();
    const status   = (c.status || 'PENDING').toUpperCase();
    const date     = c.submissionDate
        ? new Date(c.submissionDate).toLocaleString('en-GB', { day:'2-digit', month:'short', year:'numeric', hour:'2-digit', minute:'2-digit' })
        : '—';
    const resolved = c.resolutionDate
        ? new Date(c.resolutionDate).toLocaleString('en-GB', { day:'2-digit', month:'short', year:'numeric', hour:'2-digit', minute:'2-digit' })
        : null;

    container.innerHTML = `
        <div class="status-result-card">
            <div class="status-result-header">
                <span class="status-result-id">Complaint ID #${id}</span>
                <span class="badge badge-${status.toLowerCase().replace('_', '-')}">${status.replace('_', ' ')}</span>
            </div>
            <div class="status-result-body">
                <div class="status-result-title">${title}</div>
                <div class="status-meta-grid">
                    <div class="status-meta-item">
                        <span class="status-meta-label">Category</span>
                        <span class="status-meta-value">${category}</span>
                    </div>
                    <div class="status-meta-item">
                        <span class="status-meta-label">Priority</span>
                        <span class="badge badge-${priority.toLowerCase()}">${priority}</span>
                    </div>
                    <div class="status-meta-item">
                        <span class="status-meta-label">Submitted</span>
                        <span class="status-meta-value">${date}</span>
                    </div>
                    ${resolved ? `
                    <div class="status-meta-item">
                        <span class="status-meta-label">Resolved</span>
                        <span class="status-meta-value" style="color:var(--success)">${resolved}</span>
                    </div>` : ''}
                </div>
                <div class="status-desc-box">
                    <div class="status-desc-label">Description</div>
                    <div class="status-desc-text">${desc}</div>
                </div>
            </div>
            <div class="status-footer">
                Last updated based on available data. Contact admin for more details.
            </div>
        </div>`;
}

function showTrackEmpty(id) {
    const container = document.getElementById('complaint-status-details');
    if (!container) return;
    container.innerHTML = `
        <div class="status-result-card">
            <div class="status-result-body" style="text-align:center; padding: 2.5rem;">
                <svg class="empty-state-icon" viewBox="0 0 24 24"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
                <p style="color:var(--text-muted); font-size:0.9rem;">No complaint found with ID <strong style="color:var(--text-primary)">#${id}</strong></p>
                <p style="color:var(--text-muted); font-size:0.8rem; margin-top:0.5rem;">Check the ID and try again.</p>
            </div>
        </div>`;
}

function clearTrackResult() {
    const container = document.getElementById('complaint-status-details');
    if (container) container.innerHTML = '';
}
