-- ============================================================
-- 00_usuario_aplicacion.sql
-- Crea el usuario de MySQL con el que se conecta la aplicacion.
-- Ejecutar como root en el MySQL 8.0 del servidor. No crea tablas.
--
-- Como MySQL y Tomcat estan en el MISMO servidor (ADR-005), el usuario
-- solo puede conectarse desde localhost. Cambie la contrasena y use la
-- misma en la variable DB_PASSWORD de Tomcat (deploy/setenv.bat).
-- ============================================================
CREATE USER IF NOT EXISTS 'polirestaurante_app'@'localhost' IDENTIFIED BY 'Cambie_Esta_Clave_2026';
GRANT SELECT, INSERT, UPDATE, DELETE ON DB_Polirestaurante.* TO 'polirestaurante_app'@'localhost';
FLUSH PRIVILEGES;
