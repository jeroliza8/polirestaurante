-- PoliRestaurante | Complemento de Cocina (RF39 y CU-COC-02).
-- Ejecutar UNA VEZ por el administrador de MySQL DESPUÉS de 01_schema.sql
-- (o de 02_migracion_atributos.sql si la BD se creó con una versión anterior).
-- NO crea tablas ni elimina o renombra registros. Requiere aprobación del equipo.
USE DB_Polirestaurante;
ALTER TABLE `Pedido`
    ADD COLUMN `Usuario_inicio_preparacion` INT DEFAULT NULL,
    ADD COLUMN `Usuario_listo` INT DEFAULT NULL,
    ADD CONSTRAINT `fk_Pedido_CocinaInicio_Usuario` FOREIGN KEY (`Usuario_inicio_preparacion`) REFERENCES `Usuario` (`idUsuario`),
    ADD CONSTRAINT `fk_Pedido_CocinaListo_Usuario` FOREIGN KEY (`Usuario_listo`) REFERENCES `Usuario` (`idUsuario`);
ALTER TABLE `Producto`
    ADD COLUMN `fecha_disponibilidad` DATETIME DEFAULT NULL,
    ADD COLUMN `Usuario_disponibilidad` INT DEFAULT NULL,
    ADD CONSTRAINT `fk_Producto_Disponibilidad_Usuario` FOREIGN KEY (`Usuario_disponibilidad`) REFERENCES `Usuario` (`idUsuario`);
