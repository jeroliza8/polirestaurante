import { state, setSession, clearSession, sesionVigente } from './state.js';
import { api } from './api.js';
import { toast } from './ui.js';

const PAGINAS = {
  ADMINISTRADOR: 'administrador.html',
  COCINERO: 'cocinero.html',
  COCINA: 'cocinero.html',
  CAJERO: 'cajero.html',
  PROVEEDOR: 'proveedor.html'
};

const normalizarRol = rol => String(rol || '').trim().toUpperCase().replace(/^ROLE_/, '');

const $ = id => document.getElementById(id);

// Aviso dejado por una pagina al cerrar la sesion (expiracion o inactividad)
try {
  const aviso = sessionStorage.getItem('polirestaurante.aviso');
  if (aviso) { $('login-msg').textContent = aviso; sessionStorage.removeItem('polirestaurante.aviso'); }
} catch { /* nada */ }

$('login-form').addEventListener('submit', async e => {
  e.preventDefault();
  $('login-msg').textContent = '';
  const btn = e.submitter || $('login-form').querySelector('button');
  btn.disabled = true;
  try {
    const r = await api('/auth/login', { method: 'POST',
      body: { usuario: $('login-user').value.trim(), password: $('login-pass').value } });
    $('login-pass').value = '';
    const rol = normalizarRol(r.rol);
    const destino = PAGINAS[rol];
    if (!destino) {
      $('login-msg').textContent = `Rol "${r.rol || 'sin rol'}" sin vista asignada.`;
      return;
    }
    setSession({ ...r, rol });
    location.href = destino;
  } catch (err) {
    $('login-msg').textContent = err.message;
  } finally { btn.disabled = false; }
});

const rolGuardado = normalizarRol(state.rol);
if (sesionVigente() && PAGINAS[rolGuardado]) location.replace(PAGINAS[rolGuardado]);
else clearSession();

window.addEventListener('unhandledrejection', e => { toast(e.reason?.message || 'Error inesperado', 'error'); });