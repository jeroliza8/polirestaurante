@echo off
rem =====================================================================
rem  Respaldo diario de DB_Polirestaurante (mitigacion del riesgo de
rem  "punto unico de fallo" de ADR-005). Programelo en el Programador de
rem  tareas de Windows, por ejemplo todos los dias a las 23:30.
rem  Conserva los ultimos 14 respaldos.
rem =====================================================================
setlocal
set "MYSQL_BIN=C:\Program Files\MySQL\MySQL Server 8.0\bin"
set "DESTINO=D:\Respaldos\PoliRestaurante"
set "USUARIO=root"
rem La clave de root se lee de un archivo de opciones protegido, no se escribe aqui:
set "OPCIONES=C:\ProgramData\MySQL\respaldo.cnf"

if not exist "%DESTINO%" mkdir "%DESTINO%"
for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd_HHmm"') do set "FECHA=%%i"

"%MYSQL_BIN%\mysqldump.exe" --defaults-extra-file="%OPCIONES%" -u %USUARIO% ^
  --single-transaction --routines --triggers --databases DB_Polirestaurante ^
  > "%DESTINO%\DB_Polirestaurante_%FECHA%.sql"

if errorlevel 1 (
  echo [%date% %time%] ERROR en el respaldo >> "%DESTINO%\respaldos.log"
  exit /b 1
)
echo [%date% %time%] OK DB_Polirestaurante_%FECHA%.sql >> "%DESTINO%\respaldos.log"

rem Borra respaldos de mas de 14 dias
forfiles /p "%DESTINO%" /m *.sql /d -14 /c "cmd /c del @path" 2>nul
endlocal
