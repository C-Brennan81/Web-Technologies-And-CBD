(function () {
    const API = '';
    const TOKEN_KEY = 'jwtToken';

    function getToken() {
        try { return localStorage.getItem(TOKEN_KEY); } catch { return null; }
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
        if (token) headers['Authorization'] = `Bearer ${token}`;
        return fetch(API + url, { ...options, headers });
    }

    async function loginUser(username, password) {
        const res = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        if (!res.ok) throw new Error(await res.text());
        const data = await res.json();
        return data.token;
    }

    async function registerUser(username, password) {
        const res = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        if (!res.ok) throw new Error(await res.text());
        return await res.text();
    }

    function decodeJwtPayload(token) {
        if (!token) return {};
        try {
            const base64Url = token.split('.')[1];
            const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
            return JSON.parse(atob(base64));
        } catch {
            return {};
        }
    }

    function isAdmin() {
        const p = decodeJwtPayload(getToken());
        const r = p && (p.role || p.roles || p.authorities);
        if (!r) return false;
        if (Array.isArray(r)) return r.some(x => String(x).includes('ADMIN'));
        return String(r).includes('ADMIN');
    }

    // ---- ADMIN ----
    async function loadUsersAdmin() {
        const res = await apiFetch('/api/admin/users');
        if (!res.ok) throw new Error(`GET /api/admin/users failed: ${res.status}`);
        return await res.json();
    }

    async function updateUserEnabled(id, enabled){
        const res = await apiFetch(`/api/admin/users/${id}/enabled`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ enabled })
        });
        if (!res.ok) throw new Error(await res.text());
        return await res.text();
    }

    async function updateUserRole(id, role){
        const res = await apiFetch(`/api/admin/users/${id}/role`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ role })
        });
        if (!res.ok) throw new Error(await res.text());
        return await res.text();
    }

    async function deleteUser(id){
        const res = await apiFetch(`/api/admin/users/${id}`, { method: 'DELETE' });
        if (!res.ok) throw new Error(await res.text());
        return await res.text();
    }

    async function renderUsersTable() {
        const tbody = document.querySelector('#usersTable tbody');
        if (!tbody) return;
        const users = await loadUsersAdmin();
        tbody.innerHTML = users.map(u => `
      <tr data-id="${u.id}">
        <td>${u.id}</td>
        <td>${u.username}</td>
        <td>
          <select class="role-select form-select form-select-sm">
            <option value="ROLE_USER" ${u.role === 'ROLE_USER' ? 'selected' : ''}>ROLE_USER</option>
            <option value="ROLE_ADMIN" ${u.role === 'ROLE_ADMIN' ? 'selected' : ''}>ROLE_ADMIN</option>
          </select>
        </td>
        <td>${u.enabled ? 'Yes' : 'No'}</td>
        <td>
          <div class="d-flex gap-1">
            <button class="btn btn-sm btn-outline-light btn-toggle-enabled">${u.enabled ? 'Disable' : 'Enable'}</button>
            <button class="btn btn-sm btn-outline-light btn-apply-role">Apply Role</button>
            <button class="btn btn-sm btn-outline-danger btn-delete-user">Delete</button>
          </div>
        </td>
      </tr>
    `).join('');

        // bind row actions
        tbody.querySelectorAll('tr').forEach(tr => {
            if (tr.dataset.bound) return;
            tr.dataset.bound = '1';
            const id = Number(tr.getAttribute('data-id'));
            const btnToggle = tr.querySelector('.btn-toggle-enabled');
            const btnApplyRole = tr.querySelector('.btn-apply-role');
            const btnDelete = tr.querySelector('.btn-delete-user');
            const roleSelect = tr.querySelector('.role-select');

            if (btnToggle){
                btnToggle.addEventListener('click', async () => {
                    try{
                        const currentlyEnabled = btnToggle.textContent.includes('Disable');
                        await updateUserEnabled(id, !currentlyEnabled);
                        await renderUsersTable();
                    }catch(e){ alert(e.message || 'Failed to update user'); }
                });
            }
            if (btnApplyRole && roleSelect){
                btnApplyRole.addEventListener('click', async () => {
                    try{
                        const role = roleSelect.value;
                        await updateUserRole(id, role);
                        await renderUsersTable();
                    }catch(e){ alert(e.message || 'Failed to update role'); }
                });
            }
            if (btnDelete){
                btnDelete.addEventListener('click', async () => {
                    if (!confirm('Delete this user and all of their data?')) return;
                    try{
                        await deleteUser(id);
                        await renderUsersTable();
                    }catch(e){ alert(e.message || 'Failed to delete user'); }
                });
            }
        });
    }

    // ---- ADMIN LAUNCHERS ----
    async function loadLaunchers(){
        const res = await apiFetch('/api/launchers');
        if (!res.ok) throw new Error(`GET /api/launchers failed: ${res.status}`);
        return await res.json();
    }

    async function createLauncher(name){
        const res = await apiFetch('/api/launchers', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name })
        });
        if (!res.ok) throw new Error(await res.text());
        return await res.json();
    }

    async function updateLauncher(id, name){
        const res = await apiFetch(`/api/launchers/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name })
        });
        if (!res.ok) throw new Error(await res.text());
        return await res.json();
    }

    async function deleteLauncher(id){
        const res = await apiFetch(`/api/launchers/${id}`, { method: 'DELETE' });
        if (!res.ok) throw new Error(await res.text());
        return await res.text();
    }

    async function renderLaunchersTable(){
        const tbody = document.querySelector('#launchersTable tbody');
        if (!tbody) return;
        const launchers = await loadLaunchers();
        tbody.innerHTML = launchers.map(l => `
          <tr data-id="${l.id}">
            <td>${l.id}</td>
            <td>${l.name}</td>
            <td>
              <div class="d-flex gap-1">
                <button class="btn btn-sm btn-outline-light btn-rename-launcher">Rename</button>
                <button class="btn btn-sm btn-outline-danger btn-delete-launcher">Delete</button>
              </div>
            </td>
          </tr>
        `).join('');

        tbody.querySelectorAll('tr').forEach(tr => {
            if (tr.dataset.bound) return;
            tr.dataset.bound = '1';
            const id = Number(tr.getAttribute('data-id'));
            const btnRename = tr.querySelector('.btn-rename-launcher');
            const btnDelete = tr.querySelector('.btn-delete-launcher');
            if (btnRename){
                btnRename.addEventListener('click', async () => {
                    const currentName = tr.children[1]?.textContent || '';
                    const name = prompt('New launcher name:', currentName);
                    if (!name) return;
                    try{
                        await updateLauncher(id, name.trim());
                        await renderLaunchersTable();
                    }catch(e){ alert(e.message || 'Failed to rename launcher'); }
                });
            }
            if (btnDelete){
                btnDelete.addEventListener('click', async () => {
                    if (!confirm('Delete this launcher?')) return;
                    try{
                        await deleteLauncher(id);
                        await renderLaunchersTable();
                    }catch(e){ alert(e.message || 'Failed to delete launcher'); }
                });
            }
        });
    }

    // ---- LANDING (LOGIN/REGISTER) ----
    function bindLanding() {
        // Tabs switching (Sign In / Register)
        const tabLogin = document.getElementById('tabLogin');
        const tabRegister = document.getElementById('tabRegister');
        const loginPanel = document.getElementById('loginPanel');
        const registerPanel = document.getElementById('registerPanel');
        function showLogin(){
            if (loginPanel) loginPanel.style.display = 'block';
            if (registerPanel) registerPanel.style.display = 'none';
            if (tabLogin) tabLogin.classList.add('active');
            if (tabRegister) tabRegister.classList.remove('active');
        }
        function showRegister(){
            if (loginPanel) loginPanel.style.display = 'none';
            if (registerPanel) registerPanel.style.display = 'block';
            if (tabLogin) tabLogin.classList.remove('active');
            if (tabRegister) tabRegister.classList.add('active');
        }
        if (tabLogin && !tabLogin.dataset.bound){
            tabLogin.dataset.bound = '1';
            tabLogin.addEventListener('click', showLogin);
        }
        if (tabRegister && !tabRegister.dataset.bound){
            tabRegister.dataset.bound = '1';
            tabRegister.addEventListener('click', showRegister);
        }

        const btnLogin = document.getElementById('btnLogin');
        if (btnLogin && !btnLogin.dataset.bound) {
            btnLogin.dataset.bound = '1';
            btnLogin.addEventListener('click', async () => {
                const username = (document.getElementById('loginUsername')?.value || '').trim();
                const password = (document.getElementById('loginPassword')?.value || '');
                const msg = document.getElementById('authMessage');

                try {
                    if (msg) msg.textContent = '';
                    const token = await loginUser(username, password);
                    setToken(token);
                    // Router polls token and will navigate accordingly
                } catch (e) {
                    clearToken();
                    if (msg) msg.textContent = e.message || 'Login failed';
                }
            });
        }

        const btnRegister = document.getElementById('btnRegister');
        if (btnRegister && !btnRegister.dataset.bound) {
            btnRegister.dataset.bound = '1';
            btnRegister.addEventListener('click', async () => {
                const username = (document.getElementById('regUsername')?.value || '').trim();
                const p1 = document.getElementById('regPassword')?.value || '';
                const p2 = document.getElementById('regPassword2')?.value || '';
                const msg = document.getElementById('authMessage');

                if (!username || !p1){
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
                    // switch back to login tab for convenience
                    showLogin();
                } catch (e) {
                    if (msg) msg.textContent = e.message || 'Register failed';
                }
            });
        }

        // Logout button exists in admin and dashboard headers
        const logoutBtn = document.getElementById('btnLogout');
        if (logoutBtn && !logoutBtn.dataset.bound) {
            logoutBtn.dataset.bound = '1';
            logoutBtn.addEventListener('click', () => {
                clearToken();
                location.hash = '#/login';
            });
        }
    }

    function bindAdmin() {
        // Users table
        const usersTable = document.getElementById('usersTable');
        if (usersTable && !usersTable.dataset.bound){
            usersTable.dataset.bound = '1';
            renderUsersTable().catch(e=>console.error(e));
        } else if (usersTable){
            // even if already bound, refresh data on each bind attempt
            renderUsersTable().catch(e=>console.error(e));
        }

        // Launchers table
        const launchersTable = document.getElementById('launchersTable');
        if (launchersTable && !launchersTable.dataset.bound){
            launchersTable.dataset.bound = '1';
            renderLaunchersTable().catch(e=>console.error(e));
        } else if (launchersTable){
            renderLaunchersTable().catch(e=>console.error(e));
        }

        // Add launcher button
        const btnAddLauncher = document.getElementById('btnAddLauncher');
        const launcherNameInput = document.getElementById('launcherNameInput');
        if (btnAddLauncher && !btnAddLauncher.dataset.bound){
            btnAddLauncher.dataset.bound = '1';
            btnAddLauncher.addEventListener('click', async () => {
                const name = (launcherNameInput?.value || '').trim();
                if (!name){ alert('Enter a launcher name'); return; }
                try{
                    await createLauncher(name);
                    if (launcherNameInput) launcherNameInput.value = '';
                    await renderLaunchersTable();
                }catch(e){
                    alert(e.message || 'Failed to add launcher');
                }
            });
        }
    }

    function bindDashboard() {
        const gridEl = document.getElementById('gameGrid');
        const tableWrap = document.getElementById('tableView');
        const tableBody = document.querySelector('#gamesTable tbody');
        const searchInput = document.getElementById('gameSearch');
        const filterPlatform = document.getElementById('filterPlatform');
        const filterStatus = document.getElementById('filterStatus');
        const sortBy = document.getElementById('sortBy');
        const btnGrid = document.getElementById('btnGridView');
        const btnTable = document.getElementById('btnTableView');
        const welcome = document.getElementById('welcomePanel');
        const fileInput = document.getElementById('gameFile');
        const btnUploadCsv = document.getElementById('btnUploadCsv');
        const totalValueEl = document.getElementById('totalValue');
        const completionEl = document.getElementById('completionValue');

        // Not on this view
        if (!gridEl && !tableWrap && !welcome) return;

        let allGames = [];
        let viewMode = (tableWrap && tableWrap.style.display !== 'none') ? 'table' : 'grid';

        function text(v){
            if (v === null || v === undefined) return '';
            return String(v);
        }

        function computeStats(list){
            const total = list.reduce((sum, g) => sum + (Number(g.purchasePrice) || 0), 0);
            const completed = list.filter(g => String(g.completionStatus || '').toUpperCase() === 'COMPLETED').length;
            const pct = list.length ? Math.round((completed / list.length) * 100) : 0;
            if (totalValueEl) totalValueEl.textContent = total.toFixed(2);
            if (completionEl) completionEl.textContent = pct + '%';
        }

        function passesFilters(g){
            const q = (searchInput?.value || '').trim().toLowerCase();
            const pf = filterPlatform?.value || '';
            const fs = filterStatus?.value || '';
            if (q && !text(g.title).toLowerCase().includes(q)) return false;
            if (pf && text(g.platform) !== pf) return false;
            if (fs && text(g.completionStatus) !== fs) return false;
            return true;
        }

        function sortGames(list){
            const key = sortBy?.value || 'title';
            const arr = [...list];
            switch(key){
                case 'priceDesc':
                    arr.sort((a,b)=>(b.purchasePrice||0)-(a.purchasePrice||0));
                    break;
                case 'priceAsc':
                    arr.sort((a,b)=>(a.purchasePrice||0)-(b.purchasePrice||0));
                    break;
                case 'hoursDesc':
                    arr.sort((a,b)=>(b.playTimeHours||0)-(a.playTimeHours||0));
                    break;
                default:
                    arr.sort((a,b)=>text(a.title).localeCompare(text(b.title)));
            }
            return arr;
        }

        function applyFilters(){
            const filtered = sortGames(allGames.filter(passesFilters));
            computeStats(filtered);
            render(filtered);
            toggleWelcome(filtered.length);
        }

        function toggleWelcome(count){
            if (!welcome) return;
            const hasData = count > 0;
            welcome.style.display = hasData ? 'none' : 'block';
            if (gridEl) gridEl.style.display = hasData && viewMode === 'grid' ? 'grid' : 'none';
            if (tableWrap) tableWrap.style.display = hasData && viewMode === 'table' ? 'block' : 'none';
        }

        function setView(mode){
            viewMode = mode;
            if (gridEl) gridEl.style.display = mode === 'grid' ? 'grid' : 'none';
            if (tableWrap) tableWrap.style.display = mode === 'table' ? 'block' : 'none';
        }

        function cardHtml(g){
            const price = (g.purchasePrice != null) ? `€${Number(g.purchasePrice).toFixed(2)}` : '€0.00';
            const hours = (g.playTimeHours != null) ? `${Number(g.playTimeHours)}h` : '0h';
            const cover = g.coverUrl ? `style="background-image:url('${g.coverUrl}')"` : '';
            return `
            <div class="poster-card" data-id="${g.id}">
              <div class="poster" ${cover}></div>
              <div class="poster-meta">
                <div class="title">${text(g.title)}</div>
                <div class="sub">${text(g.platform)} • ${text(g.completionStatus || 'NOT PLAYED')} • ${hours} • ${price}</div>
              </div>
            </div>`;
        }

        function renderGrid(list){
            if (!gridEl) return;
            gridEl.innerHTML = list.map(cardHtml).join('');
            // bind click to open modal
            gridEl.querySelectorAll('.poster-card').forEach(el => {
                if (!el.dataset.bound){
                    el.dataset.bound = '1';
                    el.addEventListener('click', () => {
                        const id = Number(el.getAttribute('data-id'));
                        const g = allGames.find(x=>x.id===id);
                        if (g) openModal(g);
                    });
                }
            });
            // lazy load covers
            list.forEach(g => ensureCover(g).catch(()=>{}));
        }

        function renderTable(list){
            if (!tableBody) return;
            tableBody.innerHTML = list.map(g => `
                <tr data-id="${g.id}">
                  <td>${text(g.title)}</td>
                  <td>${text(g.platform)}</td>
                  <td>${text(g.completionStatus || 'NOT PLAYED')}</td>
                  <td>${text(g.genre)}</td>
                  <td>${g.playTimeHours != null ? Number(g.playTimeHours) : ''}</td>
                  <td>${g.purchasePrice != null ? '€'+Number(g.purchasePrice).toFixed(2) : ''}</td>
                </tr>
            `).join('');
            tableBody.querySelectorAll('tr').forEach(tr=>{
                if (!tr.dataset.bound){
                    tr.dataset.bound = '1';
                    tr.addEventListener('click', ()=>{
                        const id = Number(tr.getAttribute('data-id'));
                        const g = allGames.find(x=>x.id===id);
                        if (g) openModal(g);
                    });
                }
            });
        }

        function render(list){
            renderGrid(list);
            renderTable(list);
        }

        async function ensureCover(g){
            if (g.coverUrl) return g.coverUrl;
            try{
                const res = await apiFetch(`/api/games/${g.id}/cover`);
                if (!res.ok) return '';
                const data = await res.json();
                if (data.coverUrl){
                    g.coverUrl = data.coverUrl;
                    // update any poster element on screen
                    const card = document.querySelector(`.poster-card[data-id="${g.id}"] .poster`);
                    if (card) card.style.backgroundImage = `url('${g.coverUrl}')`;
                }
                return g.coverUrl || '';
            }catch{ return ''; }
        }

        function bindViewToggles(){
            if (btnGrid && !btnGrid.dataset.bound){
                btnGrid.dataset.bound = '1';
                btnGrid.addEventListener('click', ()=>{
                    setView('grid');
                    applyFilters();
                });
            }
            if (btnTable && !btnTable.dataset.bound){
                btnTable.dataset.bound = '1';
                btnTable.addEventListener('click', ()=>{
                    setView('table');
                    applyFilters();
                });
            }
        }

        function bindFilters(){
            if (searchInput && !searchInput.dataset.bound){
                searchInput.dataset.bound = '1';
                searchInput.addEventListener('input', ()=>applyFilters());
            }
            if (filterPlatform && !filterPlatform.dataset.bound){
                filterPlatform.dataset.bound = '1';
                filterPlatform.addEventListener('change', ()=>applyFilters());
            }
            if (filterStatus && !filterStatus.dataset.bound){
                filterStatus.dataset.bound = '1';
                filterStatus.addEventListener('change', ()=>applyFilters());
            }
            if (sortBy && !sortBy.dataset.bound){
                sortBy.dataset.bound = '1';
                sortBy.addEventListener('change', ()=>applyFilters());
            }
        }

        function bindUpload(){
            if (btnUploadCsv && !btnUploadCsv.dataset.bound){
                btnUploadCsv.dataset.bound = '1';
                btnUploadCsv.addEventListener('click', ()=> fileInput?.click());
            }
            if (fileInput && !fileInput.dataset.bound){
                fileInput.dataset.bound = '1';
                fileInput.addEventListener('change', async ()=>{
                    const f = fileInput.files && fileInput.files[0];
                    if (!f) return;
                    try{
                        const fd = new FormData();
                        fd.append('file', f);
                        fd.append('excludeNonFull', 'true');
                        const res = await apiFetch('/api/games/upload', { method: 'POST', body: fd });
                        if (!res.ok){
                            const t = await res.text();
                            alert('Upload failed: ' + t);
                        } else {
                            await loadAndRender();
                            alert('Import completed');
                        }
                    }catch(e){
                        alert('Upload error');
                    } finally {
                        fileInput.value = '';
                    }
                });
            }
        }

        function openModal(g){
            const modal = document.getElementById('gameModal');
            const closeBtn = document.getElementById('modalClose');
            const cover = document.getElementById('modalCover');
            const title = document.getElementById('modalTitle');
            const p = document.getElementById('modalPlatform');
            const s = document.getElementById('modalStatus');
            const genre = document.getElementById('modalGenre');
            const hours = document.getElementById('modalHours');
            const price = document.getElementById('modalPrice');
            const chart = document.getElementById('gameStatsChart');
            const noChart = document.getElementById('noChartData');
            if (!modal) return;

            title.textContent = text(g.title);
            p.textContent = text(g.platform);
            s.textContent = text(g.completionStatus || 'NOT PLAYED');
            genre.textContent = text(g.genre);
            hours.textContent = (g.playTimeHours != null ? Number(g.playTimeHours) + ' h' : '0 h');
            price.textContent = (g.purchasePrice != null ? '€' + Number(g.purchasePrice).toFixed(2) : '€0.00');

            // cover
            ensureCover(g).then(()=>{
                if (g.coverUrl && cover){
                    cover.style.backgroundImage = `url('${g.coverUrl}')`;
                } else if (cover){
                    cover.style.backgroundImage = '';
                }
            });

            if (chart) chart.style.display = 'none';
            if (noChart) noChart.style.display = 'block';

            modal.style.display = 'block';

            if (closeBtn && !closeBtn.dataset.bound){
                closeBtn.dataset.bound = '1';
                closeBtn.addEventListener('click', ()=>{ modal.style.display = 'none'; });
            }
            if (!modal.dataset.bound){
                modal.dataset.bound = '1';
                modal.addEventListener('click', (e)=>{
                    if (e.target === modal) modal.style.display = 'none';
                });
            }
        }

        async function loadGames(){
            const res = await apiFetch('/api/games');
            if (!res.ok) throw new Error('Failed to load games: '+res.status);
            const data = await res.json();
            allGames = Array.isArray(data) ? data : [];
        }

        async function loadAndRender(){
            try{
                await loadGames();
                applyFilters();
            } catch(e){
                console.error(e);
            }
        }

        bindViewToggles();
        bindFilters();
        bindUpload();

        // initial
        loadAndRender();
    }

    // router.js calls this after every view injection
    window.bindApp = function () {
        // bind whichever view is currently in DOM
        bindLanding();
        if (isAdmin()) bindAdmin();
        else bindDashboard();
    };
})();