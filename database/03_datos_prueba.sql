-- ============================================================
-- 03_datos_prueba.sql  -  Datos de prueba para PoliRestaurante
-- Ejecutar DESPUES de 01_schema.sql (o de 02_migracion_atributos.sql).
-- Las fechas son relativas a NOW(), asi los reportes siempre tienen datos.
--
-- Todos los usuarios tienen la contrasena: 123456  (hash BCrypt, RNF05)
--   admin      -> ADMINISTRADOR
--   cajero1    -> CAJERO        cajero2 -> CAJERO
--   mesero1    -> MESERO        mesero2 -> MESERO   mesero3 -> MESERO (inactivo)
--   cocinero1  -> COCINERO      repartidor1 -> REPARTIDOR
--   prov1      -> PROVEEDOR (Distribuidora El Sol)
--   prov2      -> PROVEEDOR (Lacteos La Pradera)
-- Cambie las contrasenas antes de pasar a produccion.
-- ============================================================
USE DB_Polirestaurante;
SET FOREIGN_KEY_CHECKS = 0;

INSERT INTO Usuario (idUsuario, nombre, credenciales, estado) VALUES
  (1,'admin','$2a$10$ZseZxQBSFgJcliXmo8PLd.aRZKRRlu6BNGL5L9S3FugC/0pe6ZkJW','activo'),
  (2,'cajero1','$2a$10$ZseZxQBSFgJcliXmo8PLd.aRZKRRlu6BNGL5L9S3FugC/0pe6ZkJW','activo'),
  (3,'cajero2','$2a$10$ZseZxQBSFgJcliXmo8PLd.aRZKRRlu6BNGL5L9S3FugC/0pe6ZkJW','activo'),
  (4,'mesero1','$2a$10$ZseZxQBSFgJcliXmo8PLd.aRZKRRlu6BNGL5L9S3FugC/0pe6ZkJW','activo'),
  (5,'mesero2','$2a$10$ZseZxQBSFgJcliXmo8PLd.aRZKRRlu6BNGL5L9S3FugC/0pe6ZkJW','activo'),
  (6,'cocinero1','$2a$10$ZseZxQBSFgJcliXmo8PLd.aRZKRRlu6BNGL5L9S3FugC/0pe6ZkJW','activo'),
  (7,'repartidor1','$2a$10$ZseZxQBSFgJcliXmo8PLd.aRZKRRlu6BNGL5L9S3FugC/0pe6ZkJW','activo'),
  (8,'prov1','$2a$10$ZseZxQBSFgJcliXmo8PLd.aRZKRRlu6BNGL5L9S3FugC/0pe6ZkJW','activo'),
  (9,'prov2','$2a$10$ZseZxQBSFgJcliXmo8PLd.aRZKRRlu6BNGL5L9S3FugC/0pe6ZkJW','activo'),
  (10,'mesero3','$2a$10$ZseZxQBSFgJcliXmo8PLd.aRZKRRlu6BNGL5L9S3FugC/0pe6ZkJW','inactivo');
INSERT INTO Administrador (id_administrador, Usuario_idUsuario) VALUES (1,1);
INSERT INTO Cajero (id_cajero, Usuario_idUsuario) VALUES (1,2),(2,3);
INSERT INTO Mesero (id_mesero, zona_asignada, Usuario_idUsuario) VALUES (1,'Salon',4),(2,'Terraza',5),(3,'Salon',10);
INSERT INTO Cocinero (id_cocinero, estacion, Usuario_idUsuario) VALUES (1,'Parrilla',6);
INSERT INTO Repartidor (id_repartidor, placa_vehiculo, Usuario_idUsuario) VALUES (1,'ABC12D',7);
INSERT INTO Proveedor (id_proveedor, nombre, contacto, Usuario_idUsuario) VALUES
  (1,'Distribuidora El Sol','3005551234 - ventas@elsol.co',8),
  (2,'Lacteos La Pradera','3104447788 - pedidos@lapradera.co',9);

INSERT INTO Categoria (id_categoria, nombre) VALUES (1,'Entradas'),(2,'Hamburguesas'),(3,'Perros y sandwiches'),(4,'Bebidas'),(5,'Postres');
INSERT INTO Producto (idProducto, nombre, descripcion, precio, cantidad_disponible, Categoria_id_categoria, disponible, activo, stock_minimo) VALUES
  (1,'Papas a la francesa','Porcion mediana con salsa de la casa',7000,40,1,1,1,5),
  (2,'Aros de cebolla','8 aros apanados',8500,25,1,1,1,5),
  (3,'Alitas BBQ','6 alitas en salsa BBQ',16000,3,1,1,1,5),
  (4,'Hamburguesa clasica','Carne 150 g, queso, lechuga y tomate',18000,30,2,1,1,5),
  (5,'Hamburguesa doble','Doble carne, doble queso y tocineta',25000,20,2,1,1,5),
  (6,'Hamburguesa de pollo','Pechuga apanada y salsa miel mostaza',19000,15,2,1,1,5),
  (7,'Perro sencillo','Salchicha americana, papita y salsas',12000,30,3,1,1,5),
  (8,'Perro especial','Con queso gratinado y tocineta',16000,0,3,0,1,5),
  (9,'Sandwich cubano','Cerdo, jamon, queso y pepinillos',17000,12,3,1,1,5),
  (10,'Gaseosa 400 ml','Linea Postobon / Coca-Cola',4500,60,4,1,1,5),
  (11,'Limonada natural','Vaso 16 oz',6000,40,4,1,1,5),
  (12,'Malteada de vainilla','Vaso 16 oz',11000,4,4,1,1,5),
  (13,'Brownie con helado','Brownie tibio y bola de vainilla',9500,10,5,1,1,5),
  (14,'Postre de natas','Porcion individual',8000,2,5,1,1,5);

INSERT INTO Mesa (id_mesa, numero, estado, capacidad, activa) VALUES
  (1,1,'LIBRE',4,1),
  (2,2,'LIBRE',4,1),
  (3,3,'OCUPADA',6,1),
  (4,4,'LIBRE',2,1),
  (5,5,'OCUPADA',4,1),
  (6,6,'LIBRE',6,1),
  (7,7,'OCUPADA',4,1),
  (8,8,'LIBRE',2,1),
  (9,9,'LIBRE',6,1),
  (10,10,'LIBRE',4,1),
  (11,11,'LIBRE',4,1),
  (12,12,'LIBRE',2,1);

INSERT INTO Insumo (id_insumo, nombre, unidad_medida, cantidad_disponible, stock_minimo) VALUES
  (1,'Carne de res molida','kg',18,5),
  (2,'Pan de hamburguesa','unidad',120,40),
  (3,'Queso mozzarella','kg',3,4),
  (4,'Papa criolla congelada','kg',25,10),
  (5,'Salchicha americana','unidad',8,20),
  (6,'Leche entera','litro',12,10),
  (7,'Helado de vainilla','litro',2,3),
  (8,'Pechuga de pollo','kg',9,5),
  (9,'Tocineta','kg',1,2),
  (10,'Limon Tahiti','kg',6,3);
INSERT INTO InsumoProveedor (id_insumo_proveedor, precio, Proveedor_id_proveedor1, Insumo_id_insumo1, disponible) VALUES
  (1,22000,1,1,1),
  (2,900,1,2,1),
  (3,4500,1,5,1),
  (4,26000,1,8,1),
  (5,30000,1,9,1),
  (6,8000,1,4,1),
  (7,24000,2,3,1),
  (8,3600,2,6,1),
  (9,15000,2,7,1),
  (10,25000,1,3,1);

-- Turnos de caja historicos (cerrados) del cajero1
INSERT INTO Caja (id_caja, fecha_apertura, fecha_cierre, monto_inicial, monto_final, Cajero_id_cajero, estado, total_ventas) VALUES
  (1, NOW() - INTERVAL 29 DAY, NOW() - INTERVAL 15 DAY, 200000, NULL, 1, 'CERRADA', NULL),
  (2, NOW() - INTERVAL 14 DAY, NOW() - INTERVAL 1 DAY, 200000, NULL, 1, 'CERRADA', NULL);

-- Pedidos: 14 historicos pagados (reportes) + 4 cuentas abiertas para el cajero
INSERT INTO Pedido (id_pedido, estado, fecha_creacion, fecha_ultima_actualizacion, tipo_pedido, Mesero_id_mesero, fecha_en_preparacion, fecha_listo, fecha_entregado, promocion_aplicada) VALUES
  (1,'PAGADO',NOW() - INTERVAL 28 DAY - INTERVAL 120 MINUTE,NOW() - INTERVAL 28 DAY - INTERVAL 88 MINUTE,'MESA',1,NOW() - INTERVAL 28 DAY - INTERVAL 114 MINUTE,NOW() - INTERVAL 28 DAY - INTERVAL 106 MINUTE,NOW() - INTERVAL 28 DAY - INTERVAL 103 MINUTE,NULL),
  (2,'PAGADO',NOW() - INTERVAL 26 DAY - INTERVAL 123 MINUTE,NOW() - INTERVAL 26 DAY - INTERVAL 95 MINUTE,'MESA',2,NOW() - INTERVAL 26 DAY - INTERVAL 121 MINUTE,NOW() - INTERVAL 26 DAY - INTERVAL 113 MINUTE,NOW() - INTERVAL 26 DAY - INTERVAL 110 MINUTE,NULL),
  (3,'PAGADO',NOW() - INTERVAL 24 DAY - INTERVAL 126 MINUTE,NOW() - INTERVAL 24 DAY - INTERVAL 96 MINUTE,'MESA',1,NOW() - INTERVAL 24 DAY - INTERVAL 122 MINUTE,NOW() - INTERVAL 24 DAY - INTERVAL 114 MINUTE,NOW() - INTERVAL 24 DAY - INTERVAL 111 MINUTE,NULL),
  (4,'PAGADO',NOW() - INTERVAL 22 DAY - INTERVAL 129 MINUTE,NOW() - INTERVAL 22 DAY - INTERVAL 93 MINUTE,'MESA',2,NOW() - INTERVAL 22 DAY - INTERVAL 124 MINUTE,NOW() - INTERVAL 22 DAY - INTERVAL 110 MINUTE,NOW() - INTERVAL 22 DAY - INTERVAL 108 MINUTE,NULL),
  (5,'PAGADO',NOW() - INTERVAL 20 DAY - INTERVAL 132 MINUTE,NOW() - INTERVAL 20 DAY - INTERVAL 93 MINUTE,'MESA',1,NOW() - INTERVAL 20 DAY - INTERVAL 128 MINUTE,NOW() - INTERVAL 20 DAY - INTERVAL 118 MINUTE,NOW() - INTERVAL 20 DAY - INTERVAL 108 MINUTE,NULL),
  (6,'PAGADO',NOW() - INTERVAL 18 DAY - INTERVAL 135 MINUTE,NOW() - INTERVAL 18 DAY - INTERVAL 97 MINUTE,'MESA',2,NOW() - INTERVAL 18 DAY - INTERVAL 134 MINUTE,NOW() - INTERVAL 18 DAY - INTERVAL 117 MINUTE,NOW() - INTERVAL 18 DAY - INTERVAL 112 MINUTE,NULL),
  (7,'PAGADO',NOW() - INTERVAL 16 DAY - INTERVAL 138 MINUTE,NOW() - INTERVAL 16 DAY - INTERVAL 100 MINUTE,'MESA',1,NOW() - INTERVAL 16 DAY - INTERVAL 137 MINUTE,NOW() - INTERVAL 16 DAY - INTERVAL 120 MINUTE,NOW() - INTERVAL 16 DAY - INTERVAL 115 MINUTE,NULL),
  (8,'PAGADO',NOW() - INTERVAL 14 DAY - INTERVAL 141 MINUTE,NOW() - INTERVAL 14 DAY - INTERVAL 96 MINUTE,'MESA',2,NOW() - INTERVAL 14 DAY - INTERVAL 137 MINUTE,NOW() - INTERVAL 14 DAY - INTERVAL 120 MINUTE,NOW() - INTERVAL 14 DAY - INTERVAL 111 MINUTE,NULL),
  (9,'PAGADO',NOW() - INTERVAL 12 DAY - INTERVAL 144 MINUTE,NOW() - INTERVAL 12 DAY - INTERVAL 112 MINUTE,'MESA',1,NOW() - INTERVAL 12 DAY - INTERVAL 142 MINUTE,NOW() - INTERVAL 12 DAY - INTERVAL 133 MINUTE,NOW() - INTERVAL 12 DAY - INTERVAL 127 MINUTE,NULL),
  (10,'PAGADO',NOW() - INTERVAL 10 DAY - INTERVAL 147 MINUTE,NOW() - INTERVAL 10 DAY - INTERVAL 115 MINUTE,'MESA',2,NOW() - INTERVAL 10 DAY - INTERVAL 142 MINUTE,NOW() - INTERVAL 10 DAY - INTERVAL 133 MINUTE,NOW() - INTERVAL 10 DAY - INTERVAL 130 MINUTE,NULL),
  (11,'PAGADO',NOW() - INTERVAL 8 DAY - INTERVAL 150 MINUTE,NOW() - INTERVAL 8 DAY - INTERVAL 115 MINUTE,'MESA',1,NOW() - INTERVAL 8 DAY - INTERVAL 146 MINUTE,NOW() - INTERVAL 8 DAY - INTERVAL 132 MINUTE,NOW() - INTERVAL 8 DAY - INTERVAL 130 MINUTE,NULL),
  (12,'PAGADO',NOW() - INTERVAL 6 DAY - INTERVAL 153 MINUTE,NOW() - INTERVAL 6 DAY - INTERVAL 109 MINUTE,'MESA',2,NOW() - INTERVAL 6 DAY - INTERVAL 150 MINUTE,NOW() - INTERVAL 6 DAY - INTERVAL 131 MINUTE,NOW() - INTERVAL 6 DAY - INTERVAL 124 MINUTE,NULL),
  (13,'PAGADO',NOW() - INTERVAL 4 DAY - INTERVAL 156 MINUTE,NOW() - INTERVAL 4 DAY - INTERVAL 119 MINUTE,'MESA',1,NOW() - INTERVAL 4 DAY - INTERVAL 155 MINUTE,NOW() - INTERVAL 4 DAY - INTERVAL 143 MINUTE,NOW() - INTERVAL 4 DAY - INTERVAL 134 MINUTE,NULL),
  (14,'PAGADO',NOW() - INTERVAL 2 DAY - INTERVAL 159 MINUTE,NOW() - INTERVAL 2 DAY - INTERVAL 114 MINUTE,'MESA',2,NOW() - INTERVAL 2 DAY - INTERVAL 156 MINUTE,NOW() - INTERVAL 2 DAY - INTERVAL 138 MINUTE,NOW() - INTERVAL 2 DAY - INTERVAL 129 MINUTE,NULL),
  (15,'ENTREGADO',NOW() - INTERVAL 40 MINUTE,NOW() - INTERVAL 5 MINUTE,'MESA',1,NOW() - INTERVAL 30 MINUTE,NOW() - INTERVAL 15 MINUTE,NOW() - INTERVAL 5 MINUTE,NULL),
  (16,'LISTO',NOW() - INTERVAL 40 MINUTE,NOW() - INTERVAL 5 MINUTE,'MESA',2,NOW() - INTERVAL 30 MINUTE,NOW() - INTERVAL 15 MINUTE,NULL,NULL),
  (17,'EN_PREPARACION',NOW() - INTERVAL 40 MINUTE,NOW() - INTERVAL 5 MINUTE,'MESA',1,NOW() - INTERVAL 30 MINUTE,NULL,NULL,NULL),
  (18,'PENDIENTE',NOW() - INTERVAL 40 MINUTE,NOW() - INTERVAL 5 MINUTE,'LLEVAR',2,NULL,NULL,NULL,NULL);
INSERT INTO PedidoMesa (Pedido_id_pedido, Mesa_id_mesa) VALUES (1,1),(2,6),(3,11),(4,4),(5,9),(6,2),(7,7),(8,12),(9,5),(10,10),(11,3),(12,8),(13,1),(14,6),(15,3),(16,5),(17,7);
INSERT INTO DetallePedido (id_detalle_pedido, cantidad, notas, Producto_idProducto, Pedido_id_pedido, precio_unitario, estado, fecha_anulacion, motivo_anulacion, Usuario_anulacion) VALUES
  (1,3,NULL,6,1,19000,'ENTREGADO',NULL,NULL,NULL),
  (2,1,NULL,3,1,16000,'ENTREGADO',NULL,NULL,NULL),
  (3,2,NULL,7,1,12000,'ENTREGADO',NULL,NULL,NULL),
  (4,2,NULL,10,2,4500,'ENTREGADO',NULL,NULL,NULL),
  (5,2,NULL,1,2,7000,'ENTREGADO',NULL,NULL,NULL),
  (6,1,NULL,9,2,17000,'ENTREGADO',NULL,NULL,NULL),
  (7,1,NULL,4,3,18000,'ENTREGADO',NULL,NULL,NULL),
  (8,3,NULL,2,3,8500,'ENTREGADO',NULL,NULL,NULL),
  (9,3,NULL,9,3,17000,'ENTREGADO',NULL,NULL,NULL),
  (10,1,NULL,10,4,4500,'ENTREGADO',NULL,NULL,NULL),
  (11,1,NULL,1,4,7000,'ENTREGADO',NULL,NULL,NULL),
  (12,3,NULL,14,4,8000,'ENTREGADO',NULL,NULL,NULL),
  (13,1,NULL,14,5,8000,'ENTREGADO',NULL,NULL,NULL),
  (14,3,NULL,3,5,16000,'ENTREGADO',NULL,NULL,NULL),
  (15,2,NULL,5,5,25000,'ENTREGADO',NULL,NULL,NULL),
  (16,2,NULL,9,6,17000,'ENTREGADO',NULL,NULL,NULL),
  (17,1,NULL,11,6,6000,'ENTREGADO',NULL,NULL,NULL),
  (18,3,NULL,3,6,16000,'ENTREGADO',NULL,NULL,NULL),
  (19,2,NULL,12,7,11000,'ENTREGADO',NULL,NULL,NULL),
  (20,3,NULL,2,7,8500,'ENTREGADO',NULL,NULL,NULL),
  (21,3,NULL,10,7,4500,'ENTREGADO',NULL,NULL,NULL),
  (22,2,NULL,7,8,12000,'ENTREGADO',NULL,NULL,NULL),
  (23,2,NULL,13,8,9500,'ENTREGADO',NULL,NULL,NULL),
  (24,1,NULL,6,8,19000,'ENTREGADO',NULL,NULL,NULL),
  (25,3,NULL,13,9,9500,'ENTREGADO',NULL,NULL,NULL),
  (26,2,NULL,3,9,16000,'ENTREGADO',NULL,NULL,NULL),
  (27,2,NULL,12,9,11000,'ENTREGADO',NULL,NULL,NULL),
  (28,3,NULL,12,10,11000,'ENTREGADO',NULL,NULL,NULL),
  (29,2,NULL,8,10,16000,'ENTREGADO',NULL,NULL,NULL),
  (30,1,NULL,5,10,25000,'ENTREGADO',NULL,NULL,NULL),
  (31,3,NULL,13,11,9500,'ENTREGADO',NULL,NULL,NULL),
  (32,1,NULL,6,11,19000,'ENTREGADO',NULL,NULL,NULL),
  (33,3,NULL,3,11,16000,'ENTREGADO',NULL,NULL,NULL),
  (34,3,NULL,10,12,4500,'ENTREGADO',NULL,NULL,NULL),
  (35,2,NULL,13,12,9500,'ENTREGADO',NULL,NULL,NULL),
  (36,3,NULL,6,12,19000,'ENTREGADO',NULL,NULL,NULL),
  (37,3,NULL,13,13,9500,'ENTREGADO',NULL,NULL,NULL),
  (38,3,NULL,8,13,16000,'ENTREGADO',NULL,NULL,NULL),
  (39,1,NULL,2,13,8500,'ENTREGADO',NULL,NULL,NULL),
  (40,2,NULL,1,14,7000,'ENTREGADO',NULL,NULL,NULL),
  (41,3,NULL,12,14,11000,'ENTREGADO',NULL,NULL,NULL),
  (42,2,NULL,13,14,9500,'ENTREGADO',NULL,NULL,NULL),
  (43,2,'Sin cebolla en una hamburguesa',4,15,NULL,'ENTREGADO',NULL,NULL,NULL),
  (44,2,NULL,1,15,NULL,'ENTREGADO',NULL,NULL,NULL),
  (45,2,NULL,10,15,NULL,'ENTREGADO',NULL,NULL,NULL),
  (46,1,NULL,5,16,NULL,'PENDIENTE',NULL,NULL,NULL),
  (47,1,'Salsa aparte',6,16,NULL,'PENDIENTE',NULL,NULL,NULL),
  (48,2,NULL,11,16,NULL,'PENDIENTE',NULL,NULL,NULL),
  (49,3,NULL,7,17,NULL,'PENDIENTE',NULL,NULL,NULL),
  (50,3,NULL,10,17,NULL,'PENDIENTE',NULL,NULL,NULL),
  (51,1,'Para llevar, bien empacado',9,18,NULL,'PENDIENTE',NULL,NULL,NULL),
  (52,1,NULL,12,18,NULL,'PENDIENTE',NULL,NULL,NULL),
  (53,1,NULL,13,18,NULL,'PENDIENTE',NULL,NULL,NULL);
INSERT INTO Pago (id_pago, monto, metodo_pago, fecha_pago, Pedido_id_pedido, estado, subtotal, descuento, impuesto, promocion, referencia, monto_recibido, cambio, numero_factura, tipo_factura, fecha_factura, codigo_factura_electronica, correo_cliente, Cajero_id_cajero, Caja_id_caja) VALUES
  (1,97000,'EFECTIVO',NOW() - INTERVAL 28 DAY - INTERVAL 88 MINUTE,1,'APROBADO',97000,0,7185.19,NULL,NULL,100000,3000,'F-000001','FISICA',NOW() - INTERVAL 28 DAY - INTERVAL 88 MINUTE,NULL,NULL,1,1),
  (2,40000,'TARJETA',NOW() - INTERVAL 26 DAY - INTERVAL 95 MINUTE,2,'APROBADO',40000,0,2962.96,NULL,'4242',NULL,NULL,'F-000002','ELECTRONICA',NOW() - INTERVAL 26 DAY - INTERVAL 95 MINUTE,'CUFE-SIM-100001','cliente@correo.com',1,1),
  (3,94500,'TRANSFERENCIA',NOW() - INTERVAL 24 DAY - INTERVAL 96 MINUTE,3,'APROBADO',94500,0,7000.0,NULL,'TRX880002',NULL,NULL,'F-000003','FISICA',NOW() - INTERVAL 24 DAY - INTERVAL 96 MINUTE,NULL,NULL,1,1),
  (4,31950,'EFECTIVO',NOW() - INTERVAL 22 DAY - INTERVAL 93 MINUTE,4,'APROBADO',35500,3550,2366.67,'Martes de amigos 10%',NULL,40000,8050,'F-000004','ELECTRONICA',NOW() - INTERVAL 22 DAY - INTERVAL 93 MINUTE,'CUFE-SIM-100003','cliente@correo.com',1,1),
  (5,106000,'TARJETA',NOW() - INTERVAL 20 DAY - INTERVAL 93 MINUTE,5,'APROBADO',106000,0,7851.85,NULL,'4242',NULL,NULL,'F-000005','FISICA',NOW() - INTERVAL 20 DAY - INTERVAL 93 MINUTE,NULL,NULL,1,1),
  (6,88000,'TRANSFERENCIA',NOW() - INTERVAL 18 DAY - INTERVAL 97 MINUTE,6,'APROBADO',88000,0,6518.52,NULL,'TRX880005',NULL,NULL,'F-000006','ELECTRONICA',NOW() - INTERVAL 18 DAY - INTERVAL 97 MINUTE,'CUFE-SIM-100005','cliente@correo.com',1,1),
  (7,61000,'EFECTIVO',NOW() - INTERVAL 16 DAY - INTERVAL 100 MINUTE,7,'APROBADO',61000,0,4518.52,NULL,NULL,70000,9000,'F-000007','FISICA',NOW() - INTERVAL 16 DAY - INTERVAL 100 MINUTE,NULL,NULL,1,1),
  (8,62000,'TARJETA',NOW() - INTERVAL 14 DAY - INTERVAL 96 MINUTE,8,'APROBADO',62000,0,4592.59,NULL,'4242',NULL,NULL,'F-000008','ELECTRONICA',NOW() - INTERVAL 14 DAY - INTERVAL 96 MINUTE,'CUFE-SIM-100007','cliente@correo.com',1,2),
  (9,82500,'TRANSFERENCIA',NOW() - INTERVAL 12 DAY - INTERVAL 112 MINUTE,9,'APROBADO',82500,0,6111.11,NULL,'TRX880008',NULL,NULL,'F-000009','FISICA',NOW() - INTERVAL 12 DAY - INTERVAL 112 MINUTE,NULL,NULL,1,2),
  (10,81000,'EFECTIVO',NOW() - INTERVAL 10 DAY - INTERVAL 115 MINUTE,10,'APROBADO',90000,9000,6000.0,'Martes de amigos 10%',NULL,90000,9000,'F-000010','ELECTRONICA',NOW() - INTERVAL 10 DAY - INTERVAL 115 MINUTE,'CUFE-SIM-100009','cliente@correo.com',1,2),
  (11,95500,'TARJETA',NOW() - INTERVAL 8 DAY - INTERVAL 115 MINUTE,11,'APROBADO',95500,0,7074.07,NULL,'4242',NULL,NULL,'F-000011','FISICA',NOW() - INTERVAL 8 DAY - INTERVAL 115 MINUTE,NULL,NULL,1,2),
  (12,89500,'TRANSFERENCIA',NOW() - INTERVAL 6 DAY - INTERVAL 109 MINUTE,12,'APROBADO',89500,0,6629.63,NULL,'TRX880011',NULL,NULL,'F-000012','ELECTRONICA',NOW() - INTERVAL 6 DAY - INTERVAL 109 MINUTE,'CUFE-SIM-100011','cliente@correo.com',1,2),
  (13,85000,'EFECTIVO',NOW() - INTERVAL 4 DAY - INTERVAL 119 MINUTE,13,'APROBADO',85000,0,6296.3,NULL,NULL,90000,5000,'F-000013','FISICA',NOW() - INTERVAL 4 DAY - INTERVAL 119 MINUTE,NULL,NULL,1,2),
  (14,66000,'TARJETA',NOW() - INTERVAL 2 DAY - INTERVAL 114 MINUTE,14,'APROBADO',66000,0,4888.89,NULL,'4242',NULL,NULL,'F-000014','ELECTRONICA',NOW() - INTERVAL 2 DAY - INTERVAL 114 MINUTE,'CUFE-SIM-100013','cliente@correo.com',1,2);

-- Totales de los turnos cerrados (calculados desde los pagos)
UPDATE Caja c SET c.total_ventas = (SELECT IFNULL(SUM(p.monto),0) FROM Pago p WHERE p.Caja_id_caja = c.id_caja) WHERE c.id_caja IN (1,2);
UPDATE Caja c SET c.monto_final = c.monto_inicial + (SELECT IFNULL(SUM(p.monto),0) FROM Pago p WHERE p.Caja_id_caja = c.id_caja AND p.metodo_pago = 'EFECTIVO') WHERE c.id_caja IN (1,2);

-- Abastecimiento: solicitudes a proveedores en distintos estados
INSERT INTO PedidoInsumo (id_pedido_insumo, fecha, estado, Administrador_id_administrador, Proveedor_id_proveedor, fecha_recepcion) VALUES
  (1, NOW() - INTERVAL 20 DAY, 'RECIBIDA', 1, 1, NOW() - INTERVAL 18 DAY),
  (2, NOW() - INTERVAL 6 DAY, 'RECIBIDA', 1, 2, NOW() - INTERVAL 5 DAY),
  (3, NOW() - INTERVAL 2 DAY, 'DESPACHADA', 1, 1, NULL),
  (4, NOW() - INTERVAL 3 HOUR, 'PENDIENTE', 1, 2, NULL);
INSERT INTO DetallePedidoInsumo (id_detalle_pedido_insumo, cantidad, Insumo_id_insumo1, PedidoInsumo_id_pedido_insumo, precio_unitario) VALUES
  (1,10,1,1,22000),
  (2,100,2,1,900),
  (3,20,4,1,8000),
  (4,10,6,2,3600),
  (5,4,7,2,15000),
  (6,8,1,3,22000),
  (7,3,9,3,30000),
  (8,5,3,4,24000),
  (9,3,7,4,15000);
INSERT INTO PagoProveedor (idPagoProveedor, monto, fecha, estado, PedidoInsumo_id_pedido_insumo, fecha_pago) VALUES
  (1, 470000, NOW() - INTERVAL 18 DAY, 'PAGADO', 1, NOW() - INTERVAL 10 DAY),
  (2, 96000, NOW() - INTERVAL 5 DAY, 'PENDIENTE', 2, NULL);

SET FOREIGN_KEY_CHECKS = 1;
