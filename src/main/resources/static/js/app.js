$(document).ready(function () {

    // State
    let allGames = [];

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

    // Needed this to stop things breaking when titles of games have below characters
    function escapeHtml(str) {
        return String(str ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    }

    // Data load
    async function loadGames() {
        try {
            const res = await fetch('/api/games');
            if (!res.ok) throw new Error(`GET /api/games failed: ${res.status}`);
            const data = await res.json();

            allGames = Array.isArray(data) ? data : [];
            applyFiltersAndRender();
        } catch (e) {
            console.error(e);
            $('#gameGrid').html(`<div class="text-danger">Failed to load games. Check backend logs.</div>`);
            $('#totalValue').text('0.00');
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
                    const res = await fetch(`/api/games/${gameId}/cover`);
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


    // Initial load

    loadGames();
});
