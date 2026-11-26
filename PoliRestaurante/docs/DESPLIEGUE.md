# Guía de despliegue — Windows Server 2022

Esta guía sigue el **diagrama de despliegue** del documento y **ADR-005**: un único servidor
Windows Server con dos nodos internos.

```
┌──────────────── Servidor PoliRestaurante (IP local, Windows Server 2022) ────────────────┐
│                                                                                          │
│  ┌──── Apache Tomcat 10.1 (Spring Boot, Java 21) ────┐   Use   ┌── MySQL 8.0 - DBServer ──┐ │
│  │  polirestaurante.war                              │ ──────► │  DB_Polirestaurante      │ │
│  │   AdministradorController → ProductoService,      │ JDBC    │  (localhost:3306)        │ │
│  │      UsuarioService, ProveedorService             │         └──────────────────────────┘ │
│  │   CajeroController → CajeroService                │                                     │
│  │   ProveedorController → ProveedorService          │                                     │
│  │   + interfaz web (HTML/CSS/JS) en el mismo WAR    │                                     │
│  └───────────────────────────────────────────────────┘                                     │
└──────────────────────────────────────────────────────────────────────────────────────────┘
        ▲  navegador (PC / tablet) en la red del restaurante — no se instala nada (RNF09)
```

La interfaz web va **dentro del WAR**: el navegador la descarga del mismo Tomcat, así no hay un
segundo servidor ni problemas de CORS.

---

## 1. Software del servidor

| Componente | Versión | Notas |
|---|---|---|
| JDK | 21 (Eclipse Temurin u Oracle) | `java -version` debe decir 21 |
| Apache Tomcat | 10.1.x | Jakarta EE 10, requerido por Spring Boot 3 |
| MySQL Server | 8.0.x | Servicio `MySQL80`, puerto 3306, solo localhost |

En la máquina de desarrollo (con internet) se necesita además **Maven 3.9+** para compilar.

## 2. Base de datos (nodo MySQL 8.0 - DBServer)

Desde *MySQL Workbench* o la consola `mysql -u root -p`, en este orden:

```sql
SOURCE C:/PoliRestaurante/database/01_schema.sql;          -- 23 tablas (solo atributos nuevos)
SOURCE C:/PoliRestaurante/database/00_usuario_aplicacion.sql; -- usuario de la aplicación
SOURCE C:/PoliRestaurante/database/03_datos_prueba.sql;    -- opcional: datos de prueba
```

¿Ya tenía la base creada con el `01_schema.sql` **anterior**? Entonces, en vez del 01, ejecute
**una sola vez** `02_migracion_atributos.sql` (agrega las columnas nuevas sin borrar datos).

Recomendado en `my.ini` (sección `[mysqld]`):

```ini
bind-address = 127.0.0.1          # la base solo acepta conexiones del propio servidor
default-time-zone = '-05:00'      # hora de Colombia
```

## 3. Compilar el WAR (máquina de desarrollo)

```bash
cd backend
mvn clean package          # ejecuta también las pruebas unitarias (RNF17)
# resultado: backend/target/polirestaurante.war
```

## 4. Tomcat 10.1 (nodo Apache Tomcat)

1. Copie `deploy/setenv.bat` a `C:\Tomcat10.1\bin\setenv.bat` y cambie **DB_PASSWORD** y
   **JWT_SECRET** (32+ caracteres aleatorios). Son las únicas cosas que cambian entre
   servidores (RNF15).
2. Copie `polirestaurante.war` a `C:\Tomcat10.1\webapps\`.
3. Inicie Tomcat. La aplicación queda en `http://<IP-del-servidor>:8080/polirestaurante/`.

### Tomcat como servicio de Windows (RNF03: arranque y reinicio automáticos)

```bat
cd C:\Tomcat10.1\bin
service.bat install Tomcat10
```

Como servicio, Tomcat **no lee setenv.bat**. Abra `tomcat10w.exe //ES//Tomcat10`
(*Configure Tomcat*) → pestaña **Java** → *Java Options* y agregue una línea por variable:

```
-DDB_URL=jdbc:mysql://localhost:3306/DB_Polirestaurante?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Bogota&characterEncoding=UTF-8
-DDB_USER=polirestaurante_app
-DDB_PASSWORD=...
-DJWT_SECRET=...
-Duser.timezone=America/Bogota
```

(Spring Boot acepta estas propiedades igual que las variables de entorno.) Luego en
*services.msc* ponga **Tomcat10** y **MySQL80** en inicio *Automático* y, en la pestaña
*Recuperación*, "Reiniciar el servicio" ante errores.

### HTTPS (RNF05)

En producción se expone solo HTTPS. Genere o importe el certificado y en
`conf/server.xml` agregue:

```xml
<Connector port="8443" protocol="org.apache.coyote.http11.Http11NioProtocol"
           maxThreads="150" SSLEnabled="true">
  <SSLHostConfig>
    <Certificate certificateKeystoreFile="conf/polirestaurante.p12"
                 certificateKeystorePassword="..." type="RSA" />
  </SSLHostConfig>
</Connector>
```

y cierre el puerto 8080 en el Firewall de Windows (deje abierto solo 8443).

## 5. Respaldos (mitigación de ADR-005)

`deploy/respaldo_bd.bat` hace `mysqldump` diario y conserva 14 días. Prográmelo en el
*Programador de tareas* (diario, 23:30, "Ejecutar tanto si el usuario inició sesión como si no").

## 6. Verificación rápida

1. Abra `https://<servidor>:8443/polirestaurante/` desde una tablet de la red.
2. Inicie sesión con `admin / 123456` (datos de prueba) → pestañas del administrador.
3. Con `cajero1 / 123456`: *Caja* → abrir caja → *Cuentas* → cobrar la mesa 3.
4. Con `prov1 / 123456`: ver la solicitud pendiente y marcarla como despachada.
5. **Cambie todas las contraseñas de prueba** desde *Usuarios y roles*.
