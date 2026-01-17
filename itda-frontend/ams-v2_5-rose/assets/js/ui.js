/**
 * AI Movie Studio v2.8 - UI Utilities
 */

const AMS = window.AMS || (window.AMS = {});

AMS.storage = {
    get(key, fallback = null) {
        try {
            const value = localStorage.getItem(key);
            return value === null ? fallback : value;
        } catch {
            return fallback;
        }
    },

    set(key, value) {
        try {
            localStorage.setItem(key, value);
            return true;
        } catch {
            return false;
        }
    },

    getJSON(key, fallback = null) {
        const raw = this.get(key, null);
        if (raw === null) return fallback;
        try {
            return JSON.parse(raw);
        } catch {
            return fallback;
        }
    },

    setJSON(key, value) {
        return this.set(key, JSON.stringify(value));
    }
};

// ================================
// Dropdown Toggle
// ================================
document.addEventListener('DOMContentLoaded', () => {
    // Dropdown
    document.querySelectorAll('[data-dropdown]').forEach(dropdown => {
        const trigger = dropdown.querySelector('[data-dropdown-trigger]');
        const menu = dropdown.querySelector('[data-dropdown-menu]');

        trigger?.addEventListener('click', (e) => {
            e.stopPropagation();
            menu?.classList.toggle('hidden');
        });
    });

    // Close dropdowns on outside click
    document.addEventListener('click', () => {
        document.querySelectorAll('[data-dropdown-menu]').forEach(menu => {
            menu.classList.add('hidden');
        });
    });

    // Tab Navigation
    document.querySelectorAll('[data-tab]').forEach(tab => {
        tab.addEventListener('click', () => {
            const tabId = tab.getAttribute('data-tab');

            // Update tab buttons
            document.querySelectorAll('[data-tab]').forEach(t => t.classList.remove('active'));
            tab.classList.add('active');

            // Update tab content
            document.querySelectorAll('.tab-content').forEach(content => {
                content.style.display = content.id === tabId ? 'block' : 'none';
            });

            // Update sidebar nav
            document.querySelectorAll('[data-tab-nav]').forEach(nav => {
                nav.classList.toggle('active', nav.getAttribute('data-tab-nav') === tabId);
            });
        });
    });

    // Sidebar nav click
    document.querySelectorAll('[data-tab-nav]').forEach(nav => {
        nav.addEventListener('click', (e) => {
            e.preventDefault();
            const tabId = nav.getAttribute('data-tab-nav');
            const tab = document.querySelector(`[data-tab="${tabId}"]`);
            tab?.click();
        });
    });

    // Chip Selection
    document.querySelectorAll('.chip-group .chip').forEach(chip => {
        chip.addEventListener('click', () => {
            const group = chip.closest('.chip-group');
            const isMultiple = group?.getAttribute('data-multiple') === 'true';

            if (!isMultiple) {
                group?.querySelectorAll('.chip').forEach(c => c.classList.remove('selected'));
            }
            chip.classList.toggle('selected');
        });
    });

    // Toggle Switch
    document.querySelectorAll('.toggle').forEach(toggle => {
        toggle.addEventListener('click', () => {
            toggle.classList.toggle('active');
        });
    });

    // Form Section Collapse
    document.querySelectorAll('.form-section').forEach(section => {
        const key = section.getAttribute('data-persist-key');
        if (!key) return;

        const persisted = AMS.storage.getJSON(`ams.ui.section:${key}`, null);
        if (persisted && typeof persisted.collapsed === 'boolean') {
            section.classList.toggle('collapsed', persisted.collapsed);
        }
    });

    document.querySelectorAll('.form-section-header').forEach(header => {
        header.addEventListener('click', () => {
            const section = header.closest('.form-section');
            if (!section) return;
            section.classList.toggle('collapsed');

            const key = section.getAttribute('data-persist-key');
            if (key) {
                AMS.storage.setJSON(`ams.ui.section:${key}`, { collapsed: section.classList.contains('collapsed') });
            }
        });
    });

    // Password Visibility Toggle
    document.querySelectorAll('[data-toggle-password]').forEach(btn => {
        btn.addEventListener('click', () => {
            const input = btn.closest('.input-icon-wrapper')?.querySelector('input');
            if (input) {
                const isPassword = input.type === 'password';
                input.type = isPassword ? 'text' : 'password';
                const icon = btn.querySelector('[data-lucide]');
                if (icon) {
                    icon.setAttribute('data-lucide', isPassword ? 'eye-off' : 'eye');
                    if (window.lucide) lucide.createIcons();
                }
            }
        });
    });

    // Sidebar Toggle
    initSidebar();
});

// ================================
// Sidebar Toggle
// ================================
function initSidebar() {
    const sidebar = document.querySelector('.sidebar');
    const toggle = document.querySelector('.sidebar-toggle');

    if (!sidebar) return;

    // Restore saved state (default to collapsed if no preference)
    const savedState = AMS.storage.get('sidebarExpanded');
    const isExpanded = savedState === 'true';
    sidebar.classList.toggle('expanded', isExpanded);

    const updateToggleIcon = () => {
        const icon = toggle?.querySelector('[data-lucide]');
        if (!icon) return;
        icon.setAttribute('data-lucide', sidebar.classList.contains('expanded') ? 'chevron-left' : 'chevron-right');
        if (window.lucide) lucide.createIcons();
    };
    updateToggleIcon();

    if (toggle) {
        toggle.addEventListener('click', () => {
            sidebar.classList.toggle('expanded');
            AMS.storage.set('sidebarExpanded', sidebar.classList.contains('expanded'));
            updateToggleIcon();
        });
    }
}

// Make initSidebar available globally
window.initSidebar = initSidebar;

// ================================
// Collab Panel (Floating)
// ================================
AMS.collab = {
    storageKey: 'ams.collab:v1',
    root: null,
    panel: null,
    chatMessages: null,
    chatForm: null,
    chatInput: null,
    toggleBtn: null,
    micBtn: null,
    camBtn: null,
    shareBtn: null,

    getState() {
        return AMS.storage.getJSON(this.storageKey, {
            expanded: false,
            mic: false,
            cam: false,
            share: false,
            messages: [
                { user: '팀원A', text: '클로즈업으로 가자', ts: Date.now() - 600000 },
                { user: '팀원B', text: '좋아, 씬2는 좀 더 밝게', ts: Date.now() - 420000 }
            ]
        });
    },

    setState(next) {
        AMS.storage.setJSON(this.storageKey, next);
        return next;
    },

    init() {
        this.root = document.querySelector('[data-collab]');
        if (!this.root) return;

        this.panel = this.root.querySelector('[data-collab-panel]');
        this.chatMessages = this.root.querySelector('[data-collab-messages]');
        this.chatForm = this.root.querySelector('[data-collab-form]');
        this.chatInput = this.root.querySelector('[data-collab-input]');
        this.toggleBtn = this.root.querySelector('[data-collab-toggle]');
        this.micBtn = this.root.querySelector('[data-collab-mic]');
        this.camBtn = this.root.querySelector('[data-collab-cam]');
        this.shareBtn = this.root.querySelector('[data-collab-share]');
        const openBtns = document.querySelectorAll('[data-collab-open]');

        const state = this.getState();
        this.applyState(state);

        this.toggleBtn?.addEventListener('click', () => {
            const current = this.getState();
            current.expanded = !current.expanded;
            this.applyState(this.setState(current));
        });

        openBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                const current = this.getState();
                current.expanded = true;
                this.applyState(this.setState(current));
            });
        });

        this.root.querySelectorAll('[data-collab-close]').forEach(btn => {
            btn.addEventListener('click', () => {
                const current = this.getState();
                current.expanded = false;
                this.applyState(this.setState(current));
            });
        });

        this.micBtn?.addEventListener('click', () => this.toggleFlag('mic'));
        this.camBtn?.addEventListener('click', () => this.toggleFlag('cam'));
        this.shareBtn?.addEventListener('click', () => this.toggleFlag('share'));

        this.chatForm?.addEventListener('submit', (e) => {
            e.preventDefault();
            const text = (this.chatInput?.value || '').trim();
            if (!text) return;
            this.addMessage({ user: '나', text, ts: Date.now() });
            if (this.chatInput) this.chatInput.value = '';
        });

        document.addEventListener('keydown', (e) => {
            if (e.key !== 'Escape') return;
            const current = this.getState();
            if (!current.expanded) return;
            current.expanded = false;
            this.applyState(this.setState(current));
        });
    },

    toggleFlag(flag) {
        const current = this.getState();
        current[flag] = !current[flag];
        this.applyState(this.setState(current));

        const labels = { mic: '마이크', cam: '카메라', share: '화면공유' };
        const onOff = current[flag] ? 'ON' : 'OFF';
        Toast.info('협업', `${labels[flag]} ${onOff}`);
    },

    addMessage(msg) {
        const current = this.getState();
        current.messages = [...(current.messages || []), msg].slice(-50);
        this.applyState(this.setState(current));
    },

    applyState(state) {
        const expanded = !!state.expanded;
        this.panel?.classList.toggle('hidden', !expanded);

        if (this.toggleBtn) {
            this.toggleBtn.style.display = expanded ? 'none' : 'flex';
        }

        const setIcon = (btn, onIcon, offIcon, on) => {
            if (!btn) return;
            const icon = btn.querySelector('[data-lucide]');
            if (icon) {
                icon.setAttribute('data-lucide', on ? onIcon : offIcon);
            }
            btn.classList.toggle('active', on);
        };

        setIcon(this.micBtn, 'mic', 'mic-off', state.mic);
        setIcon(this.camBtn, 'video', 'video-off', state.cam);
        setIcon(this.shareBtn, 'monitor', 'monitor-off', state.share);

        if (this.chatMessages) {
            const messages = state.messages || [];
            this.chatMessages.innerHTML = messages.map(m => {
                const author = escapeHtml(m.user || 'U');
                const authorClass = m.user === '나' ? 'me' : 'other';
                const text = escapeHtml(m.text || '');
                return `<p class="collab-message"><span class="collab-message-author ${authorClass}">${author}:</span> ${text}</p>`;
            }).join('');
            this.chatMessages.scrollTop = this.chatMessages.scrollHeight;
        }

        if (window.lucide) lucide.createIcons();
    }
};

function escapeHtml(text) {
    return String(text)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

document.addEventListener('DOMContentLoaded', () => {
    AMS.collab.init();
});

// ================================
// Toast System
// ================================
const Toast = {
    container: null,

    init() {
        if (!this.container) {
            this.container = document.createElement('div');
            this.container.className = 'toast-container';
            document.body.appendChild(this.container);
        }
    },

    show({ type = 'info', title, message, actions = [], duration = 5000 }) {
        this.init();

        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;

        const iconMap = {
            success: 'check-circle',
            error: 'x-circle',
            warning: 'alert-triangle',
            info: 'info'
        };

        toast.innerHTML = `
            <div class="toast-icon">
                <i data-lucide="${iconMap[type]}" class="w-4 h-4"></i>
            </div>
            <div class="toast-content">
                <div class="toast-title">${title}</div>
                ${message ? `<div class="toast-message">${message}</div>` : ''}
                ${actions.length ? `
                    <div class="toast-actions">
                        ${actions.map(a => `<button class="toast-action ${a.secondary ? 'secondary' : ''}" data-action="${a.id}">${a.label}</button>`).join('')}
                    </div>
                ` : ''}
            </div>
            <button class="toast-close">
                <i data-lucide="x" class="w-4 h-4"></i>
            </button>
        `;

        // Close button
        toast.querySelector('.toast-close')?.addEventListener('click', () => this.dismiss(toast));

        // Action buttons
        actions.forEach(action => {
            toast.querySelector(`[data-action="${action.id}"]`)?.addEventListener('click', () => {
                action.onClick?.();
                this.dismiss(toast);
            });
        });

        this.container.appendChild(toast);
        if (window.lucide) lucide.createIcons();

        // Auto dismiss
        if (duration > 0) {
            setTimeout(() => this.dismiss(toast), duration);
        }

        return toast;
    },

    dismiss(toast) {
        toast.classList.add('toast-exit');
        setTimeout(() => toast.remove(), 200);
    },

    success(title, message) {
        return this.show({ type: 'success', title, message });
    },

    error(title, message, actions) {
        return this.show({ type: 'error', title, message, actions, duration: 0 });
    },

    warning(title, message) {
        return this.show({ type: 'warning', title, message });
    },

    info(title, message) {
        return this.show({ type: 'info', title, message });
    }
};

// ================================
// Modal System
// ================================
const Modal = {
    open(modalId) {
        const overlay = document.getElementById(modalId);
        if (overlay) {
            overlay.classList.add('active');
            document.body.style.overflow = 'hidden';

            // Focus trap
            const focusable = overlay.querySelectorAll('button, input, select, textarea, [tabindex]:not([tabindex="-1"])');
            if (focusable.length) focusable[0].focus();
        }
    },

    close(modalId) {
        const overlay = document.getElementById(modalId);
        if (overlay) {
            overlay.classList.remove('active');
            document.body.style.overflow = '';
        }
    },

    closeAll() {
        document.querySelectorAll('.modal-overlay.active').forEach(overlay => {
            overlay.classList.remove('active');
        });
        document.body.style.overflow = '';
    }
};

// Modal event listeners
document.addEventListener('DOMContentLoaded', () => {
    // Open modal
    document.querySelectorAll('[data-modal-open]').forEach(btn => {
        btn.addEventListener('click', () => {
            Modal.open(btn.getAttribute('data-modal-open'));
        });
    });

    // Close modal
    document.querySelectorAll('[data-modal-close]').forEach(btn => {
        btn.addEventListener('click', () => {
            const overlay = btn.closest('.modal-overlay');
            if (overlay) Modal.close(overlay.id);
        });
    });

    // Close on overlay click
    document.querySelectorAll('.modal-overlay').forEach(overlay => {
        overlay.addEventListener('click', (e) => {
            if (e.target === overlay) Modal.close(overlay.id);
        });
    });

    // Close on ESC
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') Modal.closeAll();
    });
});

// ================================
// Loading States
// ================================
const Loading = {
    showButton(btn) {
        btn.classList.add('btn-loading');
        const text = btn.innerHTML;
        btn.setAttribute('data-original-text', text);
        btn.innerHTML = `<span class="btn-text">${text}</span><div class="spinner spinner-sm spinner-white"></div>`;
    },

    hideButton(btn) {
        btn.classList.remove('btn-loading');
        const originalText = btn.getAttribute('data-original-text');
        if (originalText) btn.innerHTML = originalText;
    },

    showOverlay(container) {
        const overlay = document.createElement('div');
        overlay.className = 'loading-overlay';
        overlay.innerHTML = `<div class="spinner spinner-lg"></div><div class="loading-text">Loading...</div>`;
        container.style.position = 'relative';
        container.appendChild(overlay);
        return overlay;
    },

    hideOverlay(overlay) {
        overlay?.remove();
    }
};

// ================================
// Utility Functions
// ================================
function formatDuration(seconds) {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
}

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(this, args), wait);
    };
}

// Make utilities globally available
window.Toast = Toast;
window.Modal = Modal;
window.Loading = Loading;
window.AMS = AMS;
