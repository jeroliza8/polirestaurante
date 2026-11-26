// Modulo Administrador: menu y categorias, mesas, usuarios y roles, reportes,
// inventario y proveedores (HU-ADM-01, HU-ADM-02, CU-ADM-01 a CU-ADM-10, RF32-RF36).
import { get, post, put, patch, del, descargar, qs } from '../api.js';
import {
  esc, money, num, fecha, inputFecha, isoLocal, badge, estado, toast, errorToast, modal, confirmar,
  leerForm, opciones, vacio, cargando, subtabs
} from '../ui.js';

export const tabs = [
  { id: 'menu', label: 'Menú', render: renderMenu },
  { id: 'mesas', label: 'Mesas', render: renderMesas },
  { id: 'usuarios', label: 'Usuarios y roles', render: renderUsuarios },
  { id: 'reportes', label: 'Reportes', render: renderReportes },
  { id: 'inventario', label: 'Inventario', render: renderInventario },
  { id: 'proveedores', label: 'Proveedores', render: renderProveedores }
];

// =====================================================================
// MENU: productos (HU-ADM-01, CU-ADM-04) y categorias (CU-ADM-05)
// =====================================================================
async function renderMenu(panel) {
  cargando(panel);
  try {
    const [productos, categorias] = await Promise.all([get('/admin/productos'), get('/admin/categorias')]);
    panel.innerHTML = `
      <div class="section-head"><h2>Gestión de Menú</h2>
        <div class="row"><button class="btn" data-nueva-cat>+ Categoría</button>
        <button class="btn btn-primary" data-nuevo>+ Crear producto</button></div></div>
      <div class="split">
        <div class="card"><div class="card-title"><h3>Categorías</h3></div>
          ${categorias.length ? `<table><tbody>${categorias.map(c => `
            <tr><td>${esc(c.nombre)}<div class="muted small">${c.productos} producto(s)</div></td>
              <td class="right"><button class="btn btn-sm" data-edit-cat="${c.id}">Editar</button>
              <button class="btn btn-sm btn-danger" data-del-cat="${c.id}">Eliminar</button></td></tr>`).join('')}
          </tbody></table>` : vacio('Sin categorías')}
        </div>
        <div class="card"><div class="card-title"><h3>Productos del menú</h3>
          <input data-filtro placeholder="Buscar producto…" style="max-width:240px"></div>
          <div class="table-wrap"><table>
            <thead><tr><th>Producto</th><th>Categoría</th><th class="num">Precio</th><th class="num">Cantidad</th>
              <th>Disponibilidad</th><th></th></tr></thead>
            <tbody data-filas>${productos.map(p => `
              <tr data-nombre="${esc(p.nombre.toLowerCase())}">
                <td><strong>${esc(p.nombre)}</strong><div class="muted small">${esc(p.descripcion)}</div></td>
                <td>${esc(p.categoria)}</td><td class="num">${money(p.precio)}</td>
                <td class="num">${num(p.cantidadDisponible)}</td>
                <td>${p.disponible ? estado(p.estadoStock, p.estadoStock === 'OK' ? 'Disponible' : p.estadoStock) : badge('No disponible', 'red')}</td>
                <td class="right"><button class="btn btn-sm" data-edit="${p.id}">Editar</button>
                  <button class="btn btn-sm btn-danger" data-del="${p.id}">Eliminar</button></td></tr>`).join('')}
            </tbody></table></div>
          ${productos.length ? '' : vacio('Aún no hay productos en el menú')}
        </div>
      </div>`;
    const recargar = () => renderMenu(panel);
    panel.querySelector('[data-filtro]').addEventListener('input', e => {
      const t = e.target.value.toLowerCase();
      panel.querySelectorAll('[data-filas] tr').forEach(tr => { tr.hidden = !tr.dataset.nombre.includes(t); });
    });
    panel.querySelector('[data-nuevo]').addEventListener('click', () => formProducto(null, categorias, recargar));
    panel.querySelector('[data-nueva-cat]').addEventListener('click', () => formCategoria(null, recargar));
    panel.querySelectorAll('[data-edit]').forEach(b => b.addEventListener('click', () =>
      formProducto(productos.find(p => p.id == b.dataset.edit), categorias, recargar)));
    panel.querySelectorAll('[data-del]').forEach(b => b.addEventListener('click', async () => {
      const p = productos.find(x => x.id == b.dataset.del);
      if (!await confirmar('Eliminar producto', `¿Eliminar "${p.nombre}" del menú? Los pedidos anteriores conservan su registro.`, 'Eliminar', 'btn-danger')) return;
      try { await del('/admin/productos/' + p.id); toast('Producto eliminado del menú'); recargar(); } catch (e) { errorToast(e); }
    }));
    panel.querySelectorAll('[data-edit-cat]').forEach(b => b.addEventListener('click', () =>
      formCategoria(categorias.find(c => c.id == b.dataset.editCat), recargar)));
    panel.querySelectorAll('[data-del-cat]').forEach(b => b.addEventListener('click', async () => {
      const c = categorias.find(x => x.id == b.dataset.delCat);
      if (!await confirmar('Eliminar categoría', `¿Eliminar la categoría "${c.nombre}"?`, 'Eliminar', 'btn-danger')) return;
      try { await del('/admin/categorias/' + c.id); toast('Categoría eliminada'); recargar(); } catch (e) { errorToast(e); }
    }));
  } catch (e) { panel.innerHTML = `<div class="alert alert-error">${esc(e.message)}</div>`; }
}

function formProducto(p, categorias, alGuardar) {
  modal({
    titulo: p ? 'Editar producto' : 'Crear producto',
    contenido: `<div class="form-grid">
      <label class="full">Nombre *<input name="nombre" required maxlength="100" value="${esc(p?.nombre)}"></label>
      <label class="full">Descripción<input name="descripcion" maxlength="100" value="${esc(p?.descripcion)}"></label>
      <label>Precio (COP) *<input name="precio" type="number" min="1" step="50" required value="${p?.precio ?? ''}"></label>
      <label>Categoría *<select name="categoriaId" required>${opciones(categorias, 'id', 'nombre', p?.categoriaId, 'Seleccione…')}</select></label>
      <label>Cantidad disponible *<input name="cantidadDisponible" type="number" min="0" step="1" required value="${p?.cantidadDisponible ?? 0}"></label>
      <label>Stock mínimo<input name="stockMinimo" type="number" min="0" step="1" value="${p?.stockMinimo ?? 5}"></label>
      <label class="full row"><input name="disponible" type="checkbox" ${!p || p.disponible ? 'checked' : ''}> Disponible para la toma de pedidos</label>
    </div>`,
    acciones: [
      { texto: 'Cancelar' },
      { texto: 'Guardar', clase: 'btn-primary', onClick: async ({ body }) => {
        const d = leerForm(body);
        if (p) await put('/admin/productos/' + p.id, d); else await post('/admin/productos', d);
        toast(p ? 'Cambios guardados' : 'Producto creado y agregado al menú');
        alGuardar();
      } }
    ]
  });
}

function formCategoria(c, alGuardar) {
  modal({
    titulo: c ? 'Editar categoría' : 'Crear categoría',
    contenido: `<label>Nombre *<input name="nombre" required maxlength="100" value="${esc(c?.nombre)}"></label>`,
    acciones: [
      { texto: 'Cancelar' },
      { texto: 'Guardar', clase: 'btn-primary', onClick: async ({ body }) => {
        const d = leerForm(body);
        if (c) await put('/admin/categorias/' + c.id, d); else await post('/admin/categorias', d);
        toast('Categoría guardada');
        alGuardar();
      } }
    ]
  });
}

// =====================================================================
// MESAS (CU-ADM-02)
// =====================================================================
async function renderMesas(panel) {
  cargando(panel);
  try {
    const mesas = await get('/admin/mesas');
    const libres = mesas.filter(m => m.estado === 'LIBRE').length;
    panel.innerHTML = `
      <div class="section-head"><h2>Gestión de Mesas</h2><button class="btn btn-primary" data-nueva>+ Crear mesa</button></div>
      <div class="grid grid-3" style="margin-bottom:1rem">
        <div class="kpi"><div class="label">Mesas</div><div class="value">${mesas.length}</div></div>
        <div class="kpi"><div class="label">Libres</div><div class="value">${libres}</div></div>
        <div class="kpi"><div class="label">Ocupadas</div><div class="value">${mesas.length - libres}</div></div>
      </div>
      ${mesas.length ? `<div class="tiles">${mesas.map(m => `
        <div class="tile">
          <div class="row" style="justify-content:space-between"><span class="big">#${m.numero}</span>${estado(m.estado)}</div>
          <span class="muted small">${m.capacidad} persona(s)</span>
          ${m.pedidoActivo ? badge('Pedido en curso', 'amber') : '<span class="small muted">Sin pedidos activos</span>'}
          <div class="row"><button class="btn btn-sm" data-edit="${m.id}">Editar</button>
            <button class="btn btn-sm btn-danger" data-del="${m.id}">Eliminar</button></div>
        </div>`).join('')}</div>` : vacio('No hay mesas registradas')}`;
    const recargar = () => renderMesas(panel);
    panel.querySelector('[data-nueva]').addEventListener('click', () => formMesa(null, recargar));
    panel.querySelectorAll('[data-edit]').forEach(b => b.addEventListener('click', () =>
      formMesa(mesas.find(m => m.id == b.dataset.edit), recargar)));
    panel.querySelectorAll('[data-del]').forEach(b => b.addEventListener('click', async () => {
      const m = mesas.find(x => x.id == b.dataset.del);
      if (!await confirmar('Eliminar mesa', `¿Eliminar la mesa #${m.numero}? Su número no se volverá a asignar.`, 'Eliminar', 'btn-danger')) return;
      try { await del('/admin/mesas/' + m.id); toast('Mesa eliminada'); recargar(); } catch (e) { errorToast(e); }
    }));
  } catch (e) { panel.innerHTML = `<div class="alert alert-error">${esc(e.message)}</div>`; }
}

function formMesa(m, alGuardar) {
  modal({
    titulo: m ? `Editar mesa #${m.numero}` : 'Crear nueva mesa',
    contenido: `<div class="form-grid">
      <label>Número de mesa *<input name="numero" type="number" min="1" required value="${m?.numero ?? ''}"></label>
      <label>Capacidad (personas) *<input name="capacidad" type="number" min="1" max="30" required value="${m?.capacidad ?? 4}"></label>
    </div>`,
    acciones: [
      { texto: 'Cancelar' },
      { texto: m ? 'Guardar' : 'Crear mesa', clase: 'btn-primary', onClick: async ({ body }) => {
        const d = leerForm(body);
        try {
          if (m) await put('/admin/mesas/' + m.id, d); else await post('/admin/mesas', d);
        } catch (e) {
          // CU-ADM-02 flujo alternativo: numero repetido -> se sugiere el siguiente disponible
          if (e.status === 409 && e.data?.sugerencia) {
            const ok = await confirmar('Número en uso', `${e.message}. ¿Desea usar el número ${e.data.sugerencia}?`, 'Usar ' + e.data.sugerencia);
            if (!ok) return false;
            d.numero = e.data.sugerencia;
            if (m) await put('/admin/mesas/' + m.id, d); else await post('/admin/mesas', d);
          } else throw e;
        }
        toast(m ? 'Mesa actualizada' : `Mesa #${d.numero} creada (estado Libre)`);
        alGuardar();
      } }
    ]
  });
}

// =====================================================================
// USUARIOS Y ROLES (HU-ADM-02, CU-ADM-03)
// =====================================================================
const ROLES = ['ADMINISTRADOR', 'CAJERO', 'MESERO', 'COCINERO', 'REPARTIDOR'];

async function renderUsuarios(panel) {
  cargando(panel);
  try {
    const [usuarios, roles] = await Promise.all([get('/admin/usuarios'), get('/admin/roles')]);
    const activos = usuarios.filter(u => u.estado === 'activo').length;
    panel.innerHTML = `
      <div class="section-head"><h2>Gestión de Usuarios</h2><button class="btn btn-primary" data-nuevo>+ Crear usuario</button></div>
      <div class="grid grid-3" style="margin-bottom:1rem">
        <div class="kpi"><div class="label">Total</div><div class="value">${usuarios.length}</div></div>
        <div class="kpi"><div class="label">Activos</div><div class="value">${activos}</div></div>
        <div class="kpi"><div class="label">Inactivos</div><div class="value">${usuarios.length - activos}</div></div>
      </div>
      <div class="split">
        <div class="card"><h3>Roles y permisos</h3>${roles.map(r => `
          <div style="margin-bottom:.7rem">${badge(r.rol, 'blue')} <span class="muted small">${esc(r.descripcion)}</span>
          <div class="small">${r.permisos.map(esc).join(' · ')}</div></div>`).join('')}</div>
        <div class="card">
          <div class="row" style="margin-bottom:.6rem"><input data-buscar placeholder="Buscar por nombre…" style="max-width:240px">
            <select data-rol style="max-width:200px"><option value="">Todos los roles</option>${[...ROLES, 'PROVEEDOR'].map(r => `<option>${r}</option>`).join('')}</select></div>
          <div class="table-wrap"><table><thead><tr><th>Usuario</th><th>Rol</th><th>Detalle</th><th>Estado</th><th></th></tr></thead>
          <tbody>${usuarios.map(u => `
            <tr data-nombre="${esc(u.nombre.toLowerCase())}" data-r="${esc(u.rol || '')}">
              <td><strong>${esc(u.nombre)}</strong></td><td>${u.rol ? badge(u.rol, 'blue') : badge('Sin rol')}</td>
              <td class="muted">${esc(u.detalle || '')}</td><td>${estado(u.estado)}</td>
              <td class="right">
                ${u.rol !== 'PROVEEDOR' ? `<button class="btn btn-sm" data-rolbtn="${u.id}">Rol</button>` : ''}
                <button class="btn btn-sm" data-pass="${u.id}">Contraseña</button>
                ${u.estado === 'activo'
                  ? `<button class="btn btn-sm btn-danger" data-estado="${u.id}" data-activo="false">Desactivar</button>`
                  : `<button class="btn btn-sm btn-success" data-estado="${u.id}" data-activo="true">Reactivar</button>`}
              </td></tr>`).join('')}</tbody></table></div>
        </div>
      </div>`;
    const recargar = () => renderUsuarios(panel);
    const filtrar = () => {
      const t = panel.querySelector('[data-buscar]').value.toLowerCase();
      const r = panel.querySelector('[data-rol]').value;
      panel.querySelectorAll('tbody tr[data-nombre]').forEach(tr => {
        tr.hidden = !tr.dataset.nombre.includes(t) || (r && tr.dataset.r !== r);
      });
    };
    panel.querySelector('[data-buscar]').addEventListener('input', filtrar);
    panel.querySelector('[data-rol]').addEventListener('change', filtrar);
    panel.querySelector('[data-nuevo]').addEventListener('click', () => formUsuario(recargar));
    panel.querySelectorAll('[data-rolbtn]').forEach(b => b.addEventListener('click', () =>
      formRol(usuarios.find(u => u.id == b.dataset.rolbtn), recargar)));
    panel.querySelectorAll('[data-pass]').forEach(b => b.addEventListener('click', () =>
      formPassword(usuarios.find(u => u.id == b.dataset.pass))));
    panel.querySelectorAll('[data-estado]').forEach(b => b.addEventListener('click', async () => {
      const u = usuarios.find(x => x.id == b.dataset.estado);
      const activo = b.dataset.activo === 'true';
      const ok = await confirmar(activo ? 'Reactivar usuario' : 'Desactivar usuario',
        activo ? `${u.nombre} podrá volver a iniciar sesión.` : `${u.nombre} no podrá iniciar sesión mientras esté desactivado.`,
        activo ? 'Reactivar' : 'Desactivar', activo ? 'btn-success' : 'btn-danger');
      if (!ok) return;
      try { await patch(`/admin/usuarios/${u.id}/estado`, { activo }); toast(activo ? 'Usuario reactivado' : 'Usuario desactivado'); recargar(); }
      catch (e) { errorToast(e); }
    }));
  } catch (e) { panel.innerHTML = `<div class="alert alert-error">${esc(e.message)}</div>`; }
}

const camposRol = () => `
  <label data-campo="MESERO">Zona asignada<input name="zonaAsignada" maxlength="45"></label>
  <label data-campo="COCINERO">Estación<input name="estacion" maxlength="45"></label>
  <label data-campo="REPARTIDOR">Placa del vehículo<input name="placaVehiculo" maxlength="45"></label>`;

function mostrarCamposRol(body) {
  const sel = body.querySelector('[name=rol]');
  const act = () => body.querySelectorAll('[data-campo]').forEach(l => { l.hidden = l.dataset.campo !== sel.value; });
  sel.addEventListener('change', act);
  act();
}

function formUsuario(alGuardar) {
  modal({
    titulo: 'Crear usuario',
    contenido: `<div class="form-grid">
      <label>Usuario *<input name="nombre" required pattern="[A-Za-z0-9._\\-]{3,50}" title="3 a 50 caracteres: letras, números, punto o guion"></label>
      <label>Contraseña *<input name="password" type="password" required minlength="6" maxlength="72"></label>
      <label>Rol *<select name="rol" required>${ROLES.map(r => `<option>${r}</option>`).join('')}</select></label>
      ${camposRol()}</div>
      <p class="muted small">Los proveedores se registran desde la pestaña Proveedores.</p>`,
    alAbrir: ({ body }) => mostrarCamposRol(body),
    acciones: [
      { texto: 'Cancelar' },
      { texto: 'Crear', clase: 'btn-primary', onClick: async ({ body }) => {
        await post('/admin/usuarios', leerForm(body));
        toast('Usuario creado');
        alGuardar();
      } }
    ]
  });
}

function formRol(u, alGuardar) {
  modal({
    titulo: `Asignar rol a ${u.nombre}`,
    contenido: `<div class="form-grid">
      <label class="full">Rol *<select name="rol" required>${ROLES.map(r => `<option ${r === u.rol ? 'selected' : ''}>${r}</option>`).join('')}</select></label>
      ${camposRol()}</div>`,
    alAbrir: ({ body }) => mostrarCamposRol(body),
    acciones: [
      { texto: 'Cancelar' },
      { texto: 'Asignar', clase: 'btn-primary', onClick: async ({ body }) => {
        await put(`/admin/usuarios/${u.id}/rol`, leerForm(body));
        toast('Rol actualizado: el usuario solo verá los módulos de su nuevo rol');
        alGuardar();
      } }
    ]
  });
}

function formPassword(u) {
  modal({
    titulo: `Restablecer contraseña de ${u.nombre}`,
    contenido: `<label>Nueva contraseña *<input name="password" type="password" required minlength="6" maxlength="72"></label>`,
    acciones: [
      { texto: 'Cancelar' },
      { texto: 'Guardar', clase: 'btn-primary', onClick: async ({ body }) => {
        await put(`/admin/usuarios/${u.id}/password`, leerForm(body));
        toast('Contraseña actualizada');
      } }
    ]
  });
}

// =====================================================================
// REPORTES (CU-ADM-01 ventas, CU-ADM-06 tiempos, CU-ADM-08 agotados)
// =====================================================================
function renderReportes(panel) {
  panel.innerHTML = '<div class="section-head"><h2>Reportes</h2></div><div data-r></div>';
  subtabs(panel.querySelector('[data-r]'), [
    { id: 'ventas', label: 'Ventas por periodo', render: reporteVentas },
    { id: 'tiempos', label: 'Tiempos de preparación', render: reporteTiempos },
    { id: 'agotados', label: 'Agotados (diario)', render: reporteAgotados }
  ]);
}

function rangoPorDefecto(dias) {
  const hasta = new Date();
  const desde = new Date(hasta.getTime() - dias * 86400000);
  desde.setHours(0, 0, 0, 0);
  return { desde: inputFecha(desde), hasta: inputFecha(hasta) };
}

async function reporteVentas(cont) {
  const r0 = rangoPorDefecto(30);
  let categorias = [];
  try { categorias = await get('/admin/categorias'); } catch { /* sin filtro */ }
  cont.innerHTML = `
    <div class="card"><div class="inline-form">
      <label>Fecha inicial<input type="datetime-local" name="desde" required value="${r0.desde}"></label>
      <label>Fecha final<input type="datetime-local" name="hasta" required value="${r0.hasta}"></label>
      <label>Categoría<select name="categoriaId">${opciones(categorias, 'id', 'nombre', '', 'Todas')}</select></label>
      <button class="btn btn-primary" data-generar>Generar</button>
      <button class="btn" data-pdf disabled>Exportar PDF</button>
      <button class="btn" data-excel disabled>Exportar Excel</button>
    </div></div><div data-res style="margin-top:1rem"></div>`;
  const res = cont.querySelector('[data-res]');
  const params = () => {
    const d = leerForm(cont.querySelector('.inline-form'));
    return { desde: isoLocal(d.desde), hasta: isoLocal(d.hasta), categoriaId: d.categoriaId };
  };
  const exportar = async formato => {
    try {
      const p = params();
      await descargar('/admin/reportes/ventas/exportar' + qs({ ...p, formato }),
        `reporte-ventas.${formato === 'excel' ? 'xlsx' : 'pdf'}`);
    } catch (e) { errorToast(e); }
  };
  cont.querySelector('[data-pdf]').addEventListener('click', () => exportar('pdf'));
  cont.querySelector('[data-excel]').addEventListener('click', () => exportar('excel'));
  cont.querySelector('[data-generar]').addEventListener('click', async () => {
    const habilitar = v => cont.querySelectorAll('[data-pdf],[data-excel]').forEach(b => { b.disabled = !v; });
    try {
      const r = await get('/admin/reportes/ventas' + qs(params()));
      habilitar(true);
      const maxDia = Math.max(...r.ventasPorDia.map(d => d.monto), 1);
      res.innerHTML = `
        <div class="grid grid-4">
          <div class="kpi"><div class="label">Monto total</div><div class="value">${money(r.montoTotal)}</div>
            <div class="hint">${r.categoria ? 'Categoría: ' + esc(r.categoria) : 'Todas las categorías'}</div></div>
          <div class="kpi"><div class="label">Pedidos</div><div class="value">${r.numeroPedidos}</div></div>
          <div class="kpi"><div class="label">Ticket promedio</div><div class="value">${money(r.ticketPromedio)}</div></div>
          <div class="kpi"><div class="label">Más vendido</div><div class="value" style="font-size:1.05rem">${esc(r.productoMasVendido)}</div>
            <div class="hint">${r.unidadesProductoMasVendido} unidades</div></div>
        </div>
        <div class="grid grid-2" style="margin-top:1rem">
          <div class="card"><h3>Ventas por día</h3><div class="bars">${r.ventasPorDia.map(d => `
            <div class="bar-row"><span>${esc(d.fecha)}</span><div class="bar-track"><div class="bar-fill" style="width:${(d.monto / maxDia * 100).toFixed(1)}%"></div></div>
            <span class="num">${money(d.monto)}</span></div>`).join('')}</div></div>
          <div class="card"><h3>Productos más vendidos</h3><table><thead><tr><th>Producto</th><th class="num">Und</th><th class="num">Monto</th></tr></thead>
            <tbody>${r.topProductos.map(i => `<tr><td>${esc(i.producto)}</td><td class="num">${i.cantidad}</td><td class="num">${money(i.monto)}</td></tr>`).join('')}</tbody></table></div>
          <div class="card"><h3>Por categoría</h3><table><tbody>${r.ventasPorCategoria.map(s => `
            <tr><td>${esc(s.etiqueta)}</td><td class="num">${s.cantidad} und</td><td class="num">${money(s.valor)}</td></tr>`).join('')}</tbody></table></div>
          <div class="card"><h3>Por medio de pago</h3><table><tbody>${r.ventasPorMedioPago.map(s => `
            <tr><td>${esc(s.etiqueta)}</td><td class="num">${s.cantidad} pago(s)</td><td class="num">${money(s.valor)}</td></tr>`).join('')}</tbody></table>
            ${r.descuentos ? `<p class="muted small">Descuentos otorgados: ${money(r.descuentos)}</p>` : ''}</div>
        </div>`;
    } catch (e) {
      habilitar(false);
      res.innerHTML = `<div class="alert ${e.status === 404 ? 'alert-info' : 'alert-error'}">${esc(e.message)}</div>`;
    }
  });
}

async function reporteTiempos(cont) {
  const r0 = rangoPorDefecto(30);
  cont.innerHTML = `
    <div class="card"><div class="inline-form">
      <label>Fecha inicial<input type="datetime-local" name="desde" required value="${r0.desde}"></label>
      <label>Fecha final<input type="datetime-local" name="hasta" required value="${r0.hasta}"></label>
      <button class="btn btn-primary" data-generar>Generar reporte de tiempos</button>
    </div></div><div data-res style="margin-top:1rem"></div>`;
  const res = cont.querySelector('[data-res]');
  const min = v => (v === null || v === undefined ? '-' : num(v) + ' min');
  cont.querySelector('[data-generar]').addEventListener('click', async () => {
    try {
      const d = leerForm(cont.querySelector('.inline-form'));
      const r = await get('/admin/reportes/tiempos' + qs({ desde: isoLocal(d.desde), hasta: isoLocal(d.hasta) }));
      res.innerHTML = `
        <div class="grid grid-4">
          <div class="kpi"><div class="label">Espera (pendiente → preparación)</div><div class="value">${min(r.promedioEspera)}</div></div>
          <div class="kpi"><div class="label">Preparación (→ listo)</div><div class="value">${min(r.promedioPreparacion)}</div></div>
          <div class="kpi"><div class="label">Entrega (listo → entregado)</div><div class="value">${min(r.promedioEntrega)}</div></div>
          <div class="kpi"><div class="label">Total promedio</div><div class="value">${min(r.promedioTotal)}</div>
            <div class="hint">${r.pedidosAnalizados} pedido(s)</div></div>
        </div>
        <div class="card" style="margin-top:1rem"><div class="table-wrap"><table>
          <thead><tr><th>Pedido</th><th>Mesa</th><th>Creado</th><th>Estado</th><th class="num">Espera</th><th class="num">Preparación</th><th class="num">Entrega</th><th class="num">Total</th></tr></thead>
          <tbody>${r.pedidos.map(p => `<tr><td>#${p.pedidoId}</td><td>${p.mesa ?? 'Llevar'}</td><td>${fecha(p.fechaCreacion)}</td><td>${estado(p.estado)}</td>
            <td class="num">${min(p.minutosEspera)}</td><td class="num">${min(p.minutosPreparacion)}</td><td class="num">${min(p.minutosEntrega)}</td><td class="num">${min(p.minutosTotal)}</td></tr>`).join('')}</tbody>
        </table></div></div>`;
    } catch (e) {
      res.innerHTML = `<div class="alert ${e.status === 404 ? 'alert-info' : 'alert-error'}">${esc(e.message)}</div>`;
    }
  });
}

async function reporteAgotados(cont) {
  cargando(cont);
  try {
    const r = await get('/admin/reportes/agotados');
    cont.innerHTML = `
      <div class="section-head"><h3>Reporte del ${fecha(r.fecha, false)}</h3>
        <span>${r.totalAlertas ? badge(r.totalAlertas + ' alerta(s)', 'amber') : badge('Sin alertas', 'green')}</span></div>
      <div class="grid grid-2">
        <div class="card"><h3>Insumos</h3>${r.insumos.length ? `<table><thead><tr><th>Insumo</th><th class="num">Disponible</th><th class="num">Mínimo</th><th>Estado</th></tr></thead>
          <tbody>${r.insumos.map(i => `<tr><td>${esc(i.nombre)}</td><td class="num">${num(i.cantidadDisponible)} ${esc(i.unidadMedida)}</td>
          <td class="num">${num(i.stockMinimo)}</td><td>${estado(i.estado)}</td></tr>`).join('')}</tbody></table>` : vacio('Todos los insumos están por encima del mínimo')}</div>
        <div class="card"><h3>Productos del menú</h3>${r.productos.length ? `<table><thead><tr><th>Producto</th><th class="num">Disponible</th><th>Estado</th></tr></thead>
          <tbody>${r.productos.map(p => `<tr><td>${esc(p.nombre)}</td><td class="num">${num(p.cantidadDisponible)}</td>
          <td>${p.disponible ? estado(p.estadoStock) : badge('No disponible', 'red')}</td></tr>`).join('')}</tbody></table>` : vacio('Sin productos agotados')}</div>
      </div>`;
  } catch (e) { cont.innerHTML = `<div class="alert alert-error">${esc(e.message)}</div>`; }
}

// =====================================================================
// INVENTARIO (CU-ADM-07) e INGRESO DE MERCANCIA (CU-ADM-10)
// =====================================================================
async function renderInventario(panel) {
  cargando(panel);
  try {
    const [inv, proveedores] = await Promise.all([get('/admin/inventario'), get('/admin/proveedores')]);
    panel.innerHTML = `
      <div class="section-head"><h2>Inventario</h2><button class="btn btn-primary" data-nuevo>+ Nuevo insumo</button></div>
      <div class="card"><h3>Insumos</h3><div class="table-wrap"><table>
        <thead><tr><th>Insumo</th><th class="num">Disponible</th><th class="num">Mínimo</th><th>Estado</th><th>Proveedores</th><th></th></tr></thead>
        <tbody>${inv.insumos.map(i => `<tr><td><strong>${esc(i.nombre)}</strong></td>
          <td class="num">${num(i.cantidadDisponible)} ${esc(i.unidadMedida)}</td><td class="num">${num(i.stockMinimo)}</td>
          <td>${estado(i.estado)}</td><td class="small">${i.proveedores.map(esc).join(', ') || '<span class="muted">-</span>'}</td>
          <td class="right"><button class="btn btn-sm btn-success" data-ingreso="${i.id}">Registrar ingreso</button>
            <button class="btn btn-sm" data-edit="${i.id}">Editar</button></td></tr>`).join('')}</tbody></table></div>
        ${inv.insumos.length ? '' : vacio('No hay insumos registrados')}</div>
      <div class="card" style="margin-top:1rem"><h3>Disponibilidad de productos del menú</h3><div class="table-wrap"><table>
        <thead><tr><th>Producto</th><th>Categoría</th><th class="num">Cantidad</th><th>Estado</th></tr></thead>
        <tbody>${inv.productos.map(p => `<tr><td>${esc(p.nombre)}</td><td>${esc(p.categoria)}</td><td class="num">${num(p.cantidadDisponible)}</td>
          <td>${p.disponible ? estado(p.estadoStock) : badge('No disponible', 'red')}</td></tr>`).join('')}</tbody></table></div></div>`;
    const recargar = () => renderInventario(panel);
    panel.querySelector('[data-nuevo]').addEventListener('click', () => formInsumo(null, recargar));
    panel.querySelectorAll('[data-edit]').forEach(b => b.addEventListener('click', () =>
      formInsumo(inv.insumos.find(i => i.id == b.dataset.edit), recargar)));
    panel.querySelectorAll('[data-ingreso]').forEach(b => b.addEventListener('click', () =>
      formIngreso(inv.insumos.find(i => i.id == b.dataset.ingreso), proveedores, recargar)));
  } catch (e) { panel.innerHTML = `<div class="alert alert-error">${esc(e.message)}</div>`; }
}

function formInsumo(i, alGuardar) {
  modal({
    titulo: i ? 'Editar insumo' : 'Nuevo insumo',
    contenido: `<div class="form-grid">
      <label class="full">Nombre *<input name="nombre" required maxlength="100" value="${esc(i?.nombre)}"></label>
      <label>Unidad de medida *<input name="unidadMedida" required maxlength="20" list="unidades" value="${esc(i?.unidadMedida ?? 'unidad')}">
        <datalist id="unidades"><option>unidad</option><option>kg</option><option>litro</option><option>paquete</option></datalist></label>
      <label>Cantidad disponible *<input name="cantidadDisponible" type="number" min="0" required value="${i?.cantidadDisponible ?? 0}"></label>
      <label>Stock mínimo *<input name="stockMinimo" type="number" min="0" required value="${i?.stockMinimo ?? 5}"></label>
    </div>`,
    acciones: [
      { texto: 'Cancelar' },
      { texto: 'Guardar', clase: 'btn-primary', onClick: async ({ body }) => {
        const d = leerForm(body);
        if (i) await put('/admin/insumos/' + i.id, d); else await post('/admin/insumos', d);
        toast('Insumo guardado');
        alGuardar();
      } }
    ]
  });
}

function formIngreso(insumo, proveedores, alGuardar) {
  const conInsumo = proveedores.filter(p => p.insumos.some(x => x.insumoId === insumo.id));
  modal({
    titulo: `Registrar ingreso: ${insumo.nombre}`,
    contenido: `<p class="muted">Disponible actual: <strong>${num(insumo.cantidadDisponible)} ${esc(insumo.unidadMedida)}</strong></p>
      <div class="form-grid">
        <label>Cantidad ingresada *<input name="cantidad" type="number" min="1" max="100000" required></label>
        <label>Proveedor (opcional)<select name="proveedorId">${opciones(conInsumo, 'id', 'nombre', '', 'Ingreso manual sin proveedor')}</select></label>
        <label class="full">Precio unitario (opcional, por defecto el pactado)<input name="precioUnitario" type="number" min="1" step="any"></label>
      </div>
      <p class="muted small">Con proveedor, el ingreso queda en su historial de entregas y genera el pago pendiente.</p>`,
    acciones: [
      { texto: 'Cancelar' },
      { texto: 'Registrar ingreso', clase: 'btn-primary', onClick: async ({ body }) => {
        const r = await post(`/admin/insumos/${insumo.id}/ingresos`, leerForm(body));
        toast(`Ingreso registrado. Nuevo disponible: ${num(r.cantidadDisponible)} ${r.unidadMedida}`);
        alGuardar();
      } }
    ]
  });
}

// =====================================================================
// PROVEEDORES (CU-ADM-09, RF32, RF35, RF36)
// =====================================================================
function renderProveedores(panel) {
  panel.innerHTML = '<div class="section-head"><h2>Proveedores</h2></div><div data-p></div>';
  subtabs(panel.querySelector('[data-p]'), [
    { id: 'lista', label: 'Proveedores', render: listaProveedores },
    { id: 'solicitudes', label: 'Solicitudes de insumos', render: solicitudes },
    { id: 'pagos', label: 'Pagos a proveedores', render: pagosProveedores }
  ]);
}

async function listaProveedores(cont) {
  cargando(cont);
  try {
    const [provs, inv] = await Promise.all([get('/admin/proveedores'), get('/admin/inventario')]);
    const insumos = inv.insumos;
    cont.innerHTML = `
      <div class="row-end" style="margin-bottom:1rem"><button class="btn btn-primary" data-nuevo>+ Registrar proveedor</button></div>
      ${provs.length ? `<div class="grid grid-2">${provs.map(p => `
        <div class="card">
          <div class="card-title"><h3>${esc(p.nombre)}</h3>
            ${p.saldoPendiente ? badge('Por pagar ' + money(p.saldoPendiente), 'amber') : badge('Al día', 'green')}</div>
          <p class="muted small">${esc(p.contacto)} · usuario <strong>${esc(p.usuario || '-')}</strong> ${p.usuarioActivo ? '' : badge('inactivo', 'red')}</p>
          <table><thead><tr><th>Insumo que suministra</th><th class="num">Precio</th><th></th><th></th></tr></thead><tbody>
          ${p.insumos.map(i => `<tr><td>${esc(i.insumo)} <span class="muted small">/${esc(i.unidadMedida)}</span></td>
            <td class="num">${money(i.precio)}</td><td>${i.disponible ? '' : badge('No disponible', 'red')}</td>
            <td class="right"><button class="btn btn-sm btn-ghost" data-quitar="${p.id}:${i.idRelacion}" title="Quitar">✕</button></td></tr>`).join('')
            || '<tr><td colspan="4" class="muted">Sin insumos registrados</td></tr>'}
          </tbody></table>
          <div class="row" style="margin-top:.6rem"><button class="btn btn-sm" data-agregar="${p.id}">+ Insumo</button>
            <button class="btn btn-sm" data-edit="${p.id}">Editar datos</button></div>
        </div>`).join('')}</div>` : vacio('No hay proveedores registrados')}`;
    const recargar = () => listaProveedores(cont);
    cont.querySelector('[data-nuevo]').addEventListener('click', () => formProveedor(insumos, recargar));
    cont.querySelectorAll('[data-edit]').forEach(b => b.addEventListener('click', () => {
      const p = provs.find(x => x.id == b.dataset.edit);
      modal({
        titulo: 'Editar proveedor',
        contenido: `<div class="form-grid">
          <label class="full">Nombre *<input name="nombre" required maxlength="100" value="${esc(p.nombre)}"></label>
          <label class="full">Contacto *<input name="contacto" required maxlength="100" value="${esc(p.contacto)}"></label></div>`,
        acciones: [{ texto: 'Cancelar' }, { texto: 'Guardar', clase: 'btn-primary', onClick: async ({ body }) => {
          await put('/admin/proveedores/' + p.id, leerForm(body)); toast('Proveedor actualizado'); recargar();
        } }]
      });
    }));
    cont.querySelectorAll('[data-agregar]').forEach(b => b.addEventListener('click', () => {
      modal({
        titulo: 'Agregar insumo que suministra',
        contenido: `<div class="form-grid">
          <label>Insumo *<select name="insumoId" required>${opciones(insumos, 'id', 'nombre', '', 'Seleccione…')}</select></label>
          <label>Precio pactado *<input name="precio" type="number" min="1" step="any" required></label></div>`,
        acciones: [{ texto: 'Cancelar' }, { texto: 'Guardar', clase: 'btn-primary', onClick: async ({ body }) => {
          await post(`/admin/proveedores/${b.dataset.agregar}/insumos`, leerForm(body)); toast('Insumo vinculado'); recargar();
        } }]
      });
    }));
    cont.querySelectorAll('[data-quitar]').forEach(b => b.addEventListener('click', async () => {
      const [pid, rid] = b.dataset.quitar.split(':');
      if (!await confirmar('Quitar insumo', '¿El proveedor deja de suministrar este insumo?', 'Quitar', 'btn-danger')) return;
      try { await del(`/admin/proveedores/${pid}/insumos/${rid}`); recargar(); } catch (e) { errorToast(e); }
    }));
  } catch (e) { cont.innerHTML = `<div class="alert alert-error">${esc(e.message)}</div>`; }
}

function filaInsumoPrecio(insumos) {
  return `<div class="row" data-fila><select data-insumo style="flex:2">${opciones(insumos, 'id', 'nombre', '', 'Insumo…')}</select>
    <input data-precio type="number" min="1" step="any" placeholder="Precio" style="flex:1">
    <button type="button" class="btn btn-sm btn-ghost" data-x>✕</button></div>`;
}

function formProveedor(insumos, alGuardar) {
  modal({
    titulo: 'Registrar proveedor',
    grande: true,
    contenido: `<div class="form-grid">
      <label>Nombre *<input name="nombre" required maxlength="100"></label>
      <label>Contacto (teléfono / correo) *<input name="contacto" required maxlength="100"></label>
      <label>Usuario de acceso *<input name="usuario" required pattern="[A-Za-z0-9._\\-]{3,50}"></label>
      <label>Contraseña *<input name="password" type="password" required minlength="6" maxlength="72"></label></div>
      <h4>Insumos que suministra</h4><div class="stack" data-filas>${filaInsumoPrecio(insumos)}</div>
      <div><button type="button" class="btn btn-sm" data-mas>+ Otro insumo</button></div>`,
    alAbrir: ({ body }) => {
      const filas = body.querySelector('[data-filas]');
      const enlazar = () => filas.querySelectorAll('[data-x]').forEach(x => { x.onclick = () => x.closest('[data-fila]').remove(); });
      body.querySelector('[data-mas]').addEventListener('click', () => { filas.insertAdjacentHTML('beforeend', filaInsumoPrecio(insumos)); enlazar(); });
      enlazar();
    },
    acciones: [
      { texto: 'Cancelar' },
      { texto: 'Guardar', clase: 'btn-primary', onClick: async ({ body }) => {
        const datos = leerForm(body.querySelector('.form-grid'));
        datos.insumos = [...body.querySelectorAll('[data-fila]')]
          .map(f => ({ insumoId: Number(f.querySelector('[data-insumo]').value), precio: Number(f.querySelector('[data-precio]').value) }))
          .filter(x => x.insumoId);
        if (datos.insumos.some(x => !(x.precio > 0))) throw new Error('Indique el precio de cada insumo');
        await post('/admin/proveedores', datos);
        toast('Proveedor registrado');
        alGuardar();
      } }
    ]
  });
}

async function solicitudes(cont) {
  cargando(cont);
  try {
    const [lista, provs] = await Promise.all([get('/admin/solicitudes'), get('/admin/proveedores')]);
    cont.innerHTML = `
      <div class="row-end" style="margin-bottom:1rem"><button class="btn btn-primary" data-nueva>+ Nueva solicitud</button></div>
      ${lista.length ? `<div class="card"><div class="table-wrap"><table>
        <thead><tr><th>#</th><th>Fecha</th><th>Proveedor</th><th>Insumos</th><th class="num">Total</th><th>Estado</th><th></th></tr></thead>
        <tbody>${lista.map(s => `<tr><td>${s.id}</td><td>${fecha(s.fecha)}</td><td>${esc(s.proveedor)}</td>
          <td class="small">${s.items.map(i => `${esc(i.insumo)} × ${i.cantidad} ${esc(i.unidadMedida)}`).join('<br>')}</td>
          <td class="num">${money(s.total)}</td>
          <td>${estado(s.estado)}${s.fechaRecepcion ? `<div class="muted small">${fecha(s.fechaRecepcion)}</div>` : ''}</td>
          <td class="right">${['PENDIENTE', 'DESPACHADA'].includes(s.estado) ? `
            <button class="btn btn-sm btn-success" data-recibir="${s.id}">Recibir mercancía</button>
            <button class="btn btn-sm btn-danger" data-cancelar="${s.id}">Cancelar</button>` : ''}</td></tr>`).join('')}
        </tbody></table></div></div>` : vacio('No hay solicitudes de insumos')}`;
    const recargar = () => solicitudes(cont);
    cont.querySelector('[data-nueva]').addEventListener('click', () => formSolicitud(provs, recargar));
    cont.querySelectorAll('[data-recibir]').forEach(b => b.addEventListener('click', async () => {
      if (!await confirmar('Recibir mercancía', 'Se sumará al inventario y quedará el pago pendiente al proveedor.', 'Recibir', 'btn-success')) return;
      try { await post(`/admin/solicitudes/${b.dataset.recibir}/recibir`); toast('Mercancía recibida e inventario actualizado'); recargar(); } catch (e) { errorToast(e); }
    }));
    cont.querySelectorAll('[data-cancelar]').forEach(b => b.addEventListener('click', async () => {
      if (!await confirmar('Cancelar solicitud', '¿Cancelar esta solicitud de insumos?', 'Cancelar solicitud', 'btn-danger')) return;
      try { await post(`/admin/solicitudes/${b.dataset.cancelar}/cancelar`); recargar(); } catch (e) { errorToast(e); }
    }));
  } catch (e) { cont.innerHTML = `<div class="alert alert-error">${esc(e.message)}</div>`; }
}

function formSolicitud(provs, alGuardar) {
  modal({
    titulo: 'Nueva solicitud de insumos',
    grande: true,
    contenido: `<label>Proveedor *<select name="proveedorId" required>${opciones(provs, 'id', 'nombre', '', 'Seleccione…')}</select></label>
      <div data-items class="stack"><p class="muted">Seleccione un proveedor para ver los insumos que suministra.</p></div>`,
    alAbrir: ({ body }) => {
      body.querySelector('[name=proveedorId]').addEventListener('change', e => {
        const p = provs.find(x => x.id == e.target.value);
        const disp = p ? p.insumos.filter(i => i.disponible) : [];
        body.querySelector('[data-items]').innerHTML = disp.length ? `<table><thead><tr><th>Insumo</th><th class="num">Precio</th><th style="width:140px">Cantidad</th></tr></thead>
          <tbody>${disp.map(i => `<tr><td>${esc(i.insumo)} <span class="muted small">/${esc(i.unidadMedida)}</span></td><td class="num">${money(i.precio)}</td>
          <td><input type="number" min="0" step="1" value="0" data-cant="${i.insumoId}"></td></tr>`).join('')}</tbody></table>`
          : '<p class="muted">Este proveedor no tiene insumos disponibles.</p>';
      });
    },
    acciones: [
      { texto: 'Cancelar' },
      { texto: 'Enviar solicitud', clase: 'btn-primary', onClick: async ({ body }) => {
        const proveedorId = Number(body.querySelector('[name=proveedorId]').value);
        const items = [...body.querySelectorAll('[data-cant]')]
          .map(i => ({ insumoId: Number(i.dataset.cant), cantidad: Number(i.value) })).filter(i => i.cantidad > 0);
        if (!proveedorId) throw new Error('Seleccione el proveedor');
        if (!items.length) throw new Error('Indique la cantidad de al menos un insumo');
        await post('/admin/solicitudes', { proveedorId, items });
        toast('Solicitud enviada al proveedor');
        alGuardar();
      } }
    ]
  });
}

async function pagosProveedores(cont) {
  cargando(cont);
  try {
    const pagos = await get('/admin/pagos-proveedor');
    const pendiente = pagos.filter(p => p.estado === 'PENDIENTE').reduce((a, p) => a + p.monto, 0);
    cont.innerHTML = `
      <div class="grid grid-2" style="margin-bottom:1rem">
        <div class="kpi"><div class="label">Por pagar</div><div class="value">${money(pendiente)}</div></div>
        <div class="kpi"><div class="label">Pagos registrados</div><div class="value">${pagos.length}</div></div></div>
      ${pagos.length ? `<div class="card"><table><thead><tr><th>#</th><th>Proveedor</th><th>Solicitud</th><th>Generado</th><th class="num">Monto</th><th>Estado</th><th></th></tr></thead>
      <tbody>${pagos.map(p => `<tr><td>${p.id}</td><td>${esc(p.proveedor)}</td><td>#${p.solicitudId}</td><td>${fecha(p.fechaGenerado)}</td>
        <td class="num">${money(p.monto)}</td><td>${estado(p.estado === 'PAGADO' ? 'PAGADO_PROV' : p.estado, p.estado)}
        ${p.fechaPago ? `<div class="muted small">${fecha(p.fechaPago)}</div>` : ''}</td>
        <td class="right">${p.estado === 'PENDIENTE' ? `<button class="btn btn-sm btn-success" data-pagar="${p.id}">Registrar pago</button>` : ''}</td></tr>`).join('')}
      </tbody></table></div>` : vacio('No hay pagos a proveedores')}`;
    cont.querySelectorAll('[data-pagar]').forEach(b => b.addEventListener('click', async () => {
      if (!await confirmar('Registrar pago', '¿Confirma que se pagó este valor al proveedor?', 'Registrar pago', 'btn-success')) return;
      try { await post(`/admin/pagos-proveedor/${b.dataset.pagar}/pagar`); toast('Pago registrado'); pagosProveedores(cont); } catch (e) { errorToast(e); }
    }));
  } catch (e) { cont.innerHTML = `<div class="alert alert-error">${esc(e.message)}</div>`; }
}
