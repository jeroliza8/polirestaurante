# PoliRestaurante — módulo de Cocina

## Referencia de integración

Repositorio `jeroliza8/polirestaurante`, rama `admin-juliana`, commit
`8bc25622ab8e516565ee7d9a064fe47ff8fa9a50` (26/11/2026).
Esta versión está escrita en Java 21, Spring Boot 3.3.5, Maven, WAR, Tomcat 10.1 y MySQL 8.0.
No sustituye el código de administración/proveedores ni crea un proyecto separado.
La rama `main` observada contenía solo el README inicial; no es la base de este módulo.

## Trazabilidad del documento

| Fuente | Implementación |
|---|---|
| RF07 / HU-COC-01 (pp. 9, 22) | `GET /api/cocinero/tablero`, cola ordenada por fecha e ID; mensaje vacío; WebSocket `ws/cocina`, reconciliación de cambios de BD. |
| RF08 / HU-COC-02 / CU-COC-01 (pp. 22, 36-37) | `PATCH /api/cocinero/pedidos/{id}/estado` con `EN_PREPARACION`; bloqueo de fila, regla State y marca de tiempo. |
| RF09 / HU-COC-02 / CU-COC-01 | Mismo endpoint con `LISTO`, exclusivamente desde `EN_PREPARACION`; evento `PEDIDO_LISTO` dirigido al mesero asociado. |
| RF10 / HU-COC-03 / CU-COC-02 (pp. 23, 38-39) | `PATCH /api/cocinero/productos/{id}/disponibilidad`: agotado y reactivación, confirmación, validación de inexistentes, repetidos y stock. |
| RF38 / ADR-003 | JWT de la base compartida, RBAC `COCINERO`, comprobación del usuario activo y del actor. |
| RF39 | Se guardan fecha y usuario de los dos cambios de estado autorizados a Cocina. No se promete auditar los cambios de otros roles. |
| BPMN Cocina (p. 15) | Consultar, seleccionar, iniciar, elaborar, marcar listo; cancelados no avanzan. |
| Diagramas (pp. 46-48) | `CocineroController` → `CocineroService` → DAO existentes → `DB_Polirestaurante`. |
| State (pp. 88-89) | Interfaz `EstadoCocina` y estados concretos. El enum `EstadoPedido` existente conserva los valores persistidos. |
| Observer (p. 90), ADR-001 (pp. 92-93) | Eventos Spring después de commit y WebSocket autenticado. Notificación por pedido/rol, sin acoplar controladores. |
| ADR-002 y ADR-005 | Mismo MySQL local y WAR; se conserva el despliegue de la rama. |
| Mockups pp. 37 y 39 | Tablero oscuro, tarjetas por estado, botones azules/verdes y disponibilidad roja/verde; estilos aislados de Cocina. |

### Permisos y límites del CRUD

Cocina consulta (Read) y modifica (Update) el estado de los pedidos y la disponibilidad del producto.
No crea ni elimina pedidos, productos, categorías o empleados. No cobra, no marca entregados, no cancela ni edita cantidades de un pedido.
Son responsabilidades de los actores definidos en RF01-RF06 y RF24-RF27.
El campo `Producto.disponible` es el interruptor de Cocina; la cantidad no se cambia artificialmente.
Si las existencias son cero, no se permite reactivar hasta que Administración las reponga.

### Base de datos: sin tablas nuevas

Se reutilizan `Pedido`, `DetallePedido`, `Producto`, `Categoria`, `PedidoMesa`, `Mesa`, `Mesero`, `Usuario`, `Cocinero`, `Administrador`.
Los identificadores son `INT` y se mantienen los nombres/relaciones de la rama compartida.
Los DAO existentes no se reemplazan. `CocinaAuditoriaDAO` solo lee/escribe campos de auditoría adicionales; `CocinaDTO` no tiene `@Entity`.

El esquema de `admin-juliana` contiene `fecha_en_preparacion`, `fecha_listo`, `fecha_ultima_actualizacion`, `disponible`, `activo` y `cantidad_disponible`, pero no el usuario responsable de las dos transiciones ni el de disponibilidad.
Por RF39 y CU-COC-02 se propone `database/04_cocina_auditoria.sql`: cuatro atributos en tablas existentes (`Pedido.Usuario_inicio_preparacion`, `Pedido.Usuario_listo`, `Producto.fecha_disponibilidad`, `Producto.Usuario_disponibilidad`) y tres claves foráneas a `Usuario`.
**No ejecutar este ALTER sin acordarlo con el equipo.** Es obligatorio para esta implementación: debe aplicarse antes de iniciar Cocina. No inventar responsables de datos históricos; los registros previos pueden quedar NULL.
Se registra la última modificación de disponibilidad hecha desde Cocina; no es un historial completo de cada cambio de producto. Las marcas de inicio y fin de preparación se conservan por separado.
No se añade una tabla de historial; para registrar todos los estados de los demás actores será necesaria la decisión compartida de RF39.

## Instalación en el servidor compartido

No se necesita instalar MySQL para subir los fuentes ni ejecutar las pruebas unitarias. El equipo debe utilizar su MySQL 8.0 local, Java 21 y Maven para desplegar.

1. Instalación nueva: aplicar `01_schema.sql`, `04_cocina_auditoria.sql` una vez y, únicamente en BD de prueba, `03_datos_prueba.sql`. Crear/configurar el usuario de aplicación con las instrucciones del equipo. Si el esquema es anterior, primero seguir las instrucciones de `02_migracion_atributos.sql`; no repetir migraciones ya aplicadas.
2. Configurar `DB_URL`, `DB_USER`, `DB_PASSWORD` y `JWT_SECRET` como ya establece `application.yml`. No subir secretos.
3. Ejecutar `mvn -f backend/pom.xml clean package`; desplegar `backend/target/polirestaurante.war` en Tomcat 10.1. Para pruebas locales, el WAR ejecutable puede iniciarse con `java -jar backend/target/polirestaurante.war`.
4. Abrir `http://localhost:8080/polirestaurante/` si se despliega el WAR en Tomcat, o `http://localhost:8080/` si se ejecuta de forma autónoma.
5. Con los datos de prueba oficiales, usar `cocinero1` / `123456` (solo pruebas, nunca producción). El login común redirige a `cocinero.html`.

## Integración con Mesero y Administración

La rama de referencia todavía no contiene `MeseroController` ni `mesero.html`. Por tanto no se implementa su interfaz, ni se crean pedidos desde Cocina.

- La conexión autenticada `/ws/cocina` admite COCINERO, MESERO y ADMINISTRADOR, sin credenciales en URL. El navegador envía el JWT en el primer frame (`{"tipo":"AUTENTICAR","token":"..."}`). Se verifica expiración, usuario activo y rol real. El socket se reconecta y renueva su token desde el `state.js` existente.
- Se reutiliza `shell.js` con un cambio mínimo: Mesero y Administración reciben avisos de Cocina y un evento DOM `polirestaurante:cocina`. El aviso `PEDIDO_LISTO` solo llega al mesero que tiene ese pedido. Administración recibe disponibilidad, sin exponer sus pantallas al rol Cocina.
- El módulo Mesero debe escuchar `polirestaurante:cocina` para recargar pedidos y menú sin recarga manual, y filtrar en servidor `Producto.activo = true`, `Producto.disponible = true` y `cantidad_disponible > 0`. Debe revalidar ese criterio al agregar y confirmar un pedido, dentro de su transacción. Este control en otro módulo **no puede certificarse hasta que se integre el código de Mesero**.
- Para notificación inmediata al registrar/cancelar desde otro servicio, publicar dentro de su transacción `eventos.publishEvent(EventoCocina.pedido(idPedido, meseroId, estado))`. No llamar a `CocineroController`. Si aún no emite eventos, Cocina detecta cambios con una lectura compartida del tablero cada 2 segundos en el servidor y avisa por WebSocket solo al cambiar contenido; no se hace HTTP polling periódico en el navegador. Esta reconciliación es una solución de integración temporal: la latencia depende de BD/red. Conservar notificaciones para clientes desconectados queda fuera del alcance (no hay tabla nueva); al reconectar se consulta el estado actual.
- La pantalla de Cocina permite consulta manual si se pierde conexión y muestra un indicador de desconexión. No se simula que una actualización haya llegado.

Ejemplo de suscripción para el encargado de Mesero (no copiar dentro de Cocina):

```js
window.addEventListener('polirestaurante:cocina', ({detail}) => {
  // Usar los métodos reales de la vista de Mesero una vez se integren.
  // PEDIDO_LISTO/PEDIDO_ACTUALIZADO: recargar pedidos asignados.
  // DISPONIBILIDAD_ACTUALIZADA: recargar carta desde el backend.
});
```

## Pruebas

- `CocinaReglasTest`: validación de todas las combinaciones de estados, State, cancelados, cambios repetidos y disponibilidad; no necesita MySQL.
- `cocina-ci.yml`: compila el WAR con Java 21 y Maven, crea una base MySQL 8.0 desechable con los scripts del equipo, ejecuta las pruebas unitarias y el recorrido HTTP/WS `cocina-smoke.mjs`. No utiliza la BD del equipo. Revisa la pestaña Actions de GitHub cuando subas la rama `feature/cocina`.
- Para el recorrido visual, iniciar sesión como Cocinero y verificar detalle, iniciar preparación, marcar listo, disponibilidad y reactivación. Los datos se consultan de la API, no están codificados en las pantallas.
- La ejecución completa de Maven, MySQL y WebSocket en vivo queda pendiente de CI/servidor. La comprobación local entregada solo corresponde a las verificaciones registradas en el archivo de evidencia del ZIP. No es una certificación de que todo el repositorio esté libre de errores.
