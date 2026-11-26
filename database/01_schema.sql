-- ============================================================
-- 01_schema.sql  -  Base de datos de PoliRestaurante
-- Motor: MySQL 8.0 (InnoDB)   Esquema: DB_Polirestaurante
-- Nodo: "MySQL 8.0 - DBServer" dentro del servidor Windows Server
--       (diagrama de despliegue / ADR-002 / ADR-005)
--
-- Se respetan las MISMAS 23 tablas del modelo relacional. No se crea
-- ninguna tabla nueva. Las unicas diferencias frente a la version
-- anterior son ATRIBUTOS (columnas) agregados, marcados con  -- [NUEVO]
-- y la razon (RF / CU / HU / RNF) que los exige.
--
-- Instalacion limpia: ejecute este archivo y luego 03_datos_prueba.sql.
-- Si ya tiene la base creada con la version anterior, ejecute en su
-- lugar 02_migracion_atributos.sql (una sola vez) y luego 03.
-- ============================================================

CREATE DATABASE IF NOT EXISTS DB_Polirestaurante CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE DB_Polirestaurante;

CREATE TABLE IF NOT EXISTS `Categoria` (
  `id_categoria` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(100) DEFAULT NULL,
  PRIMARY KEY (`id_categoria`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `Cliente` (
  `id_cliente` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(45) DEFAULT NULL,
  `direccion` VARCHAR(45) DEFAULT NULL,
  `telefono` VARCHAR(45) DEFAULT NULL,
  PRIMARY KEY (`id_cliente`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `Insumo` (
  `id_insumo` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(100) DEFAULT NULL,
  `cantidad_disponible` INT DEFAULT NULL,
  `unidad_medida` VARCHAR(20) NOT NULL DEFAULT 'unidad',  -- [NUEVO] RF30/CU-ADM-07: inventario legible (kg, litro, unidad)
  `stock_minimo` INT NOT NULL DEFAULT 5,                  -- [NUEVO] RF31/CU-ADM-08: umbral de "baja disponibilidad"
  PRIMARY KEY (`id_insumo`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `Mesa` (
  `id_mesa` INT NOT NULL AUTO_INCREMENT,
  `numero` INT DEFAULT NULL,
  `estado` VARCHAR(45) DEFAULT NULL,
  `capacidad` INT NOT NULL DEFAULT 4,                     -- [NUEVO] CU-ADM-02: "capacidad y disposicion"
  `activa` TINYINT(1) NOT NULL DEFAULT 1,                 -- [NUEVO] CU-ADM-02: eliminacion logica; el numero no se reasigna
  PRIMARY KEY (`id_mesa`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `Usuario` (
  `idUsuario` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(100) DEFAULT NULL,
  `credenciales` VARCHAR(255) DEFAULT NULL,
  `estado` VARCHAR(20) NOT NULL DEFAULT 'activo',
  PRIMARY KEY (`idUsuario`),
  UNIQUE KEY `uk_Usuario_nombre` (`nombre`)               -- [NUEVO] RF37: el nombre de usuario es el login, no se repite
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `Proveedor` (
  `id_proveedor` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(100) DEFAULT NULL,
  `contacto` VARCHAR(100) DEFAULT NULL,
  `Usuario_idUsuario` INT DEFAULT NULL,                   -- [NUEVO] RF34-RF36: el proveedor inicia sesion con su usuario
  PRIMARY KEY (`id_proveedor`),
  INDEX `fk_Proveedor_Usuario1_idx` (`Usuario_idUsuario`),
  CONSTRAINT `fk_Proveedor_Usuario1` FOREIGN KEY (`Usuario_idUsuario`) REFERENCES `Usuario` (`idUsuario`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `Administrador` (
  `id_administrador` INT NOT NULL AUTO_INCREMENT,
  `Usuario_idUsuario` INT NOT NULL,
  PRIMARY KEY (`id_administrador`),
  INDEX `fk_Administrador_Usuario1_idx` (`Usuario_idUsuario`),
  CONSTRAINT `fk_Administrador_Usuario1` FOREIGN KEY (`Usuario_idUsuario`) REFERENCES `Usuario` (`idUsuario`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `Cajero` (
  `id_cajero` INT NOT NULL AUTO_INCREMENT,
  `Usuario_idUsuario` INT NOT NULL,
  PRIMARY KEY (`id_cajero`),
  INDEX `fk_Cajero_Usuario1_idx` (`Usuario_idUsuario`),
  CONSTRAINT `fk_Cajero_Usuario1` FOREIGN KEY (`Usuario_idUsuario`) REFERENCES `Usuario` (`idUsuario`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `Cocinero` (
  `id_cocinero` INT NOT NULL AUTO_INCREMENT,
  `estacion` VARCHAR(45) DEFAULT NULL,
  `Usuario_idUsuario` INT NOT NULL,
  PRIMARY KEY (`id_cocinero`),
  INDEX `fk_Cocinero_Usuario1_idx` (`Usuario_idUsuario`),
  CONSTRAINT `fk_Cocinero_Usuario1` FOREIGN KEY (`Usuario_idUsuario`) REFERENCES `Usuario` (`idUsuario`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `InsumoProveedor` (
  `id_insumo_proveedor` INT NOT NULL AUTO_INCREMENT,
  `precio` DOUBLE DEFAULT NULL,
  `Proveedor_id_proveedor1` INT NOT NULL,
  `Insumo_id_insumo1` INT NOT NULL,
  `disponible` TINYINT(1) NOT NULL DEFAULT 1,             -- [NUEVO] RF34: el proveedor actualiza la disponibilidad de su catalogo
  PRIMARY KEY (`id_insumo_proveedor`),
  INDEX `fk_InsumoProveedor_Proveedor2_idx` (`Proveedor_id_proveedor1`),
  INDEX `fk_InsumoProveedor_Insumo2_idx` (`Insumo_id_insumo1`),
  CONSTRAINT `fk_InsumoProveedor_Proveedor2` FOREIGN KEY (`Proveedor_id_proveedor1`) REFERENCES `Proveedor` (`id_proveedor`),
  CONSTRAINT `fk_InsumoProveedor_Insumo2` FOREIGN KEY (`Insumo_id_insumo1`) REFERENCES `Insumo` (`id_insumo`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `Mesero` (
  `id_mesero` INT NOT NULL AUTO_INCREMENT,
  `zona_asignada` VARCHAR(45) DEFAULT NULL,
  `Usuario_idUsuario` INT NOT NULL,
  PRIMARY KEY (`id_mesero`),
  INDEX `fk_Mesero_Usuario1_idx` (`Usuario_idUsuario`),
  CONSTRAINT `fk_Mesero_Usuario1` FOREIGN KEY (`Usuario_idUsuario`) REFERENCES `Usuario` (`idUsuario`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `Producto` (
  `idProducto` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(100) DEFAULT NULL,
  `descripcion` VARCHAR(100) DEFAULT NULL,
  `precio` DOUBLE DEFAULT NULL,
  `cantidad_disponible` INT DEFAULT NULL,
  `Categoria_id_categoria` INT NOT NULL,
  `disponible` TINYINT(1) NOT NULL DEFAULT 1,             -- [NUEVO] RF24/RF10: disponibilidad del producto en el menu
  `activo` TINYINT(1) NOT NULL DEFAULT 1,                 -- [NUEVO] HU-ADM-01: eliminacion logica (los pedidos historicos lo referencian)
  `stock_minimo` INT NOT NULL DEFAULT 5,                  -- [NUEVO] RF31: umbral de baja disponibilidad
  PRIMARY KEY (`idProducto`),
  INDEX `fk_Producto_Categoria1_idx` (`Categoria_id_categoria`),
  CONSTRAINT `fk_Producto_Categoria1` FOREIGN KEY (`Categoria_id_categoria`) REFERENCES `Categoria` (`id_categoria`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `Repartidor` (
  `id_repartidor` INT NOT NULL AUTO_INCREMENT,
  `placa_vehiculo` VARCHAR(45) DEFAULT NULL,
  `Usuario_idUsuario` INT NOT NULL,
  PRIMARY KEY (`id_repartidor`),
  INDEX `fk_Repartidor_Usuario1_idx` (`Usuario_idUsuario`),
  CONSTRAINT `fk_Repartidor_Usuario1` FOREIGN KEY (`Usuario_idUsuario`) REFERENCES `Usuario` (`idUsuario`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `Reservacion` (
  `id_reservacion` INT NOT NULL AUTO_INCREMENT,
  `fecha` DATETIME DEFAULT NULL,
  `hora` DATETIME DEFAULT NULL,
  `numero_personas` INT DEFAULT NULL,
  `estado` VARCHAR(45) DEFAULT NULL,
  `Cliente_id_cliente` INT NOT NULL,
  PRIMARY KEY (`id_reservacion`),
  INDEX `fk_Reservacion_Cliente1_idx` (`Cliente_id_cliente`),
  CONSTRAINT `fk_Reservacion_Cliente1` FOREIGN KEY (`Cliente_id_cliente`) REFERENCES `Cliente` (`id_cliente`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `Caja` (
  `id_caja` INT NOT NULL AUTO_INCREMENT,
  `fecha_apertura` DATETIME DEFAULT NULL,
  `fecha_cierre` DATETIME DEFAULT NULL,
  `monto_inicial` DOUBLE DEFAULT NULL,
  `monto_final` DOUBLE DEFAULT NULL,
  `Cajero_id_cajero` INT NOT NULL,
  `estado` VARCHAR(20) NOT NULL DEFAULT 'CERRADA',        -- [NUEVO] RF11: saber si la caja esta ABIERTA (prerrequisito de CU-CAJ-001/002/003)
  `total_ventas` DOUBLE DEFAULT NULL,                     -- [NUEVO] Actor Cajero: reporte de ventas al cierre de caja
  PRIMARY KEY (`id_caja`),
  INDEX `fk_Caja_Cajero1_idx` (`Cajero_id_cajero`),
  CONSTRAINT `fk_Caja_Cajero1` FOREIGN KEY (`Cajero_id_cajero`) REFERENCES `Cajero` (`id_cajero`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `Pedido` (
  `id_pedido` INT NOT NULL AUTO_INCREMENT,
  `estado` VARCHAR(45) DEFAULT NULL,                      -- PENDIENTE, EN_PREPARACION, LISTO, ENTREGADO, CANCELADO, PAGADO
  `fecha_creacion` DATETIME DEFAULT NULL,
  `fecha_ultima_actualizacion` DATETIME DEFAULT NULL,
  `tipo_pedido` VARCHAR(45) DEFAULT NULL,
  `Mesero_id_mesero` INT NOT NULL,
  `Repartidor_id_repartidor` INT DEFAULT NULL,
  `Cliente_id_cliente` INT DEFAULT NULL,
  `fecha_en_preparacion` DATETIME DEFAULT NULL,           -- [NUEVO] RF29/CU-ADM-06: tiempos de preparacion por etapa
  `fecha_listo` DATETIME DEFAULT NULL,                    -- [NUEVO] RF29/CU-ADM-06
  `fecha_entregado` DATETIME DEFAULT NULL,                -- [NUEVO] RF29/CU-ADM-06
  `promocion_aplicada` VARCHAR(50) DEFAULT NULL,          -- [NUEVO] CU-CAJ-002: la promocion queda aplicada en la cuenta
  PRIMARY KEY (`id_pedido`),
  INDEX `fk_Pedido_Mesero1_idx` (`Mesero_id_mesero`),
  INDEX `fk_Pedido_Repartidor1_idx` (`Repartidor_id_repartidor`),
  INDEX `fk_Pedido_Cliente1_idx` (`Cliente_id_cliente`),
  INDEX `idx_Pedido_estado` (`estado`),                   -- [NUEVO] RNF01: consulta rapida de cuentas pendientes
  CONSTRAINT `fk_Pedido_Mesero1` FOREIGN KEY (`Mesero_id_mesero`) REFERENCES `Mesero` (`id_mesero`),
  CONSTRAINT `fk_Pedido_Repartidor1` FOREIGN KEY (`Repartidor_id_repartidor`) REFERENCES `Repartidor` (`id_repartidor`),
  CONSTRAINT `fk_Pedido_Cliente1` FOREIGN KEY (`Cliente_id_cliente`) REFERENCES `Cliente` (`id_cliente`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `PedidoInsumo` (
  `id_pedido_insumo` INT NOT NULL AUTO_INCREMENT,
  `fecha` DATETIME DEFAULT NULL,
  `estado` VARCHAR(45) DEFAULT NULL,                      -- PENDIENTE, DESPACHADA, RECIBIDA, CANCELADA
  `Administrador_id_administrador` INT NOT NULL,
  `Proveedor_id_proveedor` INT DEFAULT NULL,              -- [NUEVO] RF35: a que proveedor se le envia la solicitud
  `fecha_recepcion` DATETIME DEFAULT NULL,                -- [NUEVO] RF33/RF34: fecha de la entrega recibida
  PRIMARY KEY (`id_pedido_insumo`),
  INDEX `fk_PedidoInsumo_Administrador1_idx` (`Administrador_id_administrador`),
  INDEX `fk_PedidoInsumo_Proveedor1_idx` (`Proveedor_id_proveedor`),
  CONSTRAINT `fk_PedidoInsumo_Administrador1` FOREIGN KEY (`Administrador_id_administrador`) REFERENCES `Administrador` (`id_administrador`),
  CONSTRAINT `fk_PedidoInsumo_Proveedor1` FOREIGN KEY (`Proveedor_id_proveedor`) REFERENCES `Proveedor` (`id_proveedor`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `ReservacionMesa` (
  `id_reservacion_mesa` INT NOT NULL AUTO_INCREMENT,
  `Mesa_id_mesa` INT NOT NULL,
  `Reservacion_id_reservacion` INT NOT NULL,
  PRIMARY KEY (`id_reservacion_mesa`),
  INDEX `fk_PedidoMesa_Mesa1_idx` (`Mesa_id_mesa`),
  INDEX `fk_PedidoMesa_copy1_Reservacion1_idx` (`Reservacion_id_reservacion`),
  CONSTRAINT `fk_PedidoMesa_Mesa10` FOREIGN KEY (`Mesa_id_mesa`) REFERENCES `Mesa` (`id_mesa`),
  CONSTRAINT `fk_PedidoMesa_copy1_Reservacion1` FOREIGN KEY (`Reservacion_id_reservacion`) REFERENCES `Reservacion` (`id_reservacion`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `DetallePedido` (
  `id_detalle_pedido` INT NOT NULL AUTO_INCREMENT,
  `cantidad` INT DEFAULT NULL,
  `notas` TEXT DEFAULT NULL,
  `Producto_idProducto` INT NOT NULL,
  `Pedido_id_pedido` INT NOT NULL,
  `precio_unitario` DOUBLE DEFAULT NULL,                  -- [NUEVO] RF21: la reimpresion muestra el precio cobrado, no el actual
  `estado` VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',      -- [NUEVO] RF14/RF15: PENDIENTE, ENTREGADO, ANULADO
  `fecha_anulacion` DATETIME DEFAULT NULL,                -- [NUEVO] CU-CAJ-003: "guarda esta anulacion para el control interno"
  `motivo_anulacion` VARCHAR(255) DEFAULT NULL,           -- [NUEVO] CU-CAJ-003
  `Usuario_anulacion` INT DEFAULT NULL,                   -- [NUEVO] CU-CAJ-003: quien anulo
  PRIMARY KEY (`id_detalle_pedido`),
  INDEX `fk_DetallePedido_Producto1_idx` (`Producto_idProducto`),
  INDEX `fk_DetallePedido_Pedido1_idx` (`Pedido_id_pedido`),
  INDEX `fk_DetallePedido_Usuario1_idx` (`Usuario_anulacion`),
  CONSTRAINT `fk_DetallePedido_Producto1` FOREIGN KEY (`Producto_idProducto`) REFERENCES `Producto` (`idProducto`),
  CONSTRAINT `fk_DetallePedido_Pedido1` FOREIGN KEY (`Pedido_id_pedido`) REFERENCES `Pedido` (`id_pedido`),
  CONSTRAINT `fk_DetallePedido_Usuario1` FOREIGN KEY (`Usuario_anulacion`) REFERENCES `Usuario` (`idUsuario`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `DetallePedidoInsumo` (
  `id_detalle_pedido_insumo` INT NOT NULL AUTO_INCREMENT,
  `cantidad` INT DEFAULT NULL,
  `Insumo_id_insumo1` INT NOT NULL,
  `PedidoInsumo_id_pedido_insumo` INT NOT NULL,
  `precio_unitario` DOUBLE DEFAULT NULL,                  -- [NUEVO] RF36: valor a pagar al proveedor segun precio pactado
  PRIMARY KEY (`id_detalle_pedido_insumo`),
  INDEX `fk_DetallePedidoInsumo_Insumo2_idx` (`Insumo_id_insumo1`),
  INDEX `fk_DetallePedidoInsumo_PedidoInsumo1_idx` (`PedidoInsumo_id_pedido_insumo`),
  CONSTRAINT `fk_DetallePedidoInsumo_Insumo2` FOREIGN KEY (`Insumo_id_insumo1`) REFERENCES `Insumo` (`id_insumo`),
  CONSTRAINT `fk_DetallePedidoInsumo_PedidoInsumo1` FOREIGN KEY (`PedidoInsumo_id_pedido_insumo`) REFERENCES `PedidoInsumo` (`id_pedido_insumo`)
) ENGINE=InnoDB;

-- Pago: cada pago aprobado es tambien la factura (RF16, RF18-RF21).
-- No se crea tabla Factura: sus datos son atributos nuevos de Pago.
CREATE TABLE IF NOT EXISTS `Pago` (
  `id_pago` INT NOT NULL AUTO_INCREMENT,
  `monto` DOUBLE DEFAULT NULL,                            -- total cobrado (ya con descuento, impuesto incluido)
  `metodo_pago` VARCHAR(45) DEFAULT NULL,                 -- EFECTIVO, TARJETA, TRANSFERENCIA
  `fecha_pago` DATETIME DEFAULT NULL,
  `Pedido_id_pedido` INT NOT NULL,
  `estado` VARCHAR(20) NOT NULL DEFAULT 'APROBADO',       -- [NUEVO] HU-CAJ-001 AC3
  `subtotal` DOUBLE DEFAULT NULL,                         -- [NUEVO] CU-CAJ-002: valor antes de descuento
  `descuento` DOUBLE NOT NULL DEFAULT 0,                  -- [NUEVO] CU-CAJ-002
  `impuesto` DOUBLE NOT NULL DEFAULT 0,                   -- [NUEVO] impoconsumo incluido en el total (desglose de factura)
  `promocion` VARCHAR(100) DEFAULT NULL,                  -- [NUEVO] CU-CAJ-002: nombre de la promocion aplicada
  `referencia` VARCHAR(45) DEFAULT NULL,                  -- [NUEVO] Strategy: ultimos 4 digitos / referencia de transferencia
  `monto_recibido` DOUBLE DEFAULT NULL,                   -- [NUEVO] Strategy efectivo
  `cambio` DOUBLE DEFAULT NULL,                           -- [NUEVO] Strategy efectivo
  `numero_factura` VARCHAR(30) DEFAULT NULL,              -- [NUEVO] RF18/RF19/RF20
  `tipo_factura` VARCHAR(20) DEFAULT NULL,                -- [NUEVO] FISICA / ELECTRONICA (HU-CAJ-001 AC5)
  `fecha_factura` DATETIME DEFAULT NULL,                  -- [NUEVO] RF18
  `codigo_factura_electronica` VARCHAR(100) DEFAULT NULL, -- [NUEVO] RF19/RNF10: codigo devuelto por el proveedor de FE
  `correo_cliente` VARCHAR(100) DEFAULT NULL,             -- [NUEVO] RF19: destino de la factura electronica
  `Cajero_id_cajero` INT DEFAULT NULL,                    -- [NUEVO] RF13/RF18: que cajero cobro
  `Caja_id_caja` INT DEFAULT NULL,                        -- [NUEVO] RF11: a que caja (turno) pertenece el ingreso
  PRIMARY KEY (`id_pago`),
  UNIQUE KEY `uk_Pago_numero_factura` (`numero_factura`),
  INDEX `fk_Pago_Pedido1_idx` (`Pedido_id_pedido`),
  INDEX `fk_Pago_Cajero1_idx` (`Cajero_id_cajero`),
  INDEX `fk_Pago_Caja1_idx` (`Caja_id_caja`),
  INDEX `idx_Pago_fecha` (`fecha_pago`),                  -- [NUEVO] RNF01: reportes por periodo
  CONSTRAINT `fk_Pago_Pedido1` FOREIGN KEY (`Pedido_id_pedido`) REFERENCES `Pedido` (`id_pedido`),
  CONSTRAINT `fk_Pago_Cajero1` FOREIGN KEY (`Cajero_id_cajero`) REFERENCES `Cajero` (`id_cajero`),
  CONSTRAINT `fk_Pago_Caja1` FOREIGN KEY (`Caja_id_caja`) REFERENCES `Caja` (`id_caja`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `PagoProveedor` (
  `idPagoProveedor` INT NOT NULL AUTO_INCREMENT,
  `monto` DOUBLE DEFAULT NULL,
  `fecha` DATETIME DEFAULT NULL,
  `estado` VARCHAR(45) DEFAULT NULL,                      -- PENDIENTE, PAGADO
  `PedidoInsumo_id_pedido_insumo` INT NOT NULL,
  `fecha_pago` DATETIME DEFAULT NULL,                     -- [NUEVO] RF36: cuando se pago al proveedor
  PRIMARY KEY (`idPagoProveedor`),
  INDEX `fk_PagoProveedor_PedidoInsumo1_idx` (`PedidoInsumo_id_pedido_insumo`),
  CONSTRAINT `fk_PagoProveedor_PedidoInsumo1` FOREIGN KEY (`PedidoInsumo_id_pedido_insumo`) REFERENCES `PedidoInsumo` (`id_pedido_insumo`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `PedidoMesa` (
  `id_pedido_mesa` INT NOT NULL AUTO_INCREMENT,
  `Mesa_id_mesa` INT NOT NULL,
  `Pedido_id_pedido` INT NOT NULL,
  PRIMARY KEY (`id_pedido_mesa`),
  INDEX `fk_PedidoMesa_Mesa1_idx` (`Mesa_id_mesa`),
  INDEX `fk_PedidoMesa_Pedido1_idx` (`Pedido_id_pedido`),
  CONSTRAINT `fk_PedidoMesa_Mesa1` FOREIGN KEY (`Mesa_id_mesa`) REFERENCES `Mesa` (`id_mesa`),
  CONSTRAINT `fk_PedidoMesa_Pedido1` FOREIGN KEY (`Pedido_id_pedido`) REFERENCES `Pedido` (`id_pedido`)
) ENGINE=InnoDB;
