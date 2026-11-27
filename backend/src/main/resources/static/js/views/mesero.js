// Vista del mesero (RF01-RF06). Consume /api/mesero/** y se refresca con los avisos de Cocina (Observer).
import { state, sesionVigente } from '../state.js';
import { get, post, patch, del } from '../api.js';
import { esc, money, fecha, toast, errorToast, modal, estado } from '../ui.js';
import { iniciarPagina } from '../shell.js';

let datos = { mesas: [], menu: [], pedidos: [] };
let errorActual = '', cargando = false, volverACargar = false, vista = 'mesas', panel = null;
let borrador = { mesaId: null, lineas: [] };          // pedido en construcción (sobrevive al cambio de pestaña)
const filtros = { texto: '', categoria: '' };
let pedidoAbierto = null;                              // id del pedido que se muestra en el modal
let repintarModal = null, cerrarDetalle = null;

const normal = s => String(s ?? '').normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase();
const irA = id => document.querySelector(`#menu [data-tab="${id}"]`)?.click();
const ETIQUETA = { PENDIENTE: 'Pendiente', EN_PREPARACION: 'En preparación', LISTO: 'Listo para entregar',
  ENTREGADO: 'Entregado', CANCELADO: 'Cancelado', PAGADO: 'Pagado' };

// ------------------------------------------------------------------ carga

async function cargar() {
  if (!sesionVigente()) return;
  if (cargando) { volverACargar = true; return; }
  cargando = true;
  try {
    do {
      volverACargar = false;
      try {
        const [mesas, menu, pedidos] = await Promise.all([get('/mesero/mesas'), get('/mesero/menu'), get('/mesero/pedidos')]);
        datos = { mesas, menu, pedidos }; errorActual = '';
      } catch (e) { errorActual = e.message || 'No se pudo consultar el servidor.'; }
      dibujar();
      if (pedidoAbierto && repintarModal) repintarModal();
    } while (volverACargar && sesionVigente());
  } finally { cargando = false; }
}

async function accion(fn, ok) {
  try { const r = await fn(); if (ok) toast(ok); return r; }
  catch (e) { errorToast(e); return null; }
  finally { await cargar(); }
}

// ------------------------------------------------------------------ pestaña: mesas (RF06)

function tarjetaMesa(m) {
  const libre = m.estado === 'LIBRE';
  const info = m.pedidoId ? (m.cuentaPropia ? `Cuenta abierta: pedido #${m.pedidoId}` : 'Cuenta abierta de otro mesero')
    : (libre ? 'Disponible' : 'Ocupada, sin pedido');
  const botones = m.pedidoId
    ? (m.cuentaPropia ? `<button class="btn btn-primary btn-sm" data-ver="${m.pedidoId}">Ver pedido</button>` : '')
    : `<button class="btn btn-primary btn-sm" data-nuevo="${m.id}">Nuevo pedido</button>
       <button class="btn btn-ghost btn-sm" data-${libre ? 'ocupar' : 'liberar'}="${m.id}">${libre ? 'Marcar ocupada' : 'Liberar'}</button>`;
  return `<article class="ms-mesa ${libre ? 'libre' : 'ocupada'}">
    <header><h3>Mesa ${esc(m.numero)}</h3>${estado(m.estado)}</header>
    <div class="ms-mesa-info">${esc(m.capacidad)} puestos<br>${esc(info)}</div>
    <div class="ms-actions">${botones}</div></article>`;
}

function dibujarMesas(c) {
  const libres = datos.mesas.filter(m => m.estado === 'LIBRE').length;
  panel.querySelector('[data-resumen]').textContent = `${libres} libres · ${datos.mesas.length - libres} ocupadas`;
  c.innerHTML = datos.mesas.length ? `<div class="ms-mesas">${datos.mesas.map(tarjetaMesa).join('')}</div>`
    : '<div class="empty">No hay mesas activas.</div>';
}

// ------------------------------------------------------------------ pestaña: nuevo pedido (RF01)

const subtotal = () => borrador.lineas.reduce((s, l) => s + l.precio * l.cantidad, 0);
const enCarrito = id => borrador.lineas.find(l => l.productoId === id)?.cantidad || 0;

function dibujarMenu() {
  const cont = panel.querySelector('[data-menu]');
  if (!cont) return;
  const lista = datos.menu.filter(p => (!filtros.categoria || p.categoria === filtros.categoria) &&
    normal(p.nombre + ' ' + p.categoria).includes(normal(filtros.texto)));
  const sel = panel.querySelector('[data-categoria]');
  const cats = [...new Set(datos.menu.map(p => p.categoria))];
  const opc = '<option value="">Todas las categorías</option>' + cats.map(c => `<option value="${esc(c)}">${esc(c)}</option>`).join('');
  if (sel.dataset.opc !== opc) { sel.innerHTML = opc; sel.dataset.opc = opc; sel.value = filtros.categoria; }
  if (!lista.length) { cont.innerHTML = '<div class="empty">No hay productos disponibles que coincidan.</div>'; return; }
  const grupos = new Map();
  for (const p of lista) { if (!grupos.has(p.categoria)) grupos.set(p.categoria, []); grupos.get(p.categoria).push(p); }
  cont.innerHTML = [...grupos].map(([cat, items]) => `<h4 class="ms-categoria">${esc(cat)}</h4><div class="ms-menu">${items.map(p => {
    const restan = p.existencias - enCarrito(p.id);
    return `<div class="ms-prod"><strong>${esc(p.nombre)}</strong><small>${esc(p.descripcion || '')}</small>
      <div class="ms-prod-foot"><span>${money(p.precio)}</span>
      <button class="btn btn-sm btn-primary" data-agregar="${p.id}" ${restan <= 0 ? 'disabled' : ''}>${restan <= 0 ? 'Sin más' : 'Agregar'}</button></div></div>`;
  }).join('')}</div>`).join('');
}

function dibujarCarrito() {
  const cont = panel.querySelector('[data-carrito]');
  if (!cont) return;
  const libres = datos.mesas.filter(m => !m.pedidoId);
  if (borrador.mesaId && !libres.some(m => m.id === borrador.mesaId)) borrador.mesaId = null;
  cont.innerHTML = `<h3>Pedido nuevo</h3>
    <label>Mesa<br><select data-mesa><option value="">Seleccione una mesa…</option>${libres.map(m =>
      `<option value="${m.id}" ${m.id === borrador.mesaId ? 'selected' : ''}>Mesa ${esc(m.numero)} (${esc(m.capacidad)} puestos)</option>`).join('')}</select></label>
    ${borrador.lineas.length ? borrador.lineas.map((l, i) => `<div class="ms-linea">
        <div class="ms-linea-top"><strong>${esc(l.nombre)}</strong><span>${money(l.precio * l.cantidad)}</span></div>
        <div class="ms-linea-top"><div class="ms-qty"><button class="btn btn-sm" data-menos="${i}" aria-label="Quitar uno">−</button><span>${l.cantidad}</span>
          <button class="btn btn-sm" data-mas="${i}" aria-label="Agregar uno">+</button></div>
          <button class="btn btn-ghost btn-sm" data-sacar="${i}">Quitar</button></div>
        <input type="text" maxlength="200" data-nota="${i}" placeholder="Notas (sin cebolla, término…)" value="${esc(l.notas)}">
      </div>`).join('') : '<p class="muted">Agregue productos de la carta.</p>'}
    <div class="ms-total"><span>Total</span><span>${money(subtotal())}</span></div>
    <button class="btn btn-success btn-block" data-confirmar ${borrador.lineas.length && borrador.mesaId ? '' : 'disabled'}>Confirmar pedido</button>
    ${borrador.lineas.length ? '<button class="btn btn-ghost btn-block" data-vaciar>Vaciar</button>' : ''}`;
}

function mostrarNuevo() {
  panel.innerHTML = `<div class="ms-head"><div><h1>Nuevo pedido</h1><p>Elija la mesa y los productos. Solo se muestran los que cocina tiene disponibles.</p></div></div>
    <div class="ms-pedido-grid"><section><div class="ms-toolbar">
      <input type="search" data-buscar placeholder="Buscar producto…" value="${esc(filtros.texto)}" aria-label="Buscar producto">
      <select data-categoria aria-label="Categoría"></select></div><div data-menu></div></section>
      <aside class="ms-card ms-carrito" data-carrito></aside></div>`;
  panel.querySelector('[data-buscar]').addEventListener('input', e => { filtros.texto = e.target.value; dibujarMenu(); });
  panel.querySelector('[data-categoria]').addEventListener('change', e => { filtros.categoria = e.target.value; dibujarMenu(); });
  dibujarMenu(); dibujarCarrito();
}

function agregarAlCarrito(id) {
  const p = datos.menu.find(x => x.id === id);
  if (!p) return;
  const l = borrador.lineas.find(x => x.productoId === id);
  if (l) { if (l.cantidad < p.existencias) l.cantidad++; else toast(`Solo hay ${p.existencias} de ${p.nombre}.`, 'error'); }
  else borrador.lineas.push({ productoId: id, nombre: p.nombre, precio: p.precio, cantidad: 1, notas: '' });
  dibujarMenu(); dibujarCarrito();
}

async function confirmarPedido() {
  const r = await accion(() => post('/mesero/pedidos', {
    mesaId: borrador.mesaId,
    lineas: borrador.lineas.map(l => ({ productoId: l.productoId, cantidad: l.cantidad, notas: l.notas || null }))
  }), 'Pedido creado y enviado a cocina.');
  if (r) { borrador = { mesaId: null, lineas: [] }; irA('pedidos'); }
}

// ------------------------------------------------------------------ pestaña: mis pedidos (RF02-RF05)

const lugar = p => p.mesaNumero ? `Mesa ${p.mesaNumero}` : 'Sin mesa';
const lineasVivas = p => p.lineas.filter(l => !l.anulado);

function tarjetaPedido(p) {
  return `<article class="ms-ticket ${p.estado === 'LISTO' ? 'listo' : ''}">
    <header><strong>Pedido #${esc(p.id)} · ${esc(lugar(p))}</strong>${estado(p.estado, ETIQUETA[p.estado])}</header>
    <small class="muted">${esc(fecha(p.fechaCreacion))}</small>
    <ul>${lineasVivas(p).map(l => `<li><span>${esc(l.cantidad)}× ${esc(l.nombre)}</span><span>${money(l.subtotal)}</span></li>`).join('')}</ul>
    <div class="ms-total"><span>Total</span><span>${money(p.total)}</span></div>
    <div class="ms-actions"><button class="btn btn-sm" data-ver="${p.id}">Ver / editar</button>
      ${p.entregable ? `<button class="btn btn-sm btn-success" data-entregar="${p.id}">Marcar entregado</button>` : ''}
      ${p.cancelable ? `<button class="btn btn-sm btn-danger" data-cancelar="${p.id}">Cancelar</button>` : ''}</div></article>`;
}

function dibujarPedidos(c) {
  const abiertos = datos.pedidos.filter(p => !['PAGADO', 'CANCELADO'].includes(p.estado));
  const listos = datos.pedidos.filter(p => p.estado === 'LISTO').length;
  panel.querySelector('[data-resumen]').textContent = `${abiertos.length} abiertos${listos ? ` · ${listos} listos para entregar` : ''}`;
  c.innerHTML = datos.pedidos.length ? `<div class="ms-pedidos">${datos.pedidos.map(tarjetaPedido).join('')}</div>`
    : '<div class="empty">Aún no tiene pedidos. Cree uno desde Mesas o Nuevo pedido.</div>';
}

function abrirPedido(id) {
  pedidoAbierto = id;
  const ctx = modal({ titulo: `Pedido #${id}`, grande: true, contenido: '<div data-pedido></div>', acciones: [{ texto: 'Cerrar', clase: 'btn-ghost' }] });
  const soltar = () => { pedidoAbierto = null; repintarModal = null; cerrarDetalle = null; };
  cerrarDetalle = () => { ctx.cerrar(); soltar(); };
  ctx.raiz.querySelector('[data-cerrar]').addEventListener('click', soltar);
  ctx.raiz.addEventListener('click', e => { if (e.target.closest('.modal-foot .btn')) soltar(); });
  const cont = ctx.body.querySelector('[data-pedido]');
  repintarModal = () => {
    const p = datos.pedidos.find(x => x.id === id);
    if (!p) { cont.innerHTML = '<p class="muted">Este pedido ya no está en su lista.</p>'; return; }
    const menuOpc = datos.menu.map(m => `<option value="${m.id}">${esc(m.nombre)} · ${money(m.precio)} (${m.existencias})</option>`).join('');
    cont.innerHTML = `<p>${esc(lugar(p))} · ${estado(p.estado, ETIQUETA[p.estado])} · <span class="muted">${esc(fecha(p.fechaCreacion))}</span></p>
      <ul class="ms-lista">${p.lineas.map(l => `<li class="${l.anulado ? 'ms-anulada' : ''}">
        <span>${esc(l.cantidad)}× ${esc(l.nombre)}${l.notas ? `<span class="ms-nota">${esc(l.notas)}</span>` : ''}${l.anulado ? `<span class="ms-nota">${esc(l.motivoAnulacion || 'Anulado')}</span>` : ''}</span>
        <span>${money(l.subtotal)} ${p.editable && !l.anulado ? `<button class="btn btn-ghost btn-sm" data-quitar="${l.id}">Quitar</button>` : ''}</span></li>`).join('')}</ul>
      <div class="ms-total"><span>Total</span><span>${money(p.total)}</span></div>
      ${p.editable ? `<div class="ms-modal-add"><select data-prod aria-label="Producto"><option value="">Agregar producto…</option>${menuOpc}</select>
        <input type="number" min="1" max="100000" value="1" data-cant aria-label="Cantidad"><input type="text" maxlength="200" placeholder="Notas" data-notas>
        <button class="btn btn-primary btn-sm" data-sumar>Agregar</button></div>` : '<p class="muted">Solo se editan pedidos pendientes.</p>'}
      <div class="ms-actions">${p.entregable ? '<button class="btn btn-success" data-m-entregar>Marcar entregado</button>' : ''}
        ${p.cancelable ? '<button class="btn btn-danger" data-m-cancelar>Cancelar pedido</button>' : ''}</div>`;
  };
  cont.onclick = async e => {
    const b = e.target.closest('button'); if (!b) return;
    if (b.dataset.quitar) await accion(() => del(`/mesero/pedidos/${id}/productos/${b.dataset.quitar}`), 'Producto retirado.');
    else if (b.hasAttribute('data-sumar')) {
      const productoId = Number(cont.querySelector('[data-prod]').value);
      if (!productoId) return toast('Elija un producto.', 'error');
      await accion(() => post(`/mesero/pedidos/${id}/productos`, { productoId, cantidad: Number(cont.querySelector('[data-cant]').value),
        notas: cont.querySelector('[data-notas]').value || null }), 'Producto agregado.');
    } else if (b.hasAttribute('data-m-entregar')) entregar(id);
    else if (b.hasAttribute('data-m-cancelar')) cancelar(id);
  };
  repintarModal();
}

function entregar(id) {
  modal({ titulo: 'Marcar como entregado', contenido: `<p>¿Entregó el pedido #${esc(id)} al cliente?</p>`,
    acciones: [{ texto: 'No', clase: 'btn-ghost' }, { texto: 'Sí, entregado', clase: 'btn-success',
      onClick: async () => { if (await accion(() => patch(`/mesero/pedidos/${id}/entregar`), 'Pedido entregado.')) cerrarDetalle?.(); } }] });
}

function cancelar(id) {
  modal({ titulo: 'Cancelar pedido', contenido: `<p>Se cancelará el pedido #${esc(id)}. Un pedido en preparación no devuelve existencias.</p>
      <label>Motivo (opcional)<br><textarea data-motivo rows="2" maxlength="200" style="width:100%"></textarea></label>`,
    acciones: [{ texto: 'Volver', clase: 'btn-ghost' }, { texto: 'Cancelar pedido', clase: 'btn-danger',
      onClick: async ctx => { if (await accion(() => patch(`/mesero/pedidos/${id}/cancelar`, { motivo: ctx.body.querySelector('[data-motivo]').value || null }), 'Pedido cancelado.')) cerrarDetalle?.(); } }] });
}

// ------------------------------------------------------------------ estructura común

function dibujar() {
  if (!panel?.isConnected) return;
  const err = panel.querySelector('[data-error]');
  if (err) { err.hidden = !errorActual; err.textContent = errorActual; }
  if (vista === 'nuevo') { dibujarMenu(); dibujarCarrito(); return; }
  const c = panel.querySelector('[data-contenido]');
  if (!c) return;
  if (vista === 'mesas') dibujarMesas(c); else dibujarPedidos(c);
}

function mostrar(cont, id) {
  panel = cont; vista = id;
  if (id === 'nuevo') mostrarNuevo();
  else {
    const mesas = id === 'mesas';
    panel.innerHTML = `<div class="ms-head"><div><h1>${mesas ? 'Mesas' : 'Mis pedidos'}</h1>
      <p>${mesas ? 'Estado del salón y apertura de cuentas.' : 'Estado actual de sus pedidos; se actualiza cuando cocina los avanza.'}</p></div>
      <div><span class="muted" data-resumen></span> <button class="btn btn-ghost" data-refrescar>↻ Actualizar</button></div></div>
      <div class="ms-error" data-error role="alert" hidden></div><div data-contenido></div>`;
  }
  // El shell reutiliza #panel: se reemplaza el manejador, no se acumula.
  panel.onclick = e => {
    const b = e.target.closest('button'); if (!b || b.disabled) return;
    const d = b.dataset;
    if ('refrescar' in d) cargar();
    else if (d.ver) abrirPedido(Number(d.ver));
    else if (d.nuevo) { borrador.mesaId = Number(d.nuevo); irA('nuevo'); }
    else if (d.ocupar) accion(() => patch(`/mesero/mesas/${d.ocupar}/ocupar`), 'Mesa marcada como ocupada.');
    else if (d.liberar) accion(() => patch(`/mesero/mesas/${d.liberar}/liberar`), 'Mesa liberada.');
    else if (d.entregar) entregar(Number(d.entregar));
    else if (d.cancelar) cancelar(Number(d.cancelar));
    else if (d.agregar) agregarAlCarrito(Number(d.agregar));
    else if (d.mas) { const l = borrador.lineas[d.mas]; const p = datos.menu.find(x => x.id === l.productoId); if (p && l.cantidad < p.existencias) l.cantidad++; dibujarMenu(); dibujarCarrito(); }
    else if (d.menos) { const l = borrador.lineas[d.menos]; if (--l.cantidad <= 0) borrador.lineas.splice(Number(d.menos), 1); dibujarMenu(); dibujarCarrito(); }
    else if (d.sacar) { borrador.lineas.splice(Number(d.sacar), 1); dibujarMenu(); dibujarCarrito(); }
    else if ('vaciar' in d) { borrador.lineas = []; dibujarMenu(); dibujarCarrito(); }
    else if ('confirmar' in d) confirmarPedido();
  };
  panel.oninput = e => { if (e.target.dataset.nota !== undefined) borrador.lineas[e.target.dataset.nota].notas = e.target.value; };
  panel.onchange = e => { if (e.target.matches('[data-mesa]')) { borrador.mesaId = Number(e.target.value) || null; dibujarCarrito(); } };
  dibujar(); cargar();
}

if (sesionVigente() && state.rol === 'MESERO') {
  iniciarPagina({ rol: 'MESERO', raiz: 'Mesero', inicial: 'mesas', menu: [
    { id: 'mesas', label: 'Mesas', icono: '▦', render: p => mostrar(p, 'mesas') },
    { id: 'nuevo', label: 'Nuevo pedido', icono: '＋', render: p => mostrar(p, 'nuevo') },
    { id: 'pedidos', label: 'Mis pedidos', icono: '▤', render: p => mostrar(p, 'pedidos') }
  ] });
  // Observer: el shell reenvía los avisos de Cocina (pedido listo, disponibilidad) como evento DOM.
  window.addEventListener('polirestaurante:cocina', () => cargar());
} else location.replace('index.html');
