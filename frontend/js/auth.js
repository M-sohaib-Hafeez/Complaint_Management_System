// auth.js — ComplaintHub v2.0
// FIX: Redirect URL was 'admin-login.html' but file is 'Adminlogin.html'

const Auth = {
    getSession() { return localStorage.getItem('adminSession'); },
    getAdminUser() {
        const json = localStorage.getItem('adminUser');
        try { return json ? JSON.parse(json) : null; }
        catch { return null; }
    },
    isLoggedIn() { return !!this.getSession(); },

    async validateSession() {
        const sessionId = this.getSession();
        if (!sessionId) return false;
        try {
            const res = await fetch(`${API_BASE_URL}/api/auth/validate`, {
                headers: { 'Authorization': `Bearer ${sessionId}` }
            });
            const data = await res.json();
            return data.success === true;
        } catch {
            return false;
        }
    },

    async logout() {
        const sessionId = this.getSession();
        if (sessionId) {
            try {
                await fetch(`${API_BASE_URL}/api/auth/logout`, {
                    method: 'POST',
                    headers: { 'Authorization': `Bearer ${sessionId}` }
                });
            } catch { /* ignore network errors on logout */ }
        }
        localStorage.removeItem('adminSession');
        localStorage.removeItem('adminUser');
        showToast('Logged out successfully', 'success');
        // FIX: Correct filename is 'Adminlogin.html' not 'admin-login.html'
        setTimeout(() => { window.location.href = 'Adminlogin.html'; }, 800);
    },

    async fetchWithAuth(url, options = {}) {
        const sessionId = this.getSession();
        if (!sessionId) throw new Error('Not authenticated');
        const res = await fetch(url, {
            ...options,
            headers: { ...options.headers, 'Authorization': `Bearer ${sessionId}` }
        });
        const data = await res.json();
        if (data.success === false && data.message && data.message.toLowerCase().includes('unauthorized')) {
            await this.logout();
            throw new Error('Session expired');
        }
        return data;
    },

    displayAdminInfo() {
        const admin = this.getAdminUser();
        if (!admin) return;

        // Update topbar
        const topbarName = document.getElementById('admin-topbar-name');
        if (topbarName) topbarName.textContent = `Logged in as: ${admin.fullName || admin.username}`;

        // Update nav
        const container = document.getElementById('admin-info-container');
        if (!container) return;
        const initial = (admin.fullName || admin.username || 'A').charAt(0).toUpperCase();
        container.innerHTML = `
            <div class="admin-profile">
                <div class="admin-avatar">${initial}</div>
                <div class="admin-details">
                    <span class="admin-name">${admin.fullName || admin.username}</span>
                    <span class="admin-role">${admin.role || 'Admin'}</span>
                </div>
                <button class="btn-logout" onclick="Auth.logout()">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                        <polyline points="16 17 21 12 16 7"/>
                        <line x1="21" y1="12" x2="9" y2="12"/>
                    </svg>
                    Logout
                </button>
            </div>`;
    }
};
