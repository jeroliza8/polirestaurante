import { state, sesionVigente } from '../state.js';
import { get, patch } from '../api.js';
import { esc, fecha, toast, modal } from '../ui.js';
import { iniciarPagina } from '../shell.js';
import { conectarEventosCocina } from '../cocina-eventos.js';

const ESTADOS = [
  { id: 'PENDIENTE', nombre: 'Pendientes', icono: '○', clase: 'pendiente', accion: 'Iniciar preparación', siguiente: 'EN_PREPARACION' },
  { id: 'EN_PREPARACION', nombre: 'En preparación', icono: '◷', clase: 'preparacion', accion: 'Marcar como listo', siguiente: 'LISTO' },
  { id: 'LISTO', nombre: 'Listos', icono: '✓', clase: 'listo' }
];
const etiquetas = { PENDIENTE: 'Pendiente', EN_PREPARACION: 'En preparación', LISTO: 'Listo', ENTREGADO: 'Entregado', CANCELADO: 'Cancelado', PAGADO: 'Pagado' };
let datos = null, panel = null, vista = 'pedidos', errorActual = '', cargando = false, volverACargar = false;
const filtros = { pedidos: '', productos: '', categoria: '', disponible: 'todos' };
const escFecha = v => esc(v ? fecha(v) : 'Sin registro');
const lugar = p => p.mesas?.length ? 'Mesa ' + p.mesas.join(', ') : ({ DOMICILIO: 'Domicilio', PARA_LLEVAR: 'Para llevar' }[p.tipoPedido] || p.tipoPedido || 'Sin mesa');
const normal = s => String(s ?? '').normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase();
const coincide = (consulta, ...valores) => normal(valores.join(' ')).includes(normal(consulta));

function listarItems(p) {
  if (!p.items?.length) return '<p class="ck-empty-note">No hay productos activos en este pedido.</p>';
  return `<ul class="ck-items">${p.items.map(d => `<li><span class="ck-cantidad">${esc(d.cantidad)}×</span><div><strong>${esc(d.nombre)}</strong>${d.notas ? `<p class="ck-nota">${esc(d.notas)}</p>` : ''}</div></li>`).join('')}</ul>`;
}
function tarjeta(p, e) {
  return `<article class="ck-ticket ${e.clase}">
    <div class="ck-ticket-top"><strong>Pedido #${esc(p.id)}</strong><span class="ck-badge ${e.clase}">${esc(etiquetas[p.estado])}</span></div>
    <div class="ck-ticket-meta"><span>${esc(lugar(p))}</span><span>${escFecha(p.fechaCreacion)}</span></div>
    <div class="ck-ticket-mesero">Mesero: ${esc(p.mesero)}</div>
    ${listarItems(p)}
    <div class="ck-ticket-actions"><button type="button" class="btn btn-ghost ck-detail" data-detalle="${esc(p.id)}">Ver detalle</button>
      ${e.siguiente ? `<button type="button" class="btn ck-action ${e.clase}" data-estado="${e.siguiente}" data-id="${esc(p.id)}">${e.accion}</button>` : '<span class="ck-ready">Disponible para recoger</span>'}</div>
  </article>`;
}
function dibujarPedidos(contenido) {
  const todos = datos?.pedidos || [];
  const filtrados = todos.filter(p => coincide(filtros.pedidos, p.id, '#' + p.id, lugar(p), p.mesero, ...(p.items || []).map(x => x.nombre)));
  contenido.innerHTML = `<div class="ck-board">${ESTADOS.map(e => {
    const grupo = filtrados.filter(p => p.estado === e.id);
    const total = todos.filter(p => p.estado === e.id).length;
    return `<section class="ck-columna ${e.clase}"><div class="ck-col-head"><span class="ck-state-icon" aria-hidden="true">${e.icono}</span><h2>${e.nombre}</h2><span class="ck-count">${grupo.length}${grupo.length !== total ? '/' + total : ''}</span></div>
      <div class="ck-tickets">${grupo.length ? grupo.map(p => tarjeta(p, e)).join('') : `<div class="ck-empty"><span aria-hidden="true">${e.icono}</span><p>${filtros.pedidos ? 'Sin coincidencias' : e.id === 'PENDIENTE' ? 'No hay pedidos por preparar' : e.id === 'EN_PREPARACION' ? 'No hay pedidos en preparación' : 'No hay pedidos listos'}</p></div>`}</div></section>`;
  }).join('')}</div>`;
  panel.querySelector('[data-resumen]').textContent = `${todos.length} pedidos activos · Orden de llegada`;
}
function tarjetaProducto(p) {
  const stock = p.cantidadDisponible ?? 0;
  const agotado = !p.disponibilidadEfectiva;
  const reactivar = !p.disponible;
  return `<article class="ck-producto ${agotado ? 'agotado' : 'disponible'}">
    <div class="ck-producto-top"><div><h3>${esc(p.nombre)}</h3><p>${esc(p.categoria)}</p></div><span class="ck-badge ${agotado ? 'agotado' : 'listo'}">${agotado ? 'Agotado' : 'Disponible'}</span></div>
    <div class="ck-stock"><span>Existencias registradas</span><strong>${esc(stock)}</strong></div>
    ${p.fechaDisponibilidad ? `<p class="ck-auditoria">Último cambio de Cocina: ${escFecha(p.fechaDisponibilidad)} · Usuario #${esc(p.usuarioDisponibilidad)}</p>` : '<p class="ck-auditoria">Sin cambios de Cocina registrados</p>'}
    ${reactivar && stock <= 0 ? '<p class="ck-stock-alert">Se requiere ingreso de mercancía.</p>' : ''}
    <button type="button" class="btn ck-action ${reactivar ? 'preparacion' : 'agotado'}" data-producto="${esc(p.id)}" data-disponible="${reactivar}" ${reactivar && stock <= 0 ? 'disabled' : ''}>${reactivar ? 'Reactivar disponibilidad' : 'Marcar como agotado'}</button>
  </article>`;
}
function dibujarProductos(contenido) {
  const todos = datos?.productos || [];
  const categorias = [...new Set(todos.map(p => p.categoria || 'Sin categoría'))].sort((a,b) => a.localeCompare(b,'es'));
  const selector = panel.querySelector('[data-categoria]');
  if (!categorias.includes(filtros.categoria)) filtros.categoria = '';
  const opciones = '<option value="">Todas las categorías</option>' + categorias.map(c => `<option value="${esc(c)}">${esc(c)}</option>`).join('');
  if (selector.dataset.opciones !== opciones) { selector.innerHTML = opciones; selector.dataset.opciones = opciones; }
  selector.value = filtros.categoria;
  const lista = todos.filter(p => coincide(filtros.productos, p.nombre, p.categoria) && (!filtros.categoria || (p.categoria || 'Sin categoría') === filtros.categoria) &&
    (filtros.disponible === 'todos' || (filtros.disponible === 'disponible') === p.disponibilidadEfectiva));
  panel.querySelector('[data-resumen]').textContent = `${todos.filter(p => p.disponibilidadEfectiva).length} disponibles · ${todos.filter(p => !p.disponibilidadEfectiva).length} agotados`;
  contenido.innerHTML = lista.length ? `<div class="ck-productos">${lista.map(tarjetaProducto).join('')}</div>` : '<div class="ck-empty"><p>No hay productos que coincidan con los filtros.</p></div>';
}
function dibujar() {
  if (!panel?.isConnected) return;
  const contenido = panel.querySelector('[data-contenido]');
  const error = panel.querySelector('[data-error]');
  if (!contenido || !error) return;
  error.hidden = !errorActual;
  error.querySelector('span').textContent = errorActual;
  if (!datos) { contenido.innerHTML = `<div class="ck-empty"><p>${errorActual ? 'No se pudo consultar el servidor.' : 'Cargando información…'}</p></div>`; return; }
  if (vista === 'pedidos') dibujarPedidos(contenido); else dibujarProductos(contenido);
}
async function cargar() {
  if (!sesionVigente()) return;
  if (cargando) { volverACargar = true; return; }
  cargando = true;
  try {
    do {
      volverACargar = false;
      try { datos = await get('/cocinero/tablero'); errorActual = ''; }
      catch (e) { errorActual = e.message || 'No se pudo cargar el tablero.'; }
      dibujar();
    } while (volverACargar && sesionVigente());
  } finally { cargando = false; }
}
async function detalle(id) {
  try {
    const p = await get(`/cocinero/pedidos/${id}`);
    modal({ titulo: `Pedido #${p.id} · ${etiquetas[p.estado] || p.estado}`, grande: true,
      contenido: `<div class="ck-detail-info"><p><strong>Ubicación:</strong> ${esc(lugar(p))}</p><p><strong>Mesero:</strong> ${esc(p.mesero)}</p><p><strong>Recibido:</strong> ${escFecha(p.fechaCreacion)}</p><p><strong>Inicio de preparación:</strong> ${escFecha(p.fechaEnPreparacion)} ${p.usuarioPreparacion ? '· Usuario #' + esc(p.usuarioPreparacion) : ''}</p><p><strong>Listo:</strong> ${escFecha(p.fechaListo)} ${p.usuarioListo ? '· Usuario #' + esc(p.usuarioListo) : ''}</p></div>${listarItems(p)}`,
      acciones: [{ texto: 'Cerrar', clase: 'btn-ghost' }] });
  } catch (e) { toast(e.message, 'error'); await cargar(); }
}
function cambiarEstado(id, estado) {
  const p = datos?.pedidos.find(x => x.id === id);
  if (!p) return;
  const listo = estado === 'LISTO';
  modal({ titulo: listo ? 'Finalizar preparación' : 'Iniciar preparación',
    contenido: `<p>${listo ? '¿Todos los productos del pedido están listos?' : '¿Desea iniciar la preparación de este pedido?'}</p><p class="muted">Pedido #${esc(id)} · ${esc(lugar(p))}</p>`,
    acciones: [{ texto: 'Cancelar', clase: 'btn-ghost' }, { texto: listo ? 'Marcar como listo' : 'Iniciar preparación', clase: 'btn-primary', onClick: async () => {
      try { await patch(`/cocinero/pedidos/${id}/estado`, { estado }); toast(listo ? 'Pedido listo. Estado actualizado.' : 'Preparación iniciada.'); }
      finally { await cargar(); }
    }}] });
}
function cambiarProducto(id, disponible) {
  const p = datos?.productos.find(x => x.id === id);
  if (!p) return;
  modal({ titulo: disponible ? 'Reactivar producto' : 'Marcar producto como agotado',
    contenido: `<p>¿Confirma ${disponible ? 'reactivar' : 'marcar como agotado'} <strong>${esc(p.nombre)}</strong>?</p><p class="muted">Los pedidos existentes no serán modificados. La disponibilidad se comparte con Mesero y Administración.</p>`,
    acciones: [{ texto: 'Cancelar', clase: 'btn-ghost' }, { texto: disponible ? 'Reactivar' : 'Marcar agotado', clase: disponible ? 'btn-primary' : 'btn-danger', onClick: async () => {
      try { await patch(`/cocinero/productos/${id}/disponibilidad`, { disponible }); toast(disponible ? 'Producto disponible nuevamente.' : 'Producto marcado como agotado.'); }
      finally { await cargar(); }
    }}] });
}
function mostrar(cont, id) {
  panel = cont; vista = id;
  const pedidos = id === 'pedidos';
  panel.innerHTML = `<div class="ck-page-head"><div><h1>${pedidos ? 'Tablero de cocina' : 'Disponibilidad de productos'}</h1><p>${pedidos ? 'Gestiona las comandas y el estado de preparación.' : 'Controla qué productos pueden ofrecerse en nuevos pedidos.'}</p></div><button type="button" class="btn btn-ghost ck-refresh" data-refrescar>↻ Actualizar</button></div>
    <div class="ck-toolbar"><label class="ck-search"><span class="ck-sr">Buscar</span><input type="search" data-buscar placeholder="${pedidos ? 'Buscar pedido, mesa o producto…' : 'Buscar producto…'}" value="${esc(filtros[id])}"></label>
      ${pedidos ? '' : '<label><span class="ck-sr">Categoría</span><select data-categoria><option value="">Todas las categorías</option></select></label><label><span class="ck-sr">Disponibilidad</span><select data-disponible><option value="todos">Todos los estados</option><option value="disponible">Disponibles</option><option value="agotado">Agotados</option></select></label>'}<span class="ck-resumen" data-resumen></span></div>
    <div class="ck-error" data-error role="alert" hidden><span></span><button type="button" class="btn btn-ghost btn-sm" data-refrescar>Reintentar</button></div><div data-contenido></div>`;
  panel.querySelector('[data-buscar]').addEventListener('input', e => { filtros[id] = e.target.value; dibujar(); });
  if (!pedidos) {
    panel.querySelector('[data-categoria]').addEventListener('change', e => { filtros.categoria = e.target.value; dibujar(); });
    const sel = panel.querySelector('select[data-disponible]');
    sel.value = filtros.disponible;
    sel.addEventListener('change', e => { filtros.disponible = e.target.value; dibujar(); });
  }
  // El shell reutiliza el mismo elemento #panel al cambiar pestañas; se reemplaza el handler, no se acumula.
  panel.onclick = e => {
    const b = e.target.closest('button');
    if (!b || b.disabled) return;
    if (b.hasAttribute('data-refrescar')) cargar();
    else if (b.dataset.detalle) detalle(Number(b.dataset.detalle));
    else if (b.dataset.estado) cambiarEstado(Number(b.dataset.id), b.dataset.estado);
    else if (b.dataset.producto) cambiarProducto(Number(b.dataset.producto), b.dataset.disponible === 'true');
  };
  dibujar(); cargar();
}
if (sesionVigente() && state.rol === 'COCINERO') {
  iniciarPagina({ rol: 'COCINERO', raiz: 'Cocina', inicial: 'pedidos', menu: [
    { id: 'pedidos', label: 'Pedidos', icono: '▤', render: p => mostrar(p, 'pedidos') },
    { id: 'productos', label: 'Disponibilidad', icono: '▦', render: p => mostrar(p, 'productos') }
  ] });
  document.getElementById('rol-badge').textContent = 'Cocinero';
  conectarEventosCocina({ alEstado: estado => {
    const el = document.getElementById('ck-connection');
    el.dataset.estado = estado;
    el.textContent = estado === 'conectado' ? 'En tiempo real' : estado === 'conectando' ? 'Conectando…' : 'Sin conexión en vivo';
  }, alEvento: evento => {
    if (evento.tipo === 'SINCRONIZACION_ERROR') {
      errorActual = 'El servidor no pudo sincronizar el tablero. Reintente la consulta.'; dibujar();
    } else if (['CONECTADO','TABLERO_CAMBIO','PEDIDO_ACTUALIZADO','PEDIDO_LISTO','DISPONIBILIDAD_ACTUALIZADA'].includes(evento.tipo)) cargar();
  }});
} else location.replace('index.html');
