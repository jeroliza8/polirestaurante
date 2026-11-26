import { state, setSession, clearSession, sesionVigente } from './state.js';
import { api } from './api.js';
import { toast } from './ui.js';

// Pagina de cada rol. Cada equipo agrega aqui solo su linea cuando su modulo este listo
// (MESERO, COCINERO, REPARTIDOR). Un rol que no este en este mapa no entra a ninguna pagina.
const PAGINAS = {
  ADMINISTRADOR: 'administrador.html',
  CAJERO: 'cajero.html',
  PROVEEDOR: 'proveedor.html'
};

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
    if (!PAGINAS[r.rol]) return;      // rol sin pagina: no se guarda sesion ni se redirige
    setSession(r);
    location.href = PAGINAS[r.rol];
  } catch (err) {
    $('login-msg').textContent = err.message;
  } finally { btn.disabled = false; }
});

if (sesionVigente() && PAGINAS[state.rol]) location.replace(PAGINAS[state.rol]);
else clearSession();

window.addEventListener('unhandledrejection', e => { toast(e.reason?.message || 'Error inesperado', 'error'); });
