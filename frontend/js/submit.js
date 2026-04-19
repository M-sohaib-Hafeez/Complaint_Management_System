// submit.js — Complaint Form

// Character counter for description
document.addEventListener('DOMContentLoaded', () => {
    const desc = document.getElementById('description');
    const counter = document.getElementById('desc-count');
    if (desc && counter) {
        desc.addEventListener('input', () => {
            const len = desc.value.length;
            counter.textContent = `${len} / 1000`;
            counter.style.color = len > 900 ? 'var(--danger)' : len > 700 ? 'var(--warning)' : 'var(--text-muted)';
        });
    }

    // Priority pill active state
    document.querySelectorAll('.priority-opt input').forEach(input => {
        input.addEventListener('change', () => {
            document.querySelectorAll('.priority-opt').forEach(opt => opt.classList.remove('selected'));
            input.closest('.priority-opt').classList.add('selected');
        });
    });

    // Form submit
    const form = document.getElementById('complaint-form');
    if (form) form.addEventListener('submit', handleComplaintSubmit);
});

async function handleComplaintSubmit(e) {
    e.preventDefault();

    clearAllErrors();

    const fields = {
        studentName: document.getElementById('studentName')?.value.trim(),
        email:       document.getElementById('email')?.value.trim(),
        department:  document.getElementById('department')?.value,
        semester:    document.getElementById('semester')?.value,
        title:       document.getElementById('title')?.value.trim(),
        description: document.getElementById('description')?.value.trim(),
        category:    document.getElementById('category')?.value,
    };

    // FIX: Comprehensive client-side validation with per-field errors
    let valid = true;
    if (!fields.studentName) { setError('studentName', 'Full name is required'); valid = false; }
    if (!fields.email) {
        setError('email', 'Email is required'); valid = false;
    } else if (!/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(fields.email)) {
        setError('email', 'Enter a valid email address'); valid = false;
    }
    if (!fields.department) { setError('department', 'Please select a department'); valid = false; }
    if (!fields.semester)   { setError('semester', 'Please select a semester'); valid = false; }
    if (!fields.title)      { setError('title', 'Complaint title is required'); valid = false; }
    if (!fields.description || fields.description.length < 20) {
        setError('description', 'Please provide a detailed description (at least 20 characters)'); valid = false;
    }
    if (!fields.category)   { setError('category', 'Please select a category'); valid = false; }

    if (!valid) {
        showToast('Please fix the errors before submitting', 'error');
        return;
    }

    const priority = document.querySelector('input[name="priority"]:checked')?.value || 'MEDIUM';

    const submitBtn = document.getElementById('submit-btn');
    if (submitBtn) { submitBtn.disabled = true; submitBtn.textContent = 'Submitting...'; }
    showLoading();

    try {
        const res = await fetch(`${API_BASE_URL}/api/complaints/submit`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                studentName: fields.studentName,
                email:       fields.email,
                department:  fields.department,
                semester:    parseInt(fields.semester),
                title:       fields.title,
                description: fields.description,
                category:    fields.category,
                priority:    priority
            })
        });

        const data = await res.json();

        if (data.success) {
            document.getElementById('submitted-id').textContent = `#${data.complaintId}`;
            document.getElementById('complaint-form-wrap').style.display = 'none';
            document.getElementById('submission-success').style.display = 'block';
            showToast('Complaint submitted successfully!', 'success');
        } else {
            showToast(data.message || 'Submission failed. Please try again.', 'error');
        }
    } catch {
        showToast('Cannot connect to server. Make sure the backend is running on port 8080.', 'error');
    } finally {
        hideLoading();
        if (submitBtn) { submitBtn.disabled = false; submitBtn.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="btn-icon"><path d="M22 2L11 13"/><path d="M22 2L15 22l-4-9-9-4 20-7z"/></svg>Submit Complaint'; }
    }
}

function setError(fieldId, message) {
    const errEl = document.getElementById(`err-${fieldId}`);
    const input = document.getElementById(fieldId);
    if (errEl) errEl.textContent = message;
    if (input) input.classList.add('error');
}

function clearAllErrors() {
    document.querySelectorAll('.field-error').forEach(el => el.textContent = '');
    document.querySelectorAll('.field-input.error').forEach(el => el.classList.remove('error'));
}

function resetComplaintForm() {
    document.getElementById('complaint-form')?.reset();
    clearAllErrors();
    const counter = document.getElementById('desc-count');
    if (counter) counter.textContent = '0 / 1000';
    // Reset priority to Medium
    const mediumRadio = document.querySelector('input[name="priority"][value="MEDIUM"]');
    if (mediumRadio) mediumRadio.checked = true;
}

function hideSuccessMessage() {
    document.getElementById('submission-success').style.display = 'none';
    document.getElementById('complaint-form-wrap').style.display = 'block';
    resetComplaintForm();
}
