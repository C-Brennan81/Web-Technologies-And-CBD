window.bindApp = function () {

    const API = '';
    const TOKEN_KEY = 'jwtToken';

    function getToken() {
        return localStorage.getItem(TOKEN_KEY);
    }

    function setToken(token) {
        localStorage.setItem(TOKEN_KEY, token);
    }

    function clearToken() {
        localStorage.removeItem(TOKEN_KEY);
    }

    async function apiFetch(url, options = {}) {
        const token = getToken();
        const headers = options.headers ? { ...options.headers } : {};
        if (token) headers['Authorization'] = `Bearer ${token}`
        return fetch(API + url, { ...options, headers });
    }

    async function registerUser(username, password) {
        const res = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        const text = await res.text();
        if (!res.ok) throw new Error(text || 'Register failed');
        return text;
    }

    function getRoleFromToken() {
        const t = getToken();
        if (!t) return null;
        try {
            const payload = JSON.parse(atob(t.split('.')[1]));
            return payload.role || null; // JwtService sets claim("role", role)
        } catch {
            return null;
        }
    }

    async function loginUser(username, password) {
        const res = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        if (!res.ok) {
            const text = await res.text();
            throw new Error(text || 'Login failed');
        }

        const data = await res.json();
        return data.token;
    }

    function showAuthedUI() {
        const landing = document.getElementById('landing');
        const appSection = document.getElementById('appSection');
        const authMsg = document.getElementById('authMessage');
        if (landing) landing.style.display = 'none';
        if (appSection) appSection.style.display = '';
        if (authMsg) authMsg.textContent = '';
    }

    function showLoggedOutUI(msg = '') {
        const landing = document.getElementById('landing');
        const appSection = document.getElementById('appSection');
        const authMsg = document.getElementById('authMessage');
        if (landing) landing.style.display = '';
        if (appSection) appSection.style.display = 'none';
        if (authMsg) authMsg.textContent = msg;
    }

    // Tabs
    $('#tabLogin').on('click', () => {
        $('#tabLogin').addClass('active');
        $('#tabRegister').removeClass('active');
        $('#loginPanel').show();
        $('#registerPanel').hide();
    });

    $('#tabRegister').on('click', () => {
        $('#tabRegister').addClass('active');
        $('#tabLogin').removeClass('active');
        $('#registerPanel').show();
        $('#loginPanel').hide();
    });

// Register
    $('#btnRegister').on('click', async () => {
        const username = $('#regUsername').val().trim();
        const email = $('#regEmail').val().trim(); // not sent yet unless you add backend support
        const p1 = $('#regPassword').val();
        const p2 = $('#regPassword2').val();

        if (p1 !== p2) {
            $('#authMessage').text('Passwords do not match');
            return;
        }

        try {
            $('#authMessage').text('');
            await registerUser(username, p1); // add email later when backend supports it
            $('#authMessage').text('Registered. You can sign in now.');
            $('#tabLogin').click();
        } catch (e) {
            $('#authMessage').text(e.message);
        }
    });

// Login
    $('#btnLogin').on('click', async () => {
        const username = $('#loginUsername').val().trim();
        const password = $('#loginPassword').val();

        try {
            $('#authMessage').text('');
            const token = await loginUser(username, password);
            setToken(token);
            showAuthedUI();
            await onAuthed();
        } catch (e) {
            showLoggedOutUI(e.message);
        }
    });

// Auto-login
    if (getToken()) {
        showAuthedUI();
        onAuthed().catch(() => {
            clearToken();
            showLoggedOutUI('Session expired. Please sign in again.');
        });
    } else {
        showLoggedOutUI('');
    }

    // State
    let allGames = [];
    let gameStatsChart = null; // Chart.js instance for per-game modal chart

    // Helpers
    function money(n) {
        const num = (n != null && !isNaN(n)) ? Number(n) : 0;
        return `€${num.toFixed(2)}`;
    }

    function hoursFmt(h) {
        if (h == null || isNaN(h)) return '—';
        return `${Number(h).toFixed(1)}h`;
    }

    function safeText(v, fallback = '—') {
        if (v == null) return fallback;
        const s = String(v).trim();
        return s.length ? s : fallback;
    }

    function numOrZero(v) {
        const n = Number(v);
        return isNaN(n) ? 0 : n;
    }

    // --- Analytics helpers (US-10, US-11) ---
    function computeCompletionRate(games) {
        if (!Array.isArray(games) || games.length === 0) return 0;
        const withStatus = games.filter(g => (g && g.completionStatus && String(g.completionStatus).trim().length));
        if (withStatus.length === 0) return 0;
        const completed = withStatus.filter(g => String(g.completionStatus).toUpperCase() === 'COMPLETED').length;
        return Math.round((completed / withStatus.length) * 100);
    }

    function renderCompletionRate() {
        const el = document.getElementById('completionValue');
        if (!el) return;
        const pct = computeCompletionRate(allGames);
        el.textContent = `${pct}%`;
    }

    function avgOf(arr) {
        const nums = arr.map(Number).filter(n => !isNaN(n));
        if (nums.length === 0) return NaN;
        const sum = nums.reduce((a, b) => a + b, 0);
        return sum / nums.length;
    }

    function getLibraryAverages() {
        const hours = allGames
            .map(g => g && g.playTimeHours)
            .map(v => Number(v))
            .filter(v => !isNaN(v));
        const prices = allGames
            .map(g => g && g.purchasePrice)
            .map(v => Number(v))
            .filter(v => !isNaN(v));
        return {
            avgHours: hours.length ? avgOf(hours) : NaN,
            avgPrice: prices.length ? avgOf(prices) : NaN
        };
    }

    function renderPerGameChart(gameHours, gamePrice) {
        const canvas = document.getElementById('gameStatsChart');
        const emptyDiv = document.getElementById('noChartData');
        if (!canvas || !emptyDiv) return;

        // Dispose previous chart
        if (gameStatsChart && typeof gameStatsChart.destroy === 'function') {
            try { gameStatsChart.destroy(); } catch {}
            gameStatsChart = null;
        }

        const { avgHours, avgPrice } = getLibraryAverages();
        const hasHours = !isNaN(Number(gameHours)) || !isNaN(avgHours);
        const hasPrice = !isNaN(Number(gamePrice)) || !isNaN(avgPrice);

        // If library has no data at all for both metrics, show the empty state
        if ((!hasHours || (isNaN(Number(gameHours)) && isNaN(avgHours))) && (!hasPrice || (isNaN(Number(gamePrice)) && isNaN(avgPrice)))) {
            canvas.style.display = 'none';
            emptyDiv.style.display = '';
            return;
        }

        // Prepare datasets (fallback missing values to 0 for visualization)
        const thisGameHours = isNaN(Number(gameHours)) ? 0 : Number(gameHours);
        const thisGamePrice = isNaN(Number(gamePrice)) ? 0 : Number(gamePrice);
        const libraryHours = isNaN(avgHours) ? 0 : avgHours;
        const libraryPrice = isNaN(avgPrice) ? 0 : avgPrice;

        const labels = ['Playtime (h)', 'Price (€)'];
        const data = {
            labels,
            datasets: [
                {
                    label: 'This Game',
                    backgroundColor: 'rgba(0, 200, 255, 0.5)',
                    borderColor: 'rgba(0, 200, 255, 1)',
                    borderWidth: 1,
                    data: [thisGameHours, thisGamePrice]
                },
                {
                    label: 'Library Avg',
                    backgroundColor: 'rgba(0, 255, 120, 0.4)',
                    borderColor: 'rgba(0, 255, 120, 1)',
                    borderWidth: 1,
                    data: [libraryHours, libraryPrice]
                }
            ]
        };

        const opts = {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: true
                }
            },
            plugins: {
                legend: { position: 'bottom', labels: { color: '#cfe6ff' } },
                tooltip: { enabled: true }
            }
        };

        // Show canvas, hide empty div
        emptyDiv.style.display = 'none';
        canvas.style.display = '';
        const ctx = canvas.getContext('2d');
        // Chart global color tweaks are optional; rely on per-dataset colors
        gameStatsChart = new Chart(ctx, { type: 'bar', data, options: opts });
    }

    // Rendering
    function renderGames(data) {
        const grid = $('#gameGrid');
        grid.empty();

        let totalVal = 0;

        data.forEach(game => {
            const id = game.id;

            const title = safeText(game.title, '');
            const platform = safeText(game.platform, 'Unknown');
            const genre = safeText(game.genre, '—');
            const status = safeText(game.completionStatus, '—');

            const playHours = (game.playTimeHours != null && !isNaN(game.playTimeHours))
                ? Number(game.playTimeHours)
                : null;

            const h = hoursFmt(playHours);

            const priceNum = (game.purchasePrice != null && !isNaN(game.purchasePrice))
                ? Number(game.purchasePrice)
                : 0;

            totalVal += priceNum;

            // NOTE: poster-img starts with CSS placeholder.
            const card = `
                <div class="game-poster"
                     data-title="${title.toLowerCase()}"
                     data-title-raw="${escapeHtml(title)}"
                     data-platform="${escapeHtml(platform)}"
                     data-genre="${escapeHtml(genre)}"
                     data-status="${escapeHtml(status)}"
                     data-hours="${playHours != null ? playHours : ''}"
                     data-price="${priceNum}">
                  
                  <div class="poster-img" data-game-id="${id}"></div>

                  <div class="poster-info">
                    <div class="topline">
                      <span class="platform-tag">${escapeHtml(platform)}</span>
                      <span class="status-pill">${escapeHtml(status)}</span>
                    </div>

                    <div class="fw-bold text-truncate" title="${escapeHtml(title)}">${escapeHtml(title)}</div>

                    <div class="meta-line">
                      <span>${escapeHtml(genre)}</span>
                      <span class="dot">•</span>
                      <span>${h}</span>
                    </div>

                    <div class="text-neon small">${money(priceNum)}</div>
                  </div>
                </div>
            `;

            grid.append(card);
        });

        $('#totalValue').text(
            totalVal.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
        );

        setupCoverLazyLoad();
        setupModalClicks();
    }

    async function loadUsersAdmin() {
        const res = await apiFetch('/api/admin/users');
        if (!res.ok) throw new Error(await res.text());

        const users = await res.json();
        const tbody = document.querySelector('#usersTable tbody');
        tbody.innerHTML = '';

        for (const u of users) {
            const tr = document.createElement('tr');
            tr.innerHTML = `
      <td>${u.id}</td>
      <td>${u.username}</td>
      <td>${u.role}</td>
      <td>${u.enabled}</td>
      <td>
        <button class="btn btn-sm btn-outline-light" data-action="toggle" data-id="${u.id}" data-enabled="${u.enabled}">
          ${u.enabled ? 'Disable' : 'Enable'}
        </button>
        <button class="btn btn-sm btn-outline-light" data-action="role" data-id="${u.id}" data-role="${u.role}">
          ${u.role === 'ROLE_ADMIN' ? 'Make USER' : 'Make ADMIN'}
        </button>
        <button class="btn btn-sm btn-outline-danger" data-action="delete" data-id="${u.id}">
          Delete
        </button>
      </td>
    `;
            tbody.appendChild(tr);
        }

        tbody.querySelectorAll('button').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = btn.dataset.id;
                const action = btn.dataset.action;

                if (action === 'toggle') {
                    const enabled = btn.dataset.enabled !== 'true';
                    await apiFetch(`/api/admin/users/${id}/enabled`, {
                        method: 'PATCH',
                        headers: {'Content-Type':'application/json'},
                        body: JSON.stringify({ enabled })
                    });
                }

                if (action === 'role') {
                    const newRole = btn.dataset.role === 'ROLE_ADMIN' ? 'ROLE_USER' : 'ROLE_ADMIN';
                    await apiFetch(`/api/admin/users/${id}/role`, {
                        method: 'PATCH',
                        headers: {'Content-Type':'application/json'},
                        body: JSON.stringify({ role: newRole })
                    });
                }

                if (action === 'delete') {
                    if (!confirm('Delete this user and ALL their games?')) return;
                    await apiFetch(`/api/admin/users/${id}`, { method: 'DELETE' });
                }

                await loadUsersAdmin();
            });
        });
    }


    async function onAuthed() {
        const role = getRoleFromToken();

        if (role === 'ROLE_ADMIN') {
            // admin mode: do NOT load games
            allGames = [];
            $('#gameGrid').empty();
            $('#totalValue').text('0.00');

            $('#adminSection').show();
            $('#adminLaunchers').show();

            // only call these if you actually implement them in JS
            if (typeof loadUsersAdmin === 'function') await loadUsersAdmin();
            if (typeof loadLaunchersAdmin === 'function') await loadLaunchersAdmin();

            await loadUsersAdmin();

            return;
        }

        // user mode
        $('#adminSection').hide();
        $('#adminLaunchers').hide();
        await loadGames();
    }

    // Import CSV -> POST to backend
    const gameFileInput = document.getElementById('gameFile');
    if (gameFileInput) {
        // Wire the welcome panel's Upload button to trigger the file picker
        const uploadBtn = document.getElementById('btnUploadCsv');
        if (uploadBtn) {
            uploadBtn.addEventListener('click', () => gameFileInput.click());
        }

        gameFileInput.addEventListener('change', async () => {
            if (!gameFileInput.files || gameFileInput.files.length === 0) return;

            try {
                const fd = new FormData();
                fd.append('file', gameFileInput.files[0]);

                const excludeNonFull = confirm('Exclude demos / betas / playtests from import?');

                const res = await apiFetch(`/api/games/upload?excludeNonFull=${excludeNonFull}`, {
                    method: 'POST',
                    body: fd
                });

                const text = await res.text();
                if (!res.ok) throw new Error(text || `Import failed (${res.status})`);

                // optional: replace alert with a nicer UI message later
                alert(text);

                gameFileInput.value = '';
                await loadGames(); // will also refresh welcome panel visibility

            } catch (err) {
                alert(err.message || 'Import failed');
            }
        });
    }

    // Needed this to stop things breaking when titles of games have below characters
    function escapeHtml(str) {
        return String(str ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    }

    // --- ADMIN HELPERS ---
    function decodeJwtPayload(token) {
        try {
            const base64Url = token.split('.')[1];
            const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
            const json = atob(base64);
            return JSON.parse(json);
        } catch (e) {
            return {};
        }
    }

    function isAdmin() {
        const token = getToken();
        if (!token) return false;
        const p = decodeJwtPayload(token);
        const role = p && (p.role || p.roles || p.authorities);
        if (!role) return false;
        if (Array.isArray(role)) return role.some(r => String(r).includes('ADMIN'));
        return String(role).includes('ADMIN');
    }

    function isUser() {
        const token = getToken();
        if (!token) return false;
        const p = decodeJwtPayload(token);
        const role = p && (p.role || p.roles || p.authorities);
        if (!role) return false;
        if (Array.isArray(role)) return role.some(r => String(r).includes('USER'));
        return String(role).includes('USER');
    }

    async function loadLaunchersAdmin() {
        const res = await apiFetch('/api/launchers');
        if (!res.ok) throw new Error(await res.text());
        const launchers = await res.json();

        const tbody = document.querySelector('#launchersTable tbody');
        if (!tbody) return;
        tbody.innerHTML = '';

        for (const l of launchers) {
            const tr = document.createElement('tr');
            tr.innerHTML = `
      <td>${l.id}</td>
      <td>
        <input class="form-control form-control-sm launcher-name" value="${escapeHtml(l.name)}" data-id="${l.id}">
      </td>
      <td>
        <button class="btn btn-sm btn-outline-light btn-save" data-id="${l.id}">Save</button>
        <button class="btn btn-sm btn-outline-danger btn-del" data-id="${l.id}">Delete</button>
      </td>
    `;
            tbody.appendChild(tr);
        }

        tbody.querySelectorAll('.btn-save').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = btn.dataset.id;
                const input = tbody.querySelector(`input.launcher-name[data-id="${id}"]`);
                const name = input.value.trim();

                const r = await apiFetch(`/api/launchers/${id}`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ name })
                });

                if (!r.ok) alert(await r.text());
                else await loadLaunchersAdmin();
            });
        });

        tbody.querySelectorAll('.btn-del').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = btn.dataset.id;
                if (!confirm('Delete this launcher?')) return;

                const r = await apiFetch(`/api/launchers/${id}`, { method: 'DELETE' });
                if (!r.ok) alert(await r.text());
                else await loadLaunchersAdmin();
            });
        });
    }

    function updateAdminPanelsVisibility() {
        const admin = isAdmin();
        const adminSection = document.getElementById('adminSection');
        const launchersPanel = document.getElementById('adminLaunchers');
        if (adminSection) adminSection.style.display = admin ? '' : 'none';
        if (launchersPanel) launchersPanel.style.display = admin ? '' : 'none';
        if (admin) {
            // Populate launchers table when visible
            loadLaunchersAdmin().catch(() => {});
        }
    }

    // Wire Add button for launchers
    const addBtn = document.getElementById('btnAddLauncher');
    if (addBtn) {
        addBtn.addEventListener('click', async () => {
            const input = document.getElementById('launcherNameInput');
            if (!input) return;
            const name = input.value.trim();
            if (!name) return;

            const res = await apiFetch('/api/launchers', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name })
            });

            const text = await res.text();
            if (!res.ok) {
                alert(text);
                return;
            }

            input.value = '';
            await loadLaunchersAdmin();
        });
    }



    // New User Greeting toggle
    function updateWelcomePanel() {
        const panel = document.getElementById('welcomePanel');
        if (!panel) return;
        const isLoggedIn = !!getToken();
        const admin = isAdmin && typeof isAdmin === 'function' ? isAdmin() : false;
        const empty = Array.isArray(allGames) && allGames.length === 0;
        // Show only for logged-in non-admin users with no games
        panel.style.display = (isLoggedIn && !admin && empty) ? '' : 'none';
    }

    // Data load
    async function loadGames() {
        try {
            const res = await apiFetch('/api/games');
            if (!res.ok) throw new Error(`GET /api/games failed: ${res.status}`);
            const data = await res.json();

            allGames = Array.isArray(data) ? data : [];
            renderCompletionRate();
            applyFiltersAndRender();
            updateWelcomePanel();
        } catch (e) {
            console.error(e);
            $('#gameGrid').html(`<div class="text-danger">Failed to load games. Check backend logs.</div>`);
            $('#totalValue').text('0.00');
            updateWelcomePanel();
        }
    }

    // Lazy-load covers (backend caches in DB)
    function setupCoverLazyLoad() {
        const targets = document.querySelectorAll('.poster-img[data-game-id]');
        if (!targets.length) return;

        const observer = new IntersectionObserver(async (entries, obs) => {
            for (const entry of entries) {
                if (!entry.isIntersecting) continue;

                const el = entry.target;
                const gameId = el.getAttribute('data-game-id');

                obs.unobserve(el);

                try {
                    const res = await apiFetch(`/api/games/${gameId}/cover`);
                    if (!res.ok) return;

                    const json = await res.json(); // { coverUrl: "..." }
                    if (json && json.coverUrl) {
                        el.style.backgroundImage = `url('${json.coverUrl}')`;
                        el.classList.add('has-cover');
                    }
                } catch (e) {
                    // keep placeholder styling
                }
            }
        }, { root: null, threshold: 0.15 });

        targets.forEach(t => observer.observe(t));
    }

    // Filters + sorting
    function applyFiltersAndRender() {
        const q = ($('#gameSearch').val() || '').toLowerCase().trim();
        const platform = $('#filterPlatform').val() || '';
        const status = $('#filterStatus').val() || '';
        const sortBy = $('#sortBy').val() || 'title';

        let filtered = allGames.filter(g => {
            const title = (g.title || '').toLowerCase();

            const okSearch = !q || title.includes(q);
            const okPlatform = !platform || (g.platform === platform);
            const okStatus = !status || (g.completionStatus === status);

            return okSearch && okPlatform && okStatus;
        });

        filtered.sort((a, b) => {
            if (sortBy === 'priceDesc') return (numOrZero(b.purchasePrice) - numOrZero(a.purchasePrice));
            if (sortBy === 'priceAsc') return (numOrZero(a.purchasePrice) - numOrZero(b.purchasePrice));
            if (sortBy === 'hoursDesc') return (numOrZero(b.playTimeHours) - numOrZero(a.playTimeHours));
            return String(a.title || '').localeCompare(String(b.title || ''));
        });

        renderGames(filtered);
        renderDataTable(filtered);
        // Keep completion rate in sync; currently based on entire library
        renderCompletionRate();
    }

    let dt = null;

    function renderDataTable(games) {
        const rows = games.map(g => ([
            g.title ?? '',
            g.platform ?? '',
            g.completionStatus ?? '',
            g.genre ?? '',
            g.playTimeHours ?? '',
            g.purchasePrice ?? ''
        ]));

        if (dt) {
            dt.clear();
            dt.rows.add(rows);
            dt.draw();
            return;
        }

        dt = $('#gamesTable').DataTable({
            data: rows,
            pageLength: 10
        });
    }

    // Wire up all filter controls to rerender
    $('#gameSearch, #filterPlatform, #filterStatus, #sortBy').on('input change', applyFiltersAndRender);


    function setupModalClicks() {
        $('.game-poster').off('click').on('click', function () {
            const title = $(this).attr('data-title-raw') || '';
            const platform = $(this).attr('data-platform') || '—';
            const genre = $(this).attr('data-genre') || '—';
            const status = $(this).attr('data-status') || '—';
            const hoursRaw = $(this).attr('data-hours');
            const priceRaw = $(this).attr('data-price');

            const coverEl = $(this).find('.poster-img')[0];
            const bg = coverEl ? coverEl.style.backgroundImage : '';

            // If your modal isn’t in the DOM yet, do nothing instead of crashing the page
            if (!document.getElementById('gameModal')) return;

            $('#modalTitle').text(title);
            $('#modalPlatform').text(platform);
            $('#modalGenre').text(genre);
            $('#modalStatus').text(status);
            $('#modalHours').text(hoursRaw ? `${Number(hoursRaw).toFixed(1)}h` : '—');
            $('#modalPrice').text(money(priceRaw));

            if (document.getElementById('modalCover')) {
                document.getElementById('modalCover').style.backgroundImage = bg;
            }

            // Render per-game chart comparing this game vs library average
            const hoursNum = hoursRaw ? Number(hoursRaw) : NaN;
            const priceNum = priceRaw ? Number(priceRaw) : NaN;
            renderPerGameChart(hoursNum, priceNum);

            $('#gameModal').show();
        });

        $('#modalClose').off('click').on('click', () => $('#gameModal').hide());

        $('#gameModal').off('click').on('click', function (e) {
            if (e.target === this) $('#gameModal').hide();
        });

        // Escape closes modal
        $(document).off('keydown.gameModal').on('keydown.gameModal', function (e) {
            if (e.key === 'Escape') $('#gameModal').hide();
        });
    }

    $('#btnGridView').on('click', () => {
        $('#tableView').hide();
        $('#gameGrid').show();
        $('#btnGridView').addClass('active');
        $('#btnTableView').removeClass('active');
    });

    $('#btnTableView').on('click', () => {
        $('#gameGrid').hide();
        $('#tableView').show();
        if (dt) dt.columns.adjust().draw();
        $('#btnTableView').addClass('active');
        $('#btnGridView').removeClass('active');
    });

    // Set initial active state for view toggle
    $('#btnGridView').addClass('active');
    $('#btnTableView').removeClass('active');


    // --- AUTH WIRING ---

// Tabs
    $('#tabLogin').on('click', () => {
        $('#tabLogin').addClass('active');
        $('#tabRegister').removeClass('active');
        $('#loginPanel').show();
        $('#registerPanel').hide();
    });

    $('#tabRegister').on('click', () => {
        $('#tabRegister').addClass('active');
        $('#tabLogin').removeClass('active');
        $('#registerPanel').show();
        $('#loginPanel').hide();
    });

// Register (guard if element present)
    const regBtn = document.getElementById('btnRegister');
    if (regBtn) {
        regBtn.addEventListener('click', async () => {
            const msg = document.getElementById('authMessage');

            const username = (document.getElementById('regUsername') || {}).value?.trim?.() || '';
            const p1 = (document.getElementById('regPassword') || {}).value || '';
            const p2 = (document.getElementById('regPassword2') || {}).value || '';

            if (!username || !p1) {
                if (msg) msg.textContent = 'Username and password are required';
                return;
            }
            if (p1 !== p2) {
                if (msg) msg.textContent = 'Passwords do not match';
                return;
            }

            try {
                if (msg) msg.textContent = '';
                await registerUser(username, p1);
                if (msg) msg.textContent = 'Registered. You can sign in now.';
                $('#tabLogin').click();
            } catch (e) {
                if (msg) msg.textContent = e.message || 'Register failed';
            }
        });
    }

// Login (guard if element present)
    const loginBtn = document.getElementById('btnLogin');
    if (loginBtn) {
        loginBtn.addEventListener('click', async () => {
            const msg = document.getElementById('authMessage');

            const username = (document.getElementById('loginUsername') || {}).value?.trim?.() || '';
            const password = (document.getElementById('loginPassword') || {}).value || '';

            if (!username || !password) {
                if (msg) msg.textContent = 'Username and password are required';
                return;
            }

            try {
                if (msg) msg.textContent = '';
                const token = await loginUser(username, password);
                setToken(token);
                showAuthedUI();
                updateAdminPanelsVisibility();

                if (isUser()) {
                    await loadGames();
                    applyFiltersAndRender();
                } else {
                    // Admin-only session: do not hit user-only endpoints
                    $('#gameGrid').empty();
                    $('#totalValue').text('0.00');
                }

            } catch (e) {
                clearToken();
                showLoggedOutUI(e.message || 'Login failed');
            }
        });
    }

// Logout (guard if element present)
    const logoutBtn = document.getElementById('btnLogout');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', () => {
            clearToken();
            showLoggedOutUI('Logged out.');
        });
    }

// Auto-login
    if (getToken()) {
        showAuthedUI();
        updateAdminPanelsVisibility();
        if (isUser()) {
            loadGames()
                .then(() => applyFiltersAndRender())
                .catch(() => {
                    clearToken();
                    showLoggedOutUI('Session expired. Please sign in again.');
                });
        } else {
            // Admin-only session: avoid calling user endpoints
            $('#gameGrid').empty();
            $('#totalValue').text('0.00');
        }
    } else {
        showLoggedOutUI('');
    }
};
