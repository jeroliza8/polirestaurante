# PoliRestaurante — módulo de Mesero

Integra el módulo `mesero-java` (consola + JDBC + UUID) al backend Spring Boot y al front existentes.
Misma pila que Cocina: Java 21, Spring Boot 3.3.5, MySQL 8.0, JWT/RBAC, páginas estáticas con módulos ES.
**No se crean tablas ni columnas**: se usa el esquema `01_schema.sql` (+ `04_cocina_auditoria.sql`) con IDs `INT`.

## Qué cambió respecto a `mesero-java`

| `mesero-java` | Aquí |
|---|---|
| Consola (`MeseroConsola`), `exec:java` | `mesero.html` + `js/views/mesero.js`; login común redirige a la página del rol |
| DAO JDBC con `UUID` y tablas `pedidos`, `mesas`… (Flyway V1) | DAO JPA existentes (`PedidoDAO`, `MesaDAO`, `ProductoDAO`…) con `INT`, tablas `Pedido`, `Mesa`… |
| Identifica al mesero por correo | JWT (RF37-38); `MeseroService` revalida usuario activo y rol en cada llamada |
| `SujetoPedidos` / `ObservadorMeseroConsola` | `EventoCocina` + `CocinaSocket` (WebSocket, ADR-001); la pantalla escucha `polirestaurante:cocina` |
| Reglas dentro de `MeseroService` | Reglas puras en `ReglasMesero` (probadas sin BD) y orquestación en `MeseroService` |

## Trazabilidad

| Requisito | Implementación |
|---|---|
| RF01 / HU-MES-001 | `POST /api/mesero/pedidos`: mesa activa y sin cuenta abierta, descuenta existencias atómicamente (orden fijo, sin bloqueos cruzados), crea `Pedido` + `PedidoMesa` + `DetallePedido` y pasa la mesa a OCUPADA. |
| RF02 | `POST /pedidos/{id}/productos`, `DELETE /pedidos/{id}/productos/{detalle}`. Solo PENDIENTE. Quitar = línea `ANULADO` (fecha, motivo y usuario en las columnas de anulación), existencias devueltas, mínimo un producto. |
| RF03 | `PATCH /pedidos/{id}/entregar`: solo desde LISTO; guarda `fecha_entregado`; líneas PENDIENTE → ENTREGADO. |
| RF04 / HU-MES-002 | `PATCH /pedidos/{id}/cancelar`: PENDIENTE (devuelve existencias) o EN_PREPARACION (no las devuelve). Entregado o pagado se rechaza con mensaje claro. Libera la mesa si era su única cuenta. |
| RF05 | `GET /pedidos`: abiertos y cerrados de las últimas 24 h, con estado actual y total. |
| RF06 / HU-MES-003 | `PATCH /mesas/{id}/ocupar` y `/liberar`: no cambia con cuentas abiertas (PENDIENTE…ENTREGADO sin pagar). |
| Carta | `GET /menu`: `activo` y `disponible` y `cantidad_disponible > 0`, el mismo criterio que exige `docs/COCINA.md`, revalidado dentro de la transacción al crear/agregar. |
| RNF12 | Todo error de negocio llega como `{"error": "..."}` (400/404/409) por `GlobalExceptionHandler`. |

Permisos: `/api/mesero/**` solo para `MESERO` (`SecurityConfig`). Un mesero no puede ver ni tocar pedidos de otro.

## Integración con Cocina

Cada creación, edición, entrega o cancelación publica `EventoCocina.pedido(...)` dentro de la transacción; `CocinaSocket`
lo entrega tras el commit, así el tablero de Cocina se actualiza sin esperar la reconciliación de 2 s. A la inversa, cuando
Cocina marca LISTO o cambia disponibilidad, el mesero recibe el aviso por el mismo WebSocket y la pantalla recarga mesas,
carta y pedidos sin recarga manual.

## Decisiones que el equipo debe confirmar

1. **Historial (RF39 / RNF08).** `mesero-java` escribía `historial_estados` y `auditoria_mesas`; esas tablas no existen en el
   modelo compartido y no se crearon. Hoy queda: `fecha_ultima_actualizacion`, `fecha_entregado` y la anulación de líneas
   (con usuario). **No** queda quién cambió el estado de una mesa ni el motivo de una cancelación (solo se registra en el log
   del servidor). Si se exige auditoría completa, hace falta acordar una tabla o columnas, igual que se hizo con `04_cocina_auditoria.sql`.
2. **Existencias.** Se mantiene la semántica del módulo original: crear/agregar descuenta `Producto.cantidad_disponible` y
   cancelar un pedido pendiente o quitar una línea lo devuelve. Si Administración/Caja descuentan inventario por otra vía,
   hay que elegir una sola para no descontar dos veces.
3. **Tipo de pedido.** Los pedidos del mesero se guardan con `tipo_pedido = 'MESA'` (igual que los datos de prueba). Domicilio y
   para llevar siguen fuera de alcance.
4. **Pago.** El modelo no tiene `estado_pago` en `Pedido`: se considera pagado cuando el estado es `PAGADO`. La mesa se libera
   al cancelar; tras pagar, la libera el mesero (RF06), porque en esta rama no existe aún el flujo de Caja.

## Pruebas

- `MeseroReglasTest` (JUnit 5, sin MySQL): cantidades, producto disponible, transiciones de edición/entrega/cancelación,
  devolución de existencias y reglas de mesa. Se ejecuta con `mvn -f backend/pom.xml test`.
- Con los datos de prueba: `mesero1` / `123456` abre `mesero.html`. `mesero3` está inactivo y no puede iniciar sesión.
  Flujo sugerido: crear pedido en la mesa 1 → en `cocinero1` iniciar y marcar listo → en `mesero1` ver el aviso y entregar.
