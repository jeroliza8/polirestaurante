// Estructura comun de las paginas por rol: menu lateral y control de sesion.
// La logica de sesion (30 min de inactividad, renovacion del token) es la misma de antes.
import { state, setSession, clearSession, sesionVigente } from './state.js';
import { post, onSessionExpired } from './api.js';
import { esc, toast, limpiarIntervalos } from './ui.js';
import { conectarEventosCocina } from './cocina-eventos.js';

const $ = id => document.getElementById(id);
const NOMBRE_ROL = { ADMINISTRADOR: 'Administrador', CAJERO: 'Cajero', PROVEEDOR: 'Proveedor' };

// Para opciones del menu que aun no tienen vista detras
export const moduloPendiente = titulo => panel => {
  panel.innerHTML = `<div class="card"><h2>${esc(titulo)}</h2>
    <p class="muted">Este módulo aún no está disponible en esta entrega.</p></div>`;
};

function salir(mensaje) {
  limpiarIntervalos();
  clearSession();
  if (mensaje) { try { sessionStorage.setItem('polirestaurante.aviso', mensaje); } catch { /* nada */ } }
  location.replace('index.html');
}

export function iniciarPagina({ rol, raiz, menu, inicial }) {
  // Sin sesion, o con la sesion de otro rol: de vuelta al login (que reenvia a la pagina correcta)
  if (!sesionVigente() || state.rol !== rol) { location.replace('index.html'); return; }
  
  $('user-info').textContent = state.nombre;
  $('rol-badge').textContent = NOMBRE_ROL[rol] || rol;
  $('fecha-hoy').textContent = new Date().toLocaleDateString('es-CO',
    { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
  $('logout').addEventListener('click', () => salir());
  onSessionExpired(() => salir('Su sesión expiró. Inicie sesión de nuevo.'));

  const nav = $('menu');
  nav.innerHTML = menu.map(m => `<button class="sb-item" role="tab" data-tab="${m.id}" title="${esc(m.label)}">
    <span class="sb-ico" aria-hidden="true">${m.icono}</span><span>${esc(m.label)}</span></button>`).join('');
  nav.querySelectorAll('.sb-item').forEach(b => b.addEventListener('click', () => abrir(b.dataset.tab)));

  function abrir(id) {
    limpiarIntervalos();
    const item = menu.find(m => m.id === id);
    nav.querySelectorAll('.sb-item').forEach(b => b.classList.toggle('active', b.dataset.tab === id));
    $('migas').innerHTML = `<span>${esc(raiz)}</span> / <strong>${esc(item.label)}</strong>`;
    const panel = $('panel');
    panel.innerHTML = '';
    item.render(panel);
  }
  abrir(inicial || menu[0].id);

  // Observer de Cocina: no sustituye las vistas de los otros integrantes.
  if (rol === 'MESERO' || rol === 'ADMINISTRADOR') {
    conectarEventosCocina({ alEvento: evento => {
      if (evento.tipo === 'PEDIDO_LISTO') toast(`Pedido #${evento.idPedido} listo para recoger.`);
      if (evento.tipo === 'DISPONIBILIDAD_ACTUALIZADA') toast(`Producto #${evento.idProducto}: ${evento.disponible ? 'disponible' : 'agotado'}.`);
      window.dispatchEvent(new CustomEvent('polirestaurante:cocina', { detail: evento }));
    }});
  }

  // ---------- RNF04: cierre por 30 min de inactividad + renovacion del token ----------
  let ultimaActividad = Date.now();
  ['click', 'keydown', 'mousemove', 'touchstart'].forEach(ev =>
    document.addEventListener(ev, () => { ultimaActividad = Date.now(); }, { passive: true }));

  setInterval(async () => {
    if (!state.token) return;
    const inactivo = Date.now() - ultimaActividad;
    if (inactivo >= 30 * 60 * 1000 || !sesionVigente()) { salir('Sesión cerrada por inactividad.'); return; }
    if (inactivo < 5 * 60 * 1000 && state.expiraEn - Date.now() < 10 * 60 * 1000) {
      try { setSession(await post('/auth/renovar')); } catch { /* si falla, el 401 cierra la sesion */ }
    }
  }, 60 * 1000);
}

window.addEventListener('unhandledrejection', e => { toast(e.reason?.message || 'Error inesperado', 'error'); });
