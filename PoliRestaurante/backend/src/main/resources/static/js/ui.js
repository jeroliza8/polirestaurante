// Utilidades de interfaz: escape de HTML, formatos, modales, avisos e impresion.

const ESC = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
/** Escapa texto que viene de la base antes de ponerlo en el HTML (evita XSS). */
export const esc = v => (v === null || v === undefined ? '' : String(v).replace(/[&<>"']/g, c => ESC[c]));

const COP = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
export const money = v => COP.format(Number(v) || 0);
export const num = v => new Intl.NumberFormat('es-CO', { maximumFractionDigits: 2 }).format(Number(v) || 0);

export function fecha(iso, conHora = true) {
  if (!iso) return '-';
  const d = new Date(iso);
  if (isNaN(d)) return esc(iso);
  return d.toLocaleString('es-CO', conHora
    ? { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' }
    : { day: '2-digit', month: '2-digit', year: 'numeric' });
}

/** Valor para <input type="datetime-local"> */
export function inputFecha(d) {
  const p = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`;
}
export const isoLocal = v => (v && v.length === 16 ? v + ':00' : v);

export function badge(texto, color = '') {
  return `<span class="badge ${color ? 'badge-' + color : ''}">${esc(texto)}</span>`;
}

const COLORES = {
  PENDIENTE: 'amber', EN_PREPARACION: 'blue', LISTO: 'green', ENTREGADO: 'green', CANCELADO: 'red', PAGADO: 'blue',
  ANULADO: 'red', LIBRE: 'green', OCUPADA: 'amber', OK: 'green', BAJO: 'amber', AGOTADO: 'red',
  DESPACHADA: 'blue', RECIBIDA: 'green', CANCELADA: 'red', PAGADO_PROV: 'green',
  EMITIDA: 'green', SIN_EMITIR: 'amber', PENDIENTE_FE: 'amber', ABIERTA: 'green', CERRADA: '',
  activo: 'green', inactivo: 'red'
};
export const estado = (e, texto) => badge(texto || String(e ?? '-').replace(/_/g, ' '), COLORES[e] || '');

export function html(strings, ...vals) {
  return strings.reduce((acc, s, i) => acc + s + (i < vals.length ? vals[i] : ''), '');
}

export function toast(msg, tipo = 'ok') {
  const t = document.createElement('div');
  t.className = 'toast ' + tipo;
  t.textContent = msg;
  document.getElementById('toast-root').appendChild(t);
  setTimeout(() => t.remove(), tipo === 'error' ? 6000 : 3500);
}

export const errorToast = e => toast(e.message || String(e), 'error');

/**
 * Abre un modal. contenido: HTML del cuerpo. acciones: [{texto, clase, onClick(ctx)}].
 * onClick puede devolver false para dejar el modal abierto. ctx = {body, cerrar, error(msg)}.
 */
export function modal({ titulo, contenido, acciones = [], grande = false, alAbrir }) {
  const root = document.getElementById('modal-root');
  const back = document.createElement('div');
  back.className = 'modal-backdrop';
  back.innerHTML = `
    <div class="modal ${grande ? 'modal-lg' : ''}" role="dialog" aria-modal="true">
      <div class="modal-head"><h3>${esc(titulo)}</h3>
        <button class="btn btn-ghost btn-sm" data-cerrar aria-label="Cerrar">✕</button></div>
      <div class="modal-body">${contenido}<p class="form-error" data-error></p></div>
      <div class="modal-foot"></div>
    </div>`;
  const body = back.querySelector('.modal-body');
  const foot = back.querySelector('.modal-foot');
  const errEl = back.querySelector('[data-error]');
  const cerrar = () => back.remove();
  const ctx = {
    body, cerrar, raiz: back,
    error: msg => { errEl.textContent = msg || ''; },
    setAcciones: lista => pintarAcciones(lista)
  };
  function pintarAcciones(lista) {
    foot.innerHTML = '';
    for (const a of lista) {
      const b = document.createElement('button');
      b.className = 'btn ' + (a.clase || '');
      b.textContent = a.texto;
      b.type = 'button';
      b.addEventListener('click', async () => {
        if (!a.onClick) return cerrar();
        ctx.error('');
        b.disabled = true;
        try {
          const r = await a.onClick(ctx);
          if (r !== false) cerrar();
        } catch (e) {
          ctx.error(e.message);
          marcarInvalidos(body, e.message);
        } finally { b.disabled = false; }
      });
      foot.appendChild(b);
    }
  }
  pintarAcciones(acciones.length ? acciones : [{ texto: 'Cerrar' }]);
  back.querySelector('[data-cerrar]').addEventListener('click', cerrar);
  back.addEventListener('keydown', e => { if (e.key === 'Escape') cerrar(); });
  root.appendChild(back);
  const primero = back.querySelector('input, select, textarea');
  if (primero) primero.focus();
  if (alAbrir) alAbrir(ctx);
  return ctx;
}

/** HU-ADM-01 AC4: resalta los campos vacios u obligatorios cuando hay error de validacion. */
function marcarInvalidos(body, _msg) {
  body.querySelectorAll('input, select, textarea').forEach(i => {
    i.classList.toggle('invalido', !i.checkValidity());
  });
}

export function confirmar(titulo, mensaje, textoOk = 'Confirmar', clase = 'btn-primary') {
  return new Promise(resolve => {
    const ctx = modal({
      titulo,
      contenido: `<p>${esc(mensaje)}</p>`,
      acciones: [
        { texto: 'Cancelar', onClick: () => { resolve(false); } },
        { texto: textoOk, clase, onClick: () => { resolve(true); } }
      ]
    });
    ctx.raiz.querySelector('[data-cerrar]').addEventListener('click', () => resolve(false));
  });
}

/** Lee un formulario dentro de un contenedor: name -> valor (numeros convertidos). */
export function leerForm(cont) {
  const datos = {};
  let valido = true;
  cont.querySelectorAll('[name]').forEach(el => {
    if (!el.checkValidity()) { valido = false; el.classList.add('invalido'); } else el.classList.remove('invalido');
    let v = el.type === 'checkbox' ? el.checked : el.value.trim();
    if (el.type === 'number' && v !== '') v = Number(v);
    if (v === '') v = null;
    datos[el.name] = v;
  });
  if (!valido) throw new Error('Revise los campos resaltados');
  return datos;
}

export function opciones(lista, valor, etiqueta, seleccionado, vacio) {
  return (vacio !== undefined ? `<option value="">${esc(vacio)}</option>` : '') +
    lista.map(x => `<option value="${esc(x[valor])}" ${String(x[valor]) === String(seleccionado ?? '') ? 'selected' : ''}>${esc(x[etiqueta])}</option>`).join('');
}

export function vacio(msg) {
  return `<div class="empty">${esc(msg)}</div>`;
}

export function cargando(panel) {
  panel.innerHTML = '<p class="muted">Cargando…</p>';
}

/** Imprime un comprobante: solo se ve el contenido de #print-area. */
export function imprimir(htmlRecibo) {
  const area = document.getElementById('print-area');
  area.innerHTML = htmlRecibo;
  window.print();
  setTimeout(() => { area.innerHTML = ''; }, 500);
}

/** Subpestanas dentro de una vista. */
export function subtabs(cont, items, inicial) {
  cont.innerHTML = `<div class="subtabs">${items.map(i =>
    `<button class="subtab" data-sub="${i.id}">${esc(i.label)}</button>`).join('')}</div><div data-subpanel></div>`;
  const sub = cont.querySelector('[data-subpanel]');
  const ir = id => {
    cont.querySelectorAll('.subtab').forEach(b => b.classList.toggle('active', b.dataset.sub === id));
    const it = items.find(i => i.id === id);
    sub.innerHTML = '';
    it.render(sub);
  };
  cont.querySelectorAll('.subtab').forEach(b => b.addEventListener('click', () => ir(b.dataset.sub)));
  ir(inicial || items[0].id);
}

// Tareas periodicas de la pestana activa (se limpian al cambiar de pestana)
const intervalos = [];
export function cadaTanto(fn, ms) { intervalos.push(setInterval(fn, ms)); }
export function limpiarIntervalos() { while (intervalos.length) clearInterval(intervalos.pop()); }
