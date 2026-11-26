# PoliRestaurante — Módulos Administrador, Cajero y Proveedor

Ingeniería de Software 2 · Politécnico Grancolombiano · 2026

Implementación de las funcionalidades de **Administrador** y **Cajero** descritas en las historias
de usuario y casos de uso del documento, más todo lo relacionado con **proveedores**
(RF32–RF36). Respeta el diagrama de despliegue, el diagrama de componentes y los ADR.

## Estructura (arquitectura en capas — ADR-004, RNF14)

```
PoliRestaurante/
├── database/
│   ├── 00_usuario_aplicacion.sql   usuario MySQL de la app (solo localhost)
│   ├── 01_schema.sql               las MISMAS 23 tablas + atributos nuevos marcados [NUEVO]
│   ├── 02_migracion_atributos.sql  solo ALTER, para una base ya creada con el esquema anterior
│   └── 03_datos_prueba.sql         usuarios, menú, mesas, cuentas abiertas, ventas históricas
├── backend/                        proyecto Maven → polirestaurante.war (Tomcat 10.1, Java 21)
│   └── src/main/java/com/polirestaurante/
│       ├── controller/   CAPA DE PRESENTACIÓN  AdministradorController, CajeroController,
│       │                                       ProveedorController, AuthController
│       ├── service/      CAPA DE NEGOCIO       ProductoService, UsuarioService, ProveedorService,
│       │   ├── factory/                        CajeroService, CalculadoraCuenta
│       │   ├── pago/         Strategy (medios de pago)
│       │   ├── integracion/  pasarela de pagos y factura electrónica (RNF10)
│       │   └── reporte/      exportación PDF / Excel
│       ├── dao/          CAPA DE DATOS         un DAO por tabla (Spring Data JPA) + PromocionDAO
│       ├── entity/       DOMINIO               una entidad por tabla
│       ├── dto/          objetos de entrada/salida de la API (records)
│       ├── security/     JWT + RBAC (ADR-003)
│       ├── config/       seguridad y parámetros (application.yml / variables de entorno)
│       ├── enums/  exception/
│   └── src/main/resources/static/      interfaz web (HTML + CSS + JS), va dentro del WAR
├── deploy/   setenv.bat (variables de Tomcat), respaldo_bd.bat
└── docs/DESPLIEGUE.md   paso a paso en Windows Server 2022
```

La interfaz llama solo a los controladores; los controladores solo a los servicios; solo los
DAO tocan la base. No hay dependencias directas entre presentación y datos.

## Las 10 funcionalidades

| # | Funcionalidad | HU / CU | RF | Dónde |
|---|---|---|---|---|
| 1 | Apertura y cierre de caja con reporte de ventas del turno ("C-producto") | prerrequisito de CU-CAJ-001/002/003 | RF11 | `CajeroService` · pestaña *Caja* |
| 2 | Registrar el pago con medio de pago y generar factura física o electrónica | HU-CAJ-001 · CU-CAJ-001 | RF16, RF19, RF20 | `CajeroService.registrarPago / generarFactura` |
| 3 | Consultar y aplicar descuentos o promociones | CU-CAJ-002 | RF22, RF23 | `CajeroService.consultarPromociones / aplicarPromocion` |
| 4 | Consultar cuentas, anular productos y cancelar cuentas no pagadas | CU-CAJ-003 | RF12–RF15, RF17 | `CajeroService.anularProducto / cancelarCuenta` |
| 5 | Historial de facturas con método de pago y reimpresión | HU-CAJ-002 | RF18, RF21 | `CajeroService.historialFacturas / reimprimirFactura` |
| 6 | Administrar productos del menú y categorías | HU-ADM-01 · CU-ADM-04 · CU-ADM-05 | RF24, RF25 | `ProductoService` · pestaña *Menú* |
| 7 | Usuarios y roles; desactivar / reactivar cuentas | HU-ADM-02 · CU-ADM-03 | RF27, RF37, RF38 | `UsuarioService` (+ `EmpleadoFactory`) |
| 8 | Administrar mesas (número no reasignable, sugerencia de número, bloqueo si hay pedido) | CU-ADM-02 | RF26 | `UsuarioService` (como en el diagrama de secuencia) |
| 9 | Reportes: ventas por periodo (PDF/Excel), tiempos de preparación, agotados diario | CU-ADM-01 · CU-ADM-06 · CU-ADM-08 | RF28, RF29, RF31 | `ProductoService` · pestaña *Reportes* |
| 10 | Inventario, ingreso de mercancía y proveedores (registro, solicitudes, pagos y portal del proveedor) | CU-ADM-07 · CU-ADM-09 · CU-ADM-10 | RF30, RF32–RF36 | `ProductoService`, `ProveedorService`, `ProveedorController` |

Los nombres de los métodos siguen los diagramas de secuencia (`obtenerReporteVentas`,
`registrarMesa`, `cambiarEstadoUsuario`, `registrarProveedor`, `sumarCantidadDisponible`,
`buscarPromocionesVigentes`, `consultarDetalleCuenta`, `anularProducto`, `generarFactura`…).

## Base de datos: solo atributos nuevos

No se creó ninguna tabla. Se mantienen las 23 del modelo y se agregaron columnas donde los
casos de uso lo exigían (cada una comentada con su RF/CU en `01_schema.sql`):

- **Pago** guarda también los datos de la factura (número, tipo, CUFE, subtotal, descuento,
  impuesto, cajero y caja): no hay tabla Factura en el modelo.
- **DetallePedido**: `estado`, `precio_unitario` (la reimpresión muestra lo cobrado) y los datos
  de anulación (fecha, motivo, usuario) para el control interno de CU-CAJ-003.
- **Pedido**: marcas de tiempo por etapa (RF29) y `promocion_aplicada` (CU-CAJ-002).
- **Caja**: `estado`, `total_ventas`. **Mesa**: `capacidad`, `activa`. **Producto**: `disponible`,
  `activo`, `stock_minimo`. **Insumo**: `unidad_medida`, `stock_minimo`.
- **Proveedor**: `Usuario_idUsuario` (login del proveedor). **PedidoInsumo**: proveedor y fecha de
  recepción; los insumos pedidos usan la tabla existente **DetallePedidoInsumo** (+ precio).
- **Promociones**: como no hay tabla, el catálogo está en `application.yml`
  (`polirestaurante.promociones`) y lo lee `PromocionDAO`.

## Cumplimiento de ADR y patrones

| Decisión | Cómo se cumple |
|---|---|
| ADR-002 MySQL relacional | MySQL 8.0, transacciones ACID; el cobro bloquea la fila del pedido (no hay doble pago) |
| ADR-003 RBAC + JWT | Token de 30 min, `/api/admin/**` solo ADMINISTRADOR, `/api/cajero/**` solo CAJERO, `/api/proveedor/**` solo PROVEEDOR; un usuario desactivado pierde el acceso al instante |
| ADR-004 capas | controller → service → dao → entity |
| ADR-005 un servidor | WAR en Tomcat 10.1 + MySQL 8.0 en `localhost`; scripts de respaldo |
| Strategy (Cajero) | `MedioPagoStrategy`: `EfectivoStrategy`, `TarjetaStrategy`, `TransferenciaStrategy` |
| Factory (Administración) | `EmpleadoFactory` + interfaz `Empleado` |
| RNF05 | BCrypt; HTTPS configurado en Tomcat (ver guía) |
| RNF10 | Interfaces `PasarelaPagos` y `ProveedorFacturaElectronica`; si la FE falla, la cuenta queda pagada y se reintenta |
| RNF12 | Todas las validaciones responden `{"error": "..."}` con el mensaje del caso de uso |
| RNF15 | Credenciales en variables de entorno (`setenv.bat`) |
| RNF17 | Pruebas JUnit de totales, descuentos, impuestos, medios de pago y fábrica |

ADR-001 (WebSockets) y los patrones State y Observer pertenecen a los módulos de Cocina y
Mesero, que no hacen parte de esta entrega. La lista de cuentas del cajero se actualiza sola cada
30 s.

## Usuarios de prueba (`03_datos_prueba.sql`) — contraseña `123456`

| Usuario | Rol |
|---|---|
| `admin` | Administrador |
| `cajero1`, `cajero2` | Cajero |
| `prov1`, `prov2` | Proveedor |
| `mesero1`, `mesero2`, `cocinero1`, `repartidor1` | Otros roles (sus módulos no están en esta entrega) |
| `mesero3` | Mesero **desactivado** (para probar CU-ADM-03) |

Para probar los flujos de excepción con la pasarela simulada: tarjeta terminada en **0000** o
transferencia con referencia que empiece por **RECH** → *pago rechazado*; correo de factura
electrónica que contenga **falla** → la FE queda pendiente y se reintenta desde el historial.

## Ejecutar

Ver **[docs/DESPLIEGUE.md](docs/DESPLIEGUE.md)**. En resumen: scripts SQL → `mvn clean package`
→ copiar `polirestaurante.war` a `webapps` de Tomcat 10.1 con `setenv.bat` →
`http://servidor:8080/polirestaurante/`.
