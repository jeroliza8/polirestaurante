// Modulo Cajero: caja (RF11), cuentas (RF12-RF15, RF17), promociones (CU-CAJ-002),
// anulacion de productos (CU-CAJ-003), pago y factura (CU-CAJ-001, HU-CAJ-001)
// e historial / reimpresion de facturas (HU-CAJ-002).
import { get, post, del, qs } from '../api.js';
import {
  esc, money, fecha, inputFecha, isoLocal, badge, estado, toast, errorToast, modal, confirmar, leerForm,
  vacio, cargando, imprimir, cadaTanto
} from '../ui.js';

export const tabs = [
  { id: 'cuentas', label: 'Cuentas', render: renderCuentas },
  { id: 'caja', label: 'Caja', render: renderCaja },
  { id: 'facturas', label: 'Historial de facturas', render: renderFacturas }
];

const MEDIOS = { EFECTIVO: 'Efectivo', TARJETA: 'Tarjeta', TRANSFERENCIA: 'Transferencia' };
const mesaTxt = c => (c.mesa ? `Mesa #${c.mesa}` : (c.tipoPedido === 'DOMICILIO' ? 'Domicilio' : 'Para llevar'));

async function cajaActual() {
  return get('/cajero/caja');   // null si no hay caja abierta (204)
}

// =====================================================================
// CAJA: apertura, resumen del turno y cierre (RF11)
// =====================================================================
async function renderCaja(panel) {
  cargando(panel);
  try {
    const [caja, historial] = await Promise.all([cajaActual(), get('/cajero/caja/historial')]);
    panel.innerHTML = `<div class="section-head"><h2>Caja</h2></div>
      <div data-actual></div>
      <div class="card" style="margin-top:1rem"><h3>Últimos turnos</h3>${historial.length ? `<div class="table-wrap"><table>
        <thead><tr><th>#</th><th>Apertura</th><th>Cierre</th><th class="num">Inicial</th><th class="num">Ventas</th><th class="num">Contado</th><th class="num">Diferencia</th><th>Estado</th></tr></thead>
        <tbody>${historial.map(c => `<tr><td>${c.idCaja}</td><td>${fecha(c.fechaApertura)}</td><td>${fecha(c.fechaCierre)}</td>
          <td class="num">${money(c.montoInicial)}</td><td class="num">${money(c.totalVentas)}</td>
          <td class="num">${c.montoFinal == null ? '-' : money(c.montoFinal)}</td>
          <td class="num">${c.diferencia == null ? '-' : money(c.diferencia)}</td><td>${estado(c.estado)}</td></tr>`).join('')}</tbody></table></div>`
        : vacio('Aún no hay turnos registrados')}</div>`;
    const cont = panel.querySelector('[data-actual]');
    if (!caja) {
      cont.innerHTML = `<div class="card"><h3>Apertura de caja</h3>
        <p class="muted">Registre el monto inicial (base) para empezar el turno. Sin caja abierta no se pueden cobrar cuentas.</p>
        <div class="inline-form"><label>Monto inicial (COP)<input name="monto" type="number" min="0" step="100" required value="200000"></label>
        <button class="btn btn-primary" data-abrir>Abrir caja</button></div></div>`;
      cont.querySelector('[data-abrir]').addEventListener('click', async () => {
        try {
          const d = leerForm(cont.querySelector('.inline-form'));
          await post('/cajero/caja/apertura', { monto: d.monto });
          toast('Caja abierta');
          renderCaja(panel);
        } catch (e) { errorToast(e); }
      });
      return;
    }
    cont.innerHTML = resumenCajaHtml(caja, true);
    cont.querySelector('[data-cerrar]').addEventListener('click', async () => {
      try {
        const d = leerForm(cont.querySelector('.inline-form'));
        if (!await confirmar('Cierre de caja', `Se cerrará el turno con ${money(d.monto)} contados. ¿Continuar?`, 'Cerrar caja')) return;
        const r = await post('/cajero/caja/cierre', { monto: d.monto });
        modal({ titulo: 'Reporte de cierre de caja', grande: true, contenido: resumenCajaHtml(r, false) });
        renderCaja(panel);
      } catch (e) { errorToast(e); }
    });
  } catch (e) { panel.innerHTML = `<div class="alert alert-error">${esc(e.message)}</div>`; }
}

function resumenCajaHtml(c, conCierre) {
  return `<div class="grid grid-4">
      <div class="kpi"><div class="label">Caja #${c.idCaja}</div><div class="value" style="font-size:1.1rem">${estado(c.estado)}</div>
        <div class="hint">Apertura ${fecha(c.fechaApertura)}${c.fechaCierre ? '<br>Cierre ' + fecha(c.fechaCierre) : ''}</div></div>
      <div class="kpi"><div class="label">Monto inicial</div><div class="value">${money(c.montoInicial)}</div></div>
      <div class="kpi"><div class="label">Ventas del turno</div><div class="value">${money(c.totalVentas)}</div><div class="hint">${c.numeroPagos} pago(s)</div></div>
      <div class="kpi"><div class="label">Efectivo esperado</div><div class="value">${money(c.efectivoEsperado)}</div>
        ${c.diferencia != null ? `<div class="hint">Diferencia: <strong>${money(c.diferencia)}</strong></div>` : ''}</div>
    </div>
    <div class="grid grid-2" style="margin-top:1rem">
      <div class="card"><h3>Ventas por medio de pago</h3>${c.ventasPorMedioPago.length ? `<table><tbody>${c.ventasPorMedioPago.map(s =>
        `<tr><td>${esc(MEDIOS[s.etiqueta] || s.etiqueta)}</td><td class="num">${s.cantidad}</td><td class="num">${money(s.valor)}</td></tr>`).join('')}</tbody></table>` : vacio('Sin ventas')}</div>
      <div class="card"><h3>Productos vendidos (C-producto)</h3>${c.productosVendidos.length ? `<table><tbody>${c.productosVendidos.map(p =>
        `<tr><td>${esc(p.producto)}</td><td class="num">${p.cantidad} und</td><td class="num">${money(p.monto)}</td></tr>`).join('')}</tbody></table>` : vacio('Sin productos vendidos')}</div>
    </div>
    ${conCierre ? `<div class="card" style="margin-top:1rem"><h3>Cierre de caja</h3>
      <div class="inline-form"><label>Efectivo contado (COP)<input name="monto" type="number" min="0" step="100" required></label>
      <button class="btn btn-primary" data-cerrar>Cerrar caja y generar reporte</button></div></div>` : ''}`;
}

// =====================================================================
// CUENTAS (RF12, RF13) con detalle, anulacion, promociones y cobro
// =====================================================================
async function renderCuentas(panel) {
  panel.innerHTML = `<div class="section-head"><h2>Cuentas pendientes</h2>
      <div class="row"><span class="muted small" data-act></span><button class="btn btn-sm" data-refrescar>Actualizar</button></div></div>
    <div data-aviso></div>
    <div class="split"><div class="list" data-lista></div><div data-detalle>${vacio('Seleccione una cuenta para ver su detalle')}</div></div>`;
  let seleccion = null;
  let cajaAbierta = false;

  const pintarLista = async () => {
    try {
      const [caja, cuentas] = await Promise.all([cajaActual(), get('/cajero/cuentas')]);
      cajaAbierta = Boolean(caja);
      panel.querySelector('[data-aviso]').innerHTML = cajaAbierta ? ''
        : '<div class="alert alert-warn" style="margin-bottom:1rem">La caja está cerrada. Ábrala en la pestaña <strong>Caja</strong> para anular productos, aplicar promociones o cobrar.</div>';
      panel.querySelector('[data-act]').textContent = 'Actualizado ' + new Date().toLocaleTimeString('es-CO');
      const lista = panel.querySelector('[data-lista]');
      lista.innerHTML = cuentas.length ? cuentas.map(c => `
        <button class="cuenta-item ${c.pedidoId === seleccion ? 'active' : ''}" data-id="${c.pedidoId}">
          <div class="row" style="justify-content:space-between"><strong>${esc(mesaTxt(c))}</strong>${estado(c.estado)}</div>
          <span class="muted small">Pedido #${c.pedidoId} · ${esc(c.mesero || '-')} · ${c.productos} producto(s)</span>
          <div class="row" style="justify-content:space-between"><span class="small">${c.promocion ? badge(c.promocion, 'blue') : ''}</span>
            <strong>${money(c.total)}</strong></div>
        </button>`).join('') : vacio('No hay cuentas pendientes');
      lista.querySelectorAll('[data-id]').forEach(b => b.addEventListener('click', () => {
        seleccion = Number(b.dataset.id);
        lista.querySelectorAll('.cuenta-item').forEach(x => x.classList.toggle('active', x === b));
        pintarDetalle();
      }));
      if (seleccion && !cuentas.some(c => c.pedidoId === seleccion)) {
        seleccion = null;
        panel.querySelector('[data-detalle]').innerHTML = vacio('Seleccione una cuenta para ver su detalle');
      }
    } catch (e) { errorToast(e); }
  };

  const pintarDetalle = async () => {
    const cont = panel.querySelector('[data-detalle]');
    try {
      const [c, promos] = await Promise.all([get('/cajero/cuentas/' + seleccion), get('/cajero/promociones' + qs({ pedidoId: seleccion }))]);
      cont.innerHTML = detalleHtml(c, promos, cajaAbierta);
      enlazarDetalle(cont, c);
    } catch (e) { cont.innerHTML = `<div class="alert alert-error">${esc(e.message)}</div>`; }
  };

  const enlazarDetalle = (cont, c) => {
    const tras = async (promesa, msg) => {
      try { await promesa; if (msg) toast(msg); await pintarLista(); if (seleccion) await pintarDetalle(); } catch (e) { errorToast(e); }
    };
    // CU-CAJ-003: anular producto con confirmacion y motivo
    cont.querySelectorAll('[data-anular]').forEach(b => b.addEventListener('click', () => {
      const it = c.items.find(i => i.detalleId == b.dataset.anular);
      modal({
        titulo: 'Confirmar anulación de producto',
        contenido: `<div class="alert alert-warn"><strong>${esc(it.producto)}</strong> × ${it.cantidad} — ${money(it.subtotal)}</div>
          <label>Motivo de la anulación *<input name="motivo" required minlength="3" maxlength="255" placeholder="Ej.: el cliente no lo recibió"></label>
          <p class="muted small">La anulación queda registrada para control interno y el total se recalcula.</p>`,
        acciones: [{ texto: 'Cancelar' }, { texto: 'Sí, anular producto', clase: 'btn-danger', onClick: async ({ body }) => {
          const { motivo } = leerForm(body);
          await post(`/cajero/cuentas/${c.pedidoId}/productos/${it.detalleId}/anular`, { motivo });
          toast('Producto anulado y total actualizado');
          pintarLista(); pintarDetalle();
        } }]
      });
    }));
    // CU-CAJ-002: aplicar / quitar promocion
    cont.querySelectorAll('[data-promo]').forEach(b => b.addEventListener('click', () =>
      tras(post(`/cajero/cuentas/${c.pedidoId}/promocion`, { codigo: b.dataset.promo }), 'Promoción aplicada')));
    cont.querySelector('[data-quitar-promo]')?.addEventListener('click', () =>
      tras(del(`/cajero/cuentas/${c.pedidoId}/promocion`), 'Promoción retirada'));
    // RF14: pre-cuenta con los productos entregados
    cont.querySelector('[data-precuenta]').addEventListener('click', async () => {
      try { imprimir(reciboPrecuenta(await get(`/cajero/cuentas/${c.pedidoId}/entregados`))); } catch (e) { errorToast(e); }
    });
    // RF17: cancelar cuenta no pagada
    cont.querySelector('[data-cancelar]')?.addEventListener('click', () => {
      modal({
        titulo: `Cancelar cuenta #${c.pedidoId}`,
        contenido: `<p>La cuenta quedará anulada y no podrá cobrarse.</p>
          <label>Motivo *<input name="motivo" required minlength="3" maxlength="255"></label>`,
        acciones: [{ texto: 'Volver' }, { texto: 'Cancelar cuenta', clase: 'btn-danger', onClick: async ({ body }) => {
          await post(`/cajero/cuentas/${c.pedidoId}/cancelar`, leerForm(body));
          toast('Cuenta cancelada');
          seleccion = null;
          panel.querySelector('[data-detalle]').innerHTML = vacio('Seleccione una cuenta para ver su detalle');
          pintarLista();
        } }]
      });
    });
    // CU-CAJ-001: cobrar
    cont.querySelector('[data-cobrar]')?.addEventListener('click', () => flujoPago(c, () => {
      seleccion = null;
      panel.querySelector('[data-detalle]').innerHTML = vacio('Seleccione una cuenta para ver su detalle');
      pintarLista();
    }));
  };

  panel.querySelector('[data-refrescar]').addEventListener('click', pintarLista);
  await pintarLista();
  cadaTanto(pintarLista, 30000);   // RF12/RF13: las cuentas nuevas de los meseros aparecen solas
}

function detalleHtml(c, promos, cajaAbierta) {
  const activos = c.items.filter(i => i.estado !== 'ANULADO');
  return `<div class="card">
    <div class="card-title"><div><h3>${esc(mesaTxt(c))} · Pedido #${c.pedidoId}</h3>
      <span class="muted small">Mesero ${esc(c.mesero || '-')} · ${fecha(c.fechaCreacion)}</span></div>${estado(c.estado)}</div>
    <div class="table-wrap"><table>
      <thead><tr><th>Producto</th><th class="num">Cant.</th><th class="num">P. unit.</th><th class="num">Subtotal</th><th>Estado</th><th></th></tr></thead>
      <tbody>${c.items.map(i => `<tr>
        <td class="${i.estado === 'ANULADO' ? 'tachado' : ''}">${esc(i.producto)}${i.notas ? `<div class="muted small">⚠ ${esc(i.notas)}</div>` : ''}
          ${i.motivoAnulacion ? `<div class="small" style="color:#fca5a5">Anulado: ${esc(i.motivoAnulacion)}</div>` : ''}</td>
        <td class="num">${i.cantidad}</td><td class="num">${money(i.precioUnitario)}</td>
        <td class="num ${i.estado === 'ANULADO' ? 'tachado' : ''}">${money(i.subtotal)}</td><td>${estado(i.estado)}</td>
        <td class="right">${i.estado !== 'ANULADO' && cajaAbierta ? `<button class="btn btn-sm btn-danger" data-anular="${i.detalleId}">Anular</button>` : ''}</td>
      </tr>`).join('')}</tbody></table></div>
    <div class="grid grid-2" style="margin-top:1rem">
      <div><h4>Descuentos y promociones</h4>
        ${c.promocion ? `<div class="alert alert-info"><strong>${esc(c.promocion.nombre)}</strong> (${c.promocion.porcentaje}%)
            ${c.promocion.categoria ? ' en ' + esc(c.promocion.categoria) : ''}
            ${cajaAbierta ? '<button class="btn btn-sm" data-quitar-promo style="margin-left:.5rem">Quitar</button>' : ''}</div>`
          : promos.length ? `<div class="stack">${promos.map(p => `<div class="row" style="justify-content:space-between">
              <span>${esc(p.nombre)} <span class="muted small">${p.porcentaje}%${p.categoria ? ' en ' + esc(p.categoria) : ''} · ahorra ${money(p.descuentoEstimado)}</span></span>
              ${cajaAbierta ? `<button class="btn btn-sm" data-promo="${esc(p.codigo)}">Aplicar</button>` : ''}</div>`).join('')}</div>`
          : '<p class="muted small">Sin promociones disponibles para esta cuenta. Se mantiene el valor original.</p>'}
      </div>
      <div class="totales">
        <div><span class="muted">Subtotal</span><span>${money(c.subtotal)}</span></div>
        <div><span class="muted">Descuento</span><span>${c.descuento ? '- ' + money(c.descuento) : '-'}</span></div>
        <div><span class="muted">${esc(c.impuestoNombre)} incluido</span><span>${money(c.impuesto)}</span></div>
        <div class="grand"><span>Total a cobrar</span><span>${money(c.total)}</span></div>
      </div>
    </div>
    <div class="row-end" style="margin-top:1rem">
      <button class="btn" data-precuenta>Imprimir pre-cuenta (entregados)</button>
      ${cajaAbierta ? '<button class="btn btn-danger" data-cancelar>Cancelar cuenta</button>' : ''}
      ${cajaAbierta && activos.length ? '<button class="btn btn-primary" data-cobrar>Cobrar</button>' : ''}
    </div></div>`;
}

// ---------------- Flujo de pago y factura (CU-CAJ-001) ----------------
function flujoPago(cuenta, alTerminar) {
  let medio = 'EFECTIVO';
  const ctx = modal({
    titulo: `Pago · ${mesaTxt(cuenta)} · Pedido #${cuenta.pedidoId}`,
    contenido: `<div class="totales"><div class="grand"><span>Total a cobrar</span><span>${money(cuenta.total)}</span></div></div>
      <h4>Medio de pago</h4>
      <div class="opciones-pago">${Object.entries(MEDIOS).map(([k, v]) =>
        `<button type="button" class="opcion ${k === medio ? 'active' : ''}" data-medio="${k}">${v}</button>`).join('')}</div>
      <div data-campos></div><div data-resultado></div>`,
    acciones: []
  });
  const body = ctx.body;
  const campos = () => {
    const c = body.querySelector('[data-campos]');
    if (medio === 'EFECTIVO') {
      c.innerHTML = `<label>Monto recibido (COP) *<input name="montoRecibido" type="number" min="${cuenta.total}" step="100" required value="${Math.ceil(cuenta.total / 1000) * 1000}"></label>
        <p class="muted small" data-cambio></p>`;
      const inp = c.querySelector('input');
      const cambio = () => { c.querySelector('[data-cambio]').textContent = 'Cambio: ' + money(Math.max(0, (Number(inp.value) || 0) - cuenta.total)); };
      inp.addEventListener('input', cambio); cambio();
    } else if (medio === 'TARJETA') {
      c.innerHTML = `<label>Últimos 4 dígitos de la tarjeta *<input name="referencia" required pattern="\\d{4}" maxlength="4" inputmode="numeric" placeholder="1234"></label>
        <p class="muted small">Por seguridad solo se registran los 4 últimos dígitos. (Prueba de rechazo: 0000)</p>`;
    } else {
      c.innerHTML = `<label>Referencia de la transferencia *<input name="referencia" required minlength="4" maxlength="40"></label>
        <p class="muted small">(Prueba de rechazo: una referencia que empiece por RECH)</p>`;
    }
  };
  body.querySelectorAll('[data-medio]').forEach(b => b.addEventListener('click', () => {
    medio = b.dataset.medio;
    body.querySelectorAll('[data-medio]').forEach(x => x.classList.toggle('active', x === b));
    body.querySelector('[data-resultado]').innerHTML = '';
    campos();
  }));
  campos();

  ctx.setAcciones([
    { texto: 'Cancelar' },
    { texto: 'Procesar pago', clase: 'btn-primary', onClick: async () => {
      const d = leerForm(body.querySelector('[data-campos]'));
      const res = body.querySelector('[data-resultado]');
      try {
        const pago = await post('/cajero/pagos', { pedidoId: cuenta.pedidoId, medioPago: medio,
          montoRecibido: d.montoRecibido ?? null, referencia: d.referencia ?? null });
        mostrarAprobado(ctx, pago, alTerminar);
        return false;
      } catch (e) {
        if (e.status === 422) {   // AC4: pago rechazado -> se permite reintentar
          res.innerHTML = `<div class="alert alert-error"><strong>Pago rechazado.</strong> ${esc(e.message.replace(/^Pago rechazado:\s*/, ''))}
            <div class="small">Puede reintentar con el mismo medio o elegir otro.</div></div>`;
          return false;
        }
        throw e;
      }
    } }
  ]);
}

function mostrarAprobado(ctx, pago, alTerminar) {
  alTerminar();
  ctx.body.innerHTML = `<div class="alert alert-ok"><h3>✓ Pago aprobado</h3>
      <div>${money(pago.total)} · ${esc(MEDIOS[pago.medioPago])}${pago.referencia ? ' · ' + esc(pago.referencia) : ''}</div>
      ${pago.cambio != null ? `<div>Recibido ${money(pago.montoRecibido)} · <strong>Cambio ${money(pago.cambio)}</strong></div>` : ''}</div>
    <h4>Seleccione el comprobante</h4>
    <div class="opciones-pago" style="grid-template-columns:1fr 1fr">
      <button type="button" class="opcion active" data-tipo="FISICA">Factura física</button>
      <button type="button" class="opcion" data-tipo="ELECTRONICA">Factura electrónica</button></div>
    <div data-correo class="hidden"><label>Correo del cliente *<input name="correoCliente" type="email" maxlength="100"></label></div>
    <p class="form-error" data-error></p>`;
  let tipo = 'FISICA';
  ctx.body.querySelectorAll('[data-tipo]').forEach(b => b.addEventListener('click', () => {
    tipo = b.dataset.tipo;
    ctx.body.querySelectorAll('[data-tipo]').forEach(x => x.classList.toggle('active', x === b));
    ctx.body.querySelector('[data-correo]').classList.toggle('hidden', tipo !== 'ELECTRONICA');
  }));
  const err = ctx.body.querySelector('[data-error]');
  ctx.error = m => { err.textContent = m || ''; };
  ctx.setAcciones([
    { texto: 'Más tarde' },
    { texto: 'Generar factura', clase: 'btn-primary', onClick: async () => {
      const correo = ctx.body.querySelector('[name=correoCliente]').value.trim();
      const f = await post(`/cajero/pagos/${pago.pagoId}/factura`, { tipoFactura: tipo, correoCliente: correo || null });
      mostrarFactura(f);
    } }
  ]);
}

function mostrarFactura(f) {
  modal({
    titulo: `Factura ${f.numeroFactura}`,
    contenido: `${f.advertencia ? `<div class="alert alert-warn">${esc(f.advertencia)}</div>` : ''}
      ${f.tipoFactura === 'ELECTRONICA' && f.codigoFacturaElectronica ? `<div class="alert alert-ok">Factura electrónica enviada a ${esc(f.correoCliente)}</div>` : ''}
      ${reciboFactura(f)}`,
    acciones: [{ texto: 'Cerrar' }, { texto: 'Imprimir', clase: 'btn-primary', onClick: () => { imprimir(reciboFactura(f)); return false; } }]
  });
}

// ---------------- Comprobantes imprimibles ----------------
function lineasRecibo(items) {
  return `<table style="width:100%">${items.map(i => `<tr><td>${i.cantidad} x ${esc(i.producto)}</td>
    <td style="text-align:right">${money(i.subtotal)}</td></tr>`).join('')}</table>`;
}

function reciboFactura(f) {
  return `<div class="recibo">
    <h3>PoliRestaurante</h3><div class="c">Bogotá D.C.</div>
    <div class="c"><strong>${f.tipoFactura === 'ELECTRONICA' ? 'FACTURA ELECTRÓNICA DE VENTA' : 'FACTURA DE VENTA'}</strong></div>
    <div class="c">No. ${esc(f.numeroFactura)}</div><hr>
    <div>Fecha: ${fecha(f.fechaFactura || f.fechaPago)}</div>
    <div>${f.mesa ? 'Mesa #' + f.mesa : 'Para llevar'} · Pedido #${f.pedidoId}</div>
    <div>Cajero: ${esc(f.cajero || '-')}</div><hr>
    ${lineasRecibo(f.items)}<hr>
    <div class="tot"><span>Subtotal</span><span>${money(f.subtotal)}</span></div>
    ${f.descuento ? `<div class="tot"><span>Descuento ${esc(f.promocion || '')}</span><span>-${money(f.descuento)}</span></div>` : ''}
    <div class="tot"><span>${esc(f.impuestoNombre)} incluido</span><span>${money(f.impuesto)}</span></div>
    <div class="tot big"><span>TOTAL</span><span>${money(f.total)}</span></div><hr>
    <div>Medio de pago: ${esc(MEDIOS[f.medioPago] || f.medioPago)} ${f.referencia ? '(' + esc(f.referencia) + ')' : ''}</div>
    ${f.montoRecibido != null ? `<div>Recibido: ${money(f.montoRecibido)} · Cambio: ${money(f.cambio)}</div>` : ''}
    ${f.codigoFacturaElectronica ? `<hr><div class="small">CUFE: ${esc(f.codigoFacturaElectronica)}</div>` : ''}
    <hr><div class="c">¡Gracias por su visita!</div></div>`;
}

function reciboPrecuenta(c) {
  return `<div class="recibo"><h3>PoliRestaurante</h3>
    <div class="c"><strong>PRE-CUENTA</strong> (productos entregados)</div><hr>
    <div>${esc(mesaTxt(c))} · Pedido #${c.pedidoId}</div><div>${fecha(new Date().toISOString())}</div><hr>
    ${c.items.length ? lineasRecibo(c.items) : '<div class="c">Sin productos entregados</div>'}<hr>
    <div class="tot"><span>Subtotal</span><span>${money(c.subtotal)}</span></div>
    ${c.descuento ? `<div class="tot"><span>Descuento</span><span>-${money(c.descuento)}</span></div>` : ''}
    <div class="tot big"><span>TOTAL</span><span>${money(c.total)}</span></div><hr>
    <div class="c">Documento no válido como factura</div></div>`;
}

// =====================================================================
// HISTORIAL DE FACTURAS Y REIMPRESION (HU-CAJ-002, RF18, RF21)
// =====================================================================
async function renderFacturas(panel) {
  const hasta = new Date();
  const desde = new Date(hasta.getTime() - 30 * 86400000);
  panel.innerHTML = `<div class="section-head"><h2>Historial de facturas</h2></div>
    <div class="card"><div class="inline-form">
      <label>Desde<input type="datetime-local" name="desde" value="${inputFecha(desde)}"></label>
      <label>Hasta<input type="datetime-local" name="hasta" value="${inputFecha(new Date(hasta.getTime() + 60000))}"></label>
      <label>Medio de pago<select name="medioPago"><option value="">Todos</option>
        ${Object.entries(MEDIOS).map(([k, v]) => `<option value="${k}">${v}</option>`).join('')}</select></label>
      <button class="btn btn-primary" data-buscar>Consultar</button></div></div>
    <div data-res style="margin-top:1rem"></div>`;
  const res = panel.querySelector('[data-res]');
  const buscar = async () => {
    try {
      const d = leerForm(panel.querySelector('.inline-form'));
      const lista = await get('/cajero/facturas' + qs({ desde: isoLocal(d.desde), hasta: isoLocal(d.hasta), medioPago: d.medioPago }));
      const total = lista.reduce((a, f) => a + f.total, 0);
      res.innerHTML = lista.length ? `<div class="card">
        <div class="card-title"><span class="muted">${lista.length} factura(s)</span><strong>${money(total)}</strong></div>
        <div class="table-wrap"><table>
        <thead><tr><th>Número</th><th>Fecha</th><th>Cuenta</th><th>Cajero</th><th>Medio de pago</th><th>Tipo</th><th class="num">Total</th><th>Estado</th><th></th></tr></thead>
        <tbody>${lista.map(f => `<tr><td class="mono">${esc(f.numeroFactura || '—')}</td><td>${fecha(f.fechaPago)}</td>
          <td>${f.mesa ? 'Mesa #' + f.mesa : 'Llevar'} · #${f.pedidoId}</td><td>${esc(f.cajero || '-')}</td>
          <td>${badge(MEDIOS[f.medioPago] || f.medioPago, 'blue')}</td><td>${esc(f.tipoFactura === 'ELECTRONICA' ? 'Electrónica' : f.tipoFactura === 'FISICA' ? 'Física' : '-')}</td>
          <td class="num">${money(f.total)}</td><td>${estado(f.estadoFactura)}</td>
          <td class="right">${f.estadoFactura === 'SIN_EMITIR'
            ? `<button class="btn btn-sm btn-primary" data-emitir="${f.pagoId}">Emitir</button>`
            : `<button class="btn btn-sm" data-reimprimir="${f.pagoId}">Reimprimir</button>`}
            ${f.estadoFactura === 'PENDIENTE_FE' ? `<button class="btn btn-sm" data-reintentar="${f.pagoId}">Reenviar FE</button>` : ''}</td></tr>`).join('')}
        </tbody></table></div></div>` : vacio('No hay facturas en el periodo');
      res.querySelectorAll('[data-reimprimir]').forEach(b => b.addEventListener('click', async () => {
        try { mostrarFactura(await get('/cajero/facturas/' + b.dataset.reimprimir)); } catch (e) { errorToast(e); }
      }));
      res.querySelectorAll('[data-reintentar]').forEach(b => b.addEventListener('click', async () => {
        try { mostrarFactura(await post(`/cajero/facturas/${b.dataset.reintentar}/reintentar-electronica`)); buscar(); } catch (e) { errorToast(e); }
      }));
      res.querySelectorAll('[data-emitir]').forEach(b => b.addEventListener('click', () => {
        const fila = lista.find(x => x.pagoId == b.dataset.emitir);
        const ctx = modal({ titulo: 'Emitir factura', contenido: '' });
        mostrarAprobado(ctx, { pagoId: fila.pagoId, total: fila.total, medioPago: fila.medioPago }, buscar);
      }));
    } catch (e) { res.innerHTML = `<div class="alert alert-error">${esc(e.message)}</div>`; }
  };
  panel.querySelector('[data-buscar]').addEventListener('click', buscar);
  buscar();
}
