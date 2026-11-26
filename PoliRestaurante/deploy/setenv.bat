@echo off
rem =====================================================================
rem  Copie este archivo a  C:\Tomcat10.1\bin\setenv.bat
rem  (si instalo Tomcat como servicio de Windows, ponga estos mismos
rem   valores en "Configure Tomcat" > pestana Java, ver docs/DESPLIEGUE.md)
rem
rem  RNF15: las credenciales y parametros viven aqui, NO en el codigo.
rem  Mover el sistema a otro servidor = cambiar solo este archivo.
rem =====================================================================

rem --- Java 21 ---
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21"

rem --- Base de datos: MySQL 8.0 en el mismo servidor (ADR-005) ---
set "DB_URL=jdbc:mysql://localhost:3306/DB_Polirestaurante?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Bogota&characterEncoding=UTF-8"
set "DB_USER=polirestaurante_app"
set "DB_PASSWORD=Cambie_Esta_Clave_2026"

rem --- Firma de los tokens JWT (ADR-003): minimo 32 caracteres, unica por instalacion ---
set "JWT_SECRET=CAMBIE-ESTA-CLAVE-por-una-larga-y-aleatoria-0123456789"

rem --- Memoria y zona horaria (RNF19) ---
set "CATALINA_OPTS=%CATALINA_OPTS% -Xms256m -Xmx768m -Duser.timezone=America/Bogota -Dfile.encoding=UTF-8"
