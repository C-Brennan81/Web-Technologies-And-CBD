(function(){
  const TOKEN_KEY = 'jwtToken';
  const VIEW_BASE = '/views';
  const cache = new Map(); // simple in-memory view cache

  function getToken(){
    try { return localStorage.getItem(TOKEN_KEY); } catch { return null; }
  }

  function decodeJwtPayload(token){
    if (!token) return {};
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const json = atob(base64);
      return JSON.parse(json);
    } catch(e){
      return {};
    }
  }

  function getRole(){
    const t = getToken();
    const p = decodeJwtPayload(t);
    const role = p && (p.role || p.roles || p.authorities);
    if (!role) return null;
    if (Array.isArray(role)) return role.find(r => String(r).includes('ADMIN')) ? 'ROLE_ADMIN' : 'ROLE_USER';
    return String(role);
  }

  async function fetchView(name){
    const key = name;
    if (cache.has(key)) return cache.get(key);
    const res = await fetch(`${VIEW_BASE}/${name}.html`);
    if (!res.ok) throw new Error(`Failed to load view ${name}: ${res.status}`);
    const html = await res.text();
    cache.set(key, html);
    return html;
  }

  function setRootHtml(html){
    const root = document.getElementById('root');
    if (!root) throw new Error('Root container #root not found');
    root.innerHTML = html;
  }

  let settingHash = false;
  function safeSetHash(newHash){
    if (settingHash) return;
    if (location.hash === newHash) return;
    try {
      settingHash = true;
      location.hash = newHash;
    } finally {
      setTimeout(() => { settingHash = false; }, 0);
    }
  }

  async function renderForState(){
    const token = getToken();
    const role = getRole();

    let target = '#/login';
    let view = 'landing';

    if (!token){
      target = '#/login';
      view = 'landing';
    } else if (role && role.includes('ADMIN')){
      target = '#/admin';
      view = 'admin';
    } else {
      target = '#/dashboard';
      view = 'dashboard';
    }

    // If URL doesn't match target, update it (will re-enter render but guarded by settingHash)
    if (location.hash !== target) {
      safeSetHash(target);
    }

    // Inject the view
    const html = await fetchView(view);
    setRootHtml(html);

    // After injection, call app binder (idempotent inside should guard per-element where needed)
    if (typeof window.bindApp === 'function') {
      window.bindApp();
    }
  }

  async function route(){
    try {
      await renderForState();
    } catch (e) {
      console.error(e);
      // Fallback to landing on error
      try {
        const html = await fetchView('landing');
        setRootHtml(html);
        if (typeof window.bindApp === 'function') window.bindApp();
      } catch {}
    }
  }

  document.addEventListener('DOMContentLoaded', () => {
    route();

    window.addEventListener('hashchange', () => {
      if (settingHash) return;
      route();
    });

    let lastToken = getToken();
    setInterval(() => {
      const t = getToken();
      if (t !== lastToken){
        lastToken = t;
        route();
      }
    }, 400);
  });
})();
