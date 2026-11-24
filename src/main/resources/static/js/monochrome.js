// Monochrome Luxury UI System Controller
document.addEventListener('DOMContentLoaded', () => {
    // 1. Initialize Icons
    if (window.lucide) {
        lucide.createIcons();
    }

    // 1b. Highlight active sidebar menu item automatically
    const currentPath = window.location.pathname;
    document.querySelectorAll('.sidebar-menu .menu-link').forEach(link => {
        const href = link.getAttribute('href');
        if (href && (currentPath === href || (href.length > 1 && currentPath.startsWith(href + '/')))) {
            link.classList.add('active');
        }
    });

    // 2. Theme Management (Light/Dark Mode)
    const savedTheme = localStorage.getItem('theme') || 'light';
    document.documentElement.setAttribute('data-theme', savedTheme);
    updateThemeIcon(savedTheme);

    const themeToggleBtn = document.getElementById('theme-toggle');
    if (themeToggleBtn) {
        themeToggleBtn.addEventListener('click', () => {
            const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
            const nextTheme = currentTheme === 'light' ? 'dark' : 'light';
            document.documentElement.setAttribute('data-theme', nextTheme);
            localStorage.setItem('theme', nextTheme);
            updateThemeIcon(nextTheme);
            showToast(`Đã chuyển sang chế độ ${nextTheme === 'dark' ? 'Tối (Dark Mode)' : 'Sáng (Light Mode)'}`);
            if (window.lucide) lucide.createIcons();
        });
    }

    function updateThemeIcon(theme) {
        const iconContainer = document.getElementById('theme-icon');
        const textContainer = document.getElementById('theme-text');
        if (!iconContainer) return;
        if (theme === 'dark') {
            iconContainer.innerHTML = '☀️';
            if (textContainer) textContainer.textContent = 'Giao diện: Tối';
        } else {
            iconContainer.innerHTML = '🌙';
            if (textContainer) textContainer.textContent = 'Giao diện: Sáng';
        }
    }

    // 3. Command Palette (Ctrl + K)
    const commandOverlay = document.getElementById('command-overlay');
    const commandInput = document.getElementById('command-input');
    const commandResults = document.getElementById('command-results');
    const commandTriggerBtn = document.getElementById('command-trigger-btn');

    function openCommandBar() {
        if (!commandOverlay) return;
        commandOverlay.classList.add('active');
        if (commandInput) {
            commandInput.value = '';
            commandInput.focus();
            fetchCommandResults('');
        }
    }

    function closeCommandBar() {
        if (!commandOverlay) return;
        commandOverlay.classList.remove('active');
    }

    if (commandTriggerBtn) {
        commandTriggerBtn.addEventListener('click', openCommandBar);
    }

    document.addEventListener('keydown', (e) => {
        if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
            e.preventDefault();
            if (commandOverlay && commandOverlay.classList.contains('active')) {
                closeCommandBar();
            } else {
                openCommandBar();
            }
        }
        if (e.key === 'Escape' && commandOverlay && commandOverlay.classList.contains('active')) {
            closeCommandBar();
        }
    });

    if (commandOverlay) {
        commandOverlay.addEventListener('click', (e) => {
            if (e.target === commandOverlay) closeCommandBar();
        });
    }

    if (commandInput) {
        commandInput.addEventListener('input', (e) => {
            fetchCommandResults(e.target.value.trim());
        });
    }

    function fetchCommandResults(query) {
        if (!commandResults) return;
        fetch(`/api/search/command-bar?q=${encodeURIComponent(query)}`)
            .then(res => res.json())
            .then(data => {
                commandResults.innerHTML = '';
                if (!data || data.length === 0) {
                    commandResults.innerHTML = '<div style="padding: 20px; text-align: center; color: var(--text-muted); font-size: 13.5px;">Không tìm thấy kết quả phù hợp</div>';
                    return;
                }
                data.forEach(item => {
                    const el = document.createElement('a');
                    el.href = item.url;
                    el.className = 'command-item';
                    el.innerHTML = `
                        <div>
                            <div style="font-weight: 700; font-size: 14px; color: var(--text-primary);">${item.title}</div>
                            <div style="font-size: 12px; color: var(--text-secondary); margin-top: 2px;">${item.subtitle}</div>
                        </div>
                        <span class="lux-badge">${item.category}</span>
                    `;
                    commandResults.appendChild(el);
                });
            })
            .catch(() => {});
    }

    // 4. Toast notifications from backend
    const flashSuccess = document.body.dataset.flashSuccess;
    const flashError = document.body.dataset.flashError;
    if (flashSuccess && flashSuccess.trim() !== '') {
        showToast(flashSuccess, 'success');
    }
    if (flashError && flashError.trim() !== '') {
        showToast(flashError, 'error');
    }
});

// Toast Engine
function showToast(message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = 'lux-toast';
    toast.innerHTML = `
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="20 6 9 17 4 12"></polyline>
        </svg>
        <span>${message}</span>
    `;

    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(15px)';
        toast.style.transition = 'all 0.3s cubic-bezier(0.16, 1, 0.3, 1)';
        setTimeout(() => toast.remove(), 300);
    }, 4500);
}
