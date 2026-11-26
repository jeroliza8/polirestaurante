-- ============================================================
-- 02_migracion_atributos.sql
-- SOLO para una base DB_Polirestaurante que ya fue creada con la
-- version ANTERIOR de 01_schema.sql. Ejecutelo UNA sola vez
-- (MySQL 8.0 no admite ADD COLUMN IF NOT EXISTS).
--
-- No crea tablas: unicamente agrega atributos, indices y llaves
-- foraneas a tablas existentes. Instalacion limpia -> no lo use,
-- el 01_schema.sql nuevo ya los trae.
-- ============================================================
USE DB_Polirestaurante;

ALTER TABLE `Insumo`
  ADD COLUMN `unidad_medida` VARCHAR(20) NOT NULL DEFAULT 'unidad',
  ADD COLUMN `stock_minimo`  INT NOT NULL DEFAULT 5;

ALTER TABLE `Mesa`
  ADD COLUMN `capacidad` INT NOT NULL DEFAULT 4,
  ADD COLUMN `activa`    TINYINT(1) NOT NULL DEFAULT 1;

ALTER TABLE `Usuario`
  ADD UNIQUE KEY `uk_Usuario_nombre` (`nombre`);

ALTER TABLE `Proveedor`
  ADD COLUMN `Usuario_idUsuario` INT DEFAULT NULL,
  ADD INDEX `fk_Proveedor_Usuario1_idx` (`Usuario_idUsuario`),
  ADD CONSTRAINT `fk_Proveedor_Usuario1` FOREIGN KEY (`Usuario_idUsuario`) REFERENCES `Usuario` (`idUsuario`);

ALTER TABLE `InsumoProveedor`
  ADD COLUMN `disponible` TINYINT(1) NOT NULL DEFAULT 1;

ALTER TABLE `Producto`
  ADD COLUMN `disponible`   TINYINT(1) NOT NULL DEFAULT 1,
  ADD COLUMN `activo`       TINYINT(1) NOT NULL DEFAULT 1,
  ADD COLUMN `stock_minimo` INT NOT NULL DEFAULT 5;

ALTER TABLE `Caja`
  ADD COLUMN `estado`       VARCHAR(20) NOT NULL DEFAULT 'CERRADA',
  ADD COLUMN `total_ventas` DOUBLE DEFAULT NULL;

ALTER TABLE `Pedido`
  ADD COLUMN `fecha_en_preparacion` DATETIME DEFAULT NULL,
  ADD COLUMN `fecha_listo`          DATETIME DEFAULT NULL,
  ADD COLUMN `fecha_entregado`      DATETIME DEFAULT NULL,
  ADD COLUMN `promocion_aplicada`   VARCHAR(50) DEFAULT NULL,
  ADD INDEX `idx_Pedido_estado` (`estado`);

ALTER TABLE `PedidoInsumo`
  ADD COLUMN `Proveedor_id_proveedor` INT DEFAULT NULL,
  ADD COLUMN `fecha_recepcion`        DATETIME DEFAULT NULL,
  ADD INDEX `fk_PedidoInsumo_Proveedor1_idx` (`Proveedor_id_proveedor`),
  ADD CONSTRAINT `fk_PedidoInsumo_Proveedor1` FOREIGN KEY (`Proveedor_id_proveedor`) REFERENCES `Proveedor` (`id_proveedor`);

ALTER TABLE `DetallePedido`
  ADD COLUMN `precio_unitario`   DOUBLE DEFAULT NULL,
  ADD COLUMN `estado`            VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  ADD COLUMN `fecha_anulacion`   DATETIME DEFAULT NULL,
  ADD COLUMN `motivo_anulacion`  VARCHAR(255) DEFAULT NULL,
  ADD COLUMN `Usuario_anulacion` INT DEFAULT NULL,
  ADD INDEX `fk_DetallePedido_Usuario1_idx` (`Usuario_anulacion`),
  ADD CONSTRAINT `fk_DetallePedido_Usuario1` FOREIGN KEY (`Usuario_anulacion`) REFERENCES `Usuario` (`idUsuario`);

ALTER TABLE `DetallePedidoInsumo`
  ADD COLUMN `precio_unitario` DOUBLE DEFAULT NULL;

ALTER TABLE `Pago`
  ADD COLUMN `estado`                     VARCHAR(20) NOT NULL DEFAULT 'APROBADO',
  ADD COLUMN `subtotal`                   DOUBLE DEFAULT NULL,
  ADD COLUMN `descuento`                  DOUBLE NOT NULL DEFAULT 0,
  ADD COLUMN `impuesto`                   DOUBLE NOT NULL DEFAULT 0,
  ADD COLUMN `promocion`                  VARCHAR(100) DEFAULT NULL,
  ADD COLUMN `referencia`                 VARCHAR(45) DEFAULT NULL,
  ADD COLUMN `monto_recibido`             DOUBLE DEFAULT NULL,
  ADD COLUMN `cambio`                     DOUBLE DEFAULT NULL,
  ADD COLUMN `numero_factura`             VARCHAR(30) DEFAULT NULL,
  ADD COLUMN `tipo_factura`               VARCHAR(20) DEFAULT NULL,
  ADD COLUMN `fecha_factura`              DATETIME DEFAULT NULL,
  ADD COLUMN `codigo_factura_electronica` VARCHAR(100) DEFAULT NULL,
  ADD COLUMN `correo_cliente`             VARCHAR(100) DEFAULT NULL,
  ADD COLUMN `Cajero_id_cajero`           INT DEFAULT NULL,
  ADD COLUMN `Caja_id_caja`               INT DEFAULT NULL,
  ADD UNIQUE KEY `uk_Pago_numero_factura` (`numero_factura`),
  ADD INDEX `fk_Pago_Cajero1_idx` (`Cajero_id_cajero`),
  ADD INDEX `fk_Pago_Caja1_idx` (`Caja_id_caja`),
  ADD INDEX `idx_Pago_fecha` (`fecha_pago`),
  ADD CONSTRAINT `fk_Pago_Cajero1` FOREIGN KEY (`Cajero_id_cajero`) REFERENCES `Cajero` (`id_cajero`),
  ADD CONSTRAINT `fk_Pago_Caja1`   FOREIGN KEY (`Caja_id_caja`)     REFERENCES `Caja` (`id_caja`);

ALTER TABLE `PagoProveedor`
  ADD COLUMN `fecha_pago` DATETIME DEFAULT NULL;

-- Normaliza estados escritos a mano en la version anterior (si los hay)
SET SQL_SAFE_UPDATES = 0;
UPDATE `Pedido` SET `estado` = UPPER(REPLACE(TRIM(`estado`), ' ', '_')) WHERE `estado` IS NOT NULL;
UPDATE `Pedido` SET `estado` = 'EN_PREPARACION' WHERE `estado` IN ('EN_PREPARACIÓN');
UPDATE `Mesa`   SET `estado` = UPPER(TRIM(`estado`)) WHERE `estado` IS NOT NULL;
SET SQL_SAFE_UPDATES = 1;
