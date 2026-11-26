// Portal del proveedor: solicitudes recibidas (RF35), historial de entregas (RF34),
// estado de pagos (RF36) y catalogo de insumos que suministra (RF34).
import { get, post, put } from '../api.js';
import { esc, money, fecha, badge, estado, toast, errorToast, confirmar, vacio, cargando, cadaTanto } from '../ui.js';

export const tabs = [
  { id: 'solicitudes', label: 'Solicitudes', render: renderSolicitudes },
  { id: 'entregas', label: 'Historial de entregas', render: renderEntregas },
  { id: 'pagos', label: 'Mis pagos', render: renderPagos },
  { id: 'catalogo', label: 'Mi catálogo', render: renderCatalogo }
];

const items = s => s.items.map(i => `${esc(i.insumo)} × ${i.cantidad} ${esc(i.unidadMedida)}`).join('<br>');

async function renderSolicitudes(panel) {
  const pintar = async () => {
    try {
      const [perfil, lista] = await Promise.all([get('/proveedor/perfil'), get('/proveedor/solicitudes')]);
      panel.innerHTML = `<div class="section-head"><div><h2>Solicitudes de pedido</h2>
          <span class="muted">${esc(perfil.nombre)} · ${esc(perfil.contacto)}</span></div></div>
        ${lista.length ? `<div class="grid grid-2">${lista.map(s => `
          <div class="card"><div class="card-title"><h3>Solicitud #${s.id}</h3>${estado(s.estado)}</div>
            <p class="muted small">Recibida ${fecha(s.fecha)}</p>
            <table><thead><tr><th>Insumo</th><th class="num">Cantidad</th><th class="num">Precio</th><th class="num">Subtotal</th></tr></thead>
            <tbody>${s.items.map(i => `<tr><td>${esc(i.insumo)}</td><td class="num">${i.cantidad} ${esc(i.unidadMedida)}</td>
              <td class="num">${money(i.precioUnitario)}</td><td class="num">${money(i.subtotal)}</td></tr>`).join('')}</tbody>
            <tfoot><tr><td colspan="3">Total</td><td class="num">${money(s.total)}</td></tr></tfoot></table>
            ${s.estado === 'PENDIENTE' ? `<div class="row-end" style="margin-top:.6rem">
              <button class="btn btn-primary" data-despachar="${s.id}">Marcar como despachada</button></div>`
              : '<p class="muted small" style="margin-top:.6rem">Despachada: el restaurante confirmará la recepción.</p>'}
          </div>`).join('')}</div>` : vacio('No tiene solicitudes pendientes')}`;
      panel.querySelectorAll('[data-despachar]').forEach(b => b.addEventListener('click', async () => {
        if (!await confirmar('Despachar solicitud', '¿Confirma que envió esta mercancía al restaurante?', 'Confirmar despacho')) return;
        try { await post(`/proveedor/solicitudes/${b.dataset.despachar}/despachar`); toast('Solicitud marcada como despachada'); pintar(); }
        catch (e) { errorToast(e); }
      }));
    } catch (e) { panel.innerHTML = `<div class="alert alert-error">${esc(e.message)}</div>`; }
  };
  cargando(panel);
  await pintar();
  cadaTanto(pintar, 60000);
}

async function renderEntregas(panel) {
  cargando(panel);
  try {
    const lista = await get('/proveedor/entregas');
    panel.innerHTML = `<div class="section-head"><h2>Historial de entregas realizadas</h2></div>
      ${lista.length ? `<div class="card"><div class="table-wrap"><table>
        <thead><tr><th>#</th><th>Solicitud</th><th>Recibida</th><th>Insumos</th><th class="num">Total</th><th>Pago</th></tr></thead>
        <tbody>${lista.map(s => `<tr><td>${s.id}</td><td>${fecha(s.fecha)}</td><td>${fecha(s.fechaRecepcion)}</td>
          <td class="small">${items(s)}</td><td class="num">${money(s.total)}</td>
          <td>${s.estadoPago === 'PAGADO' ? badge('Pagado', 'green') : badge('Pendiente', 'amber')}</td></tr>`).join('')}
        </tbody></table></div></div>` : vacio('Aún no hay entregas registradas')}`;
  } catch (e) { panel.innerHTML = `<div class="alert alert-error">${esc(e.message)}</div>`; }
}

async function renderPagos(panel) {
  cargando(panel);
  try {
    const r = await get('/proveedor/pagos');
    panel.innerHTML = `<div class="section-head"><h2>Estado de mis pagos</h2></div>
      <div class="grid grid-2" style="margin-bottom:1rem">
        <div class="kpi"><div class="label">Pendiente por cobrar</div><div class="value">${money(r.totalPendiente)}</div></div>
        <div class="kpi"><div class="label">Pagado</div><div class="value">${money(r.totalPagado)}</div></div></div>
      ${r.pagos.length ? `<div class="card"><table><thead><tr><th>#</th><th>Solicitud</th><th>Generado</th><th class="num">Monto</th><th>Estado</th><th>Fecha de pago</th></tr></thead>
        <tbody>${r.pagos.map(p => `<tr><td>${p.id}</td><td>#${p.solicitudId}</td><td>${fecha(p.fechaGenerado)}</td>
          <td class="num">${money(p.monto)}</td><td>${p.estado === 'PAGADO' ? badge('Pagado', 'green') : badge('Pendiente', 'amber')}</td>
          <td>${fecha(p.fechaPago)}</td></tr>`).join('')}</tbody></table></div>` : vacio('No hay pagos registrados')}`;
  } catch (e) { panel.innerHTML = `<div class="alert alert-error">${esc(e.message)}</div>`; }
}

async function renderCatalogo(panel) {
  cargando(panel);
  try {
    const lista = await get('/proveedor/catalogo');
    panel.innerHTML = `<div class="section-head"><h2>Mi catálogo</h2>
        <span class="muted small">Actualice el precio y la disponibilidad de los insumos que suministra.</span></div>
      ${lista.length ? `<div class="card"><table><thead><tr><th>Insumo</th><th>Unidad</th><th style="width:180px">Precio (COP)</th><th>Disponible</th><th></th></tr></thead>
        <tbody>${lista.map(i => `<tr data-rel="${i.idRelacion}"><td>${esc(i.insumo)}</td><td>${esc(i.unidadMedida)}</td>
          <td><input type="number" min="1" step="any" value="${i.precio ?? ''}" data-precio></td>
          <td><input type="checkbox" ${i.disponible ? 'checked' : ''} data-disp></td>
          <td class="right"><button class="btn btn-sm btn-primary" data-guardar>Guardar</button></td></tr>`).join('')}
        </tbody></table></div>` : vacio('El restaurante aún no le ha asignado insumos')}`;
    panel.querySelectorAll('[data-guardar]').forEach(b => b.addEventListener('click', async () => {
      const tr = b.closest('tr');
      const precio = Number(tr.querySelector('[data-precio]').value);
      if (!(precio > 0)) return errorToast(new Error('El precio debe ser mayor a 0'));
      try {
        await put('/proveedor/catalogo/' + tr.dataset.rel, { precio, disponible: tr.querySelector('[data-disp]').checked });
        toast('Catálogo actualizado');
      } catch (e) { errorToast(e); }
    }));
  } catch (e) { panel.innerHTML = `<div class="alert alert-error">${esc(e.message)}</div>`; }
}
