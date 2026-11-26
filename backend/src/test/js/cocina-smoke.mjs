// Integración real: solo contra la BD DE PRUEBAS de cocina-ci.yml.
import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
const BASE = process.env.COCINA_BASE || 'http://localhost:8080';
const DB = process.env.COCINA_TEST_DB_CONTAINER;
assert.ok(DB, 'Ejecutar únicamente con el contenedor desechable de CI');
function sql(sentencia) {
  return execFileSync('docker', ['exec', DB, 'mysql', '-uroot', '-pci_root_password', '-N', '-B', 'DB_Polirestaurante', '-e', sentencia], { encoding: 'utf8' }).trim();
}
async function api(method, path, token, body, expected = 200) {
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const r = await fetch(BASE + '/api' + path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  const raw = await r.text();
  assert.equal(r.status, expected, `${method} ${path}: ${raw}`);
  return raw ? JSON.parse(raw) : null;
}
const login = async usuario => (await api('POST','/auth/login',null,{usuario,password:'123456'})).token;
const wsBase = new URL('/ws/cocina', BASE); wsBase.protocol = 'ws:';
function socket(token) {
  const ws = new WebSocket(wsBase), eventos = [];
  ws.addEventListener('message', e => eventos.push(JSON.parse(e.data)));
  ws.addEventListener('open', () => ws.send(JSON.stringify({tipo:'AUTENTICAR',token})));
  return {ws,eventos};
}
const sleep = ms => new Promise(r => setTimeout(r,ms));
async function esperar(c, criterio, descripcion) {
  for (let i=0;i<80;i++) { const e = c.eventos.find(criterio); if (e) return e; await sleep(150); }
  throw new Error('No se recibió: ' + descripcion);
}
const cocina = await login('cocinero1'), admin = await login('admin'), mesero = await login('mesero1'), otro = await login('mesero2');
await api('GET','/cocinero/tablero',null,undefined,401);
await api('GET','/cocinero/tablero',admin,undefined,403);
await api('GET','/cocinero/tablero',mesero,undefined,403);
const c = socket(cocina), m = socket(mesero), o = socket(otro), a = socket(admin);
try {
  await Promise.all([c,m,o,a].map(x => esperar(x,e=>e.tipo==='CONECTADO','autenticación WebSocket')));
  await esperar(c,e=>e.tipo==='TABLERO_CAMBIO','instantánea inicial');
  c.eventos.length = 0;
  let tablero = await api('GET','/cocinero/tablero',cocina);
  assert.ok(Array.isArray(tablero.pedidos) && Array.isArray(tablero.productos));
  for (const p of tablero.pedidos) assert.ok(['PENDIENTE','EN_PREPARACION','LISTO'].includes(p.estado));
  const prod = tablero.productos.find(p=>p.disponible && p.cantidadDisponible>0);
  assert.ok(prod,'Debe existir un producto disponible');
  // Pedido creado por el mesero a través de las tablas reales, no una simulación del navegador.
  // La rama base aún no tiene MeseroController; se inserta un dato de prueba en MySQL.
  const id = Number(sql(`INSERT INTO Pedido (estado,fecha_creacion,fecha_ultima_actualizacion,tipo_pedido,Mesero_id_mesero) VALUES ('PENDIENTE',NOW(),NOW(),'MESA',1); SET @p=LAST_INSERT_ID(); INSERT INTO DetallePedido (cantidad,notas,Producto_idProducto,Pedido_id_pedido,estado) VALUES (2,'Sin cebolla',${prod.id},@p,'PENDIENTE'); INSERT INTO PedidoMesa (Mesa_id_mesa,Pedido_id_pedido) VALUES (1,@p); SELECT @p;`));
  assert.ok(id > 0);
  await esperar(c,e=>e.tipo==='TABLERO_CAMBIO','nuevo pedido');
  let p = await api('GET',`/cocinero/pedidos/${id}`,cocina);
  assert.equal(p.estado,'PENDIENTE'); assert.equal(p.items[0].notas,'Sin cebolla'); assert.equal(p.items[0].cantidad,2);
  await api('PATCH',`/cocinero/pedidos/${id}/estado`,cocina,{estado:'LISTO'},400);
  await api('PATCH',`/cocinero/pedidos/${id}/estado`,admin,{estado:'EN_PREPARACION'},403);
  await api('PATCH',`/cocinero/pedidos/${id}/estado`,cocina,{estado:'EN_PREPARACION'},204);
  p = await api('GET',`/cocinero/pedidos/${id}`,cocina); assert.equal(p.estado,'EN_PREPARACION'); assert.ok(p.fechaEnPreparacion); assert.equal(p.usuarioPreparacion,6);
  await api('PATCH',`/cocinero/pedidos/${id}/estado`,cocina,{estado:'EN_PREPARACION'},400);
  await api('PATCH',`/cocinero/pedidos/${id}/estado`,cocina,{estado:'LISTO'},204);
  await esperar(m,e=>e.tipo==='PEDIDO_LISTO' && e.idPedido===id,'notificación al mesero asociado');
  await esperar(c,e=>e.tipo==='PEDIDO_LISTO' && e.idPedido===id,'notificación a cocina');
  assert.ok(!o.eventos.some(e=>e.tipo==='PEDIDO_LISTO' && e.idPedido===id));
  p=await api('GET',`/cocinero/pedidos/${id}`,cocina); assert.equal(p.estado,'LISTO'); assert.ok(p.fechaListo); assert.equal(p.usuarioListo,6);
  await api('PATCH',`/cocinero/pedidos/${id}/estado`,cocina,{estado:'ENTREGADO'},400);
  const itemsAntes = p.items;
  await api('PATCH',`/cocinero/productos/${prod.id}/disponibilidad`,cocina,{disponible:false},204);
  await esperar(a,e=>e.tipo==='DISPONIBILIDAD_ACTUALIZADA'&&e.idProducto===prod.id&&e.disponible===false,'notificación a Administración');
  await esperar(m,e=>e.tipo==='DISPONIBILIDAD_ACTUALIZADA'&&e.idProducto===prod.id&&e.disponible===false,'notificación de menú a Mesero');
  tablero=await api('GET','/cocinero/tablero',cocina);
  const agotado=tablero.productos.find(x=>x.id===prod.id); assert.equal(agotado.disponible,false); assert.equal(agotado.cantidadDisponible,prod.cantidadDisponible); assert.ok(agotado.fechaDisponibilidad); assert.equal(agotado.usuarioDisponibilidad,6);
  p=await api('GET',`/cocinero/pedidos/${id}`,cocina); assert.deepEqual(p.items,itemsAntes); assert.equal(p.estado,'LISTO');
  await api('PATCH',`/cocinero/productos/${prod.id}/disponibilidad`,cocina,{disponible:false},409);
  await api('PATCH',`/cocinero/productos/${prod.id}/disponibilidad`,cocina,{disponible:true},204);
  await api('PATCH','/cocinero/productos/999999/disponibilidad',cocina,{disponible:false},404);
  const cancelado=Number(sql("INSERT INTO Pedido (estado,fecha_creacion,fecha_ultima_actualizacion,tipo_pedido,Mesero_id_mesero) VALUES ('CANCELADO',NOW(),NOW(),'MESA',1); SELECT LAST_INSERT_ID();"));
  await api('PATCH',`/cocinero/pedidos/${cancelado}/estado`,cocina,{estado:'EN_PREPARACION'},400);
  const final = await api('GET','/cocinero/tablero',cocina); assert.ok(!final.pedidos.some(x=>x.id===cancelado));
  assert.equal(sql(`SELECT CONCAT(estado,':',Usuario_inicio_preparacion,':',Usuario_listo) FROM Pedido WHERE id_pedido=${id}`),'LISTO:6:6');
  console.log('OK: RBAC, datos reales MySQL, detalle, State, auditoría, disponibilidad y WebSockets.');
} finally { for (const x of [c,m,o,a]) x.ws.close(); }
