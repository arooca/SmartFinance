# SmartFinance

Aplicación web para gestionar ingresos, gastos, presupuestos, metas de ahorro y movimientos recurrentes. Las aportaciones a metas compartidas se registran como ahorro apartado y, al vencimiento, se convierten en gastos internos separados por usuario. Está desarrollada con Java 21, Spring Boot, Spring Security, Thymeleaf, Spring Data JPA y MySQL. Flyway mantiene el esquema de la base de datos.

Las metas de ahorro no conectan con cuentas bancarias: las aportaciones quedan registradas dentro de SmartFinance, y el gasto generado al vencimiento es un asiento interno de la aplicación.

## Requisitos

- Java 21.
- MySQL 8.4 para ejecutar la aplicación localmente.
- Docker Desktop y Docker Compose para usar el despliegue en contenedores.

## Ejecución local

1. Crea una base de datos vacía llamada `smartfinance` en MySQL.
2. Configura las variables de entorno antes de iniciar Spring Boot:

   ```powershell
   $env:SPRING_PROFILES_ACTIVE = "dev"
   $env:DB_URL = "jdbc:mysql://localhost:3306/smartfinance"
   $env:DB_USERNAME = "root"
   $env:DB_PASSWORD = "tu-contraseña"
   .\mvnw.cmd spring-boot:run
   ```

   En IntelliJ IDEA, puedes definir las mismas variables en la configuración de ejecución.
3. Abre `http://localhost:8080`.

Flyway crea y actualiza las tablas al iniciar la aplicación. Hibernate valida el esquema con `ddl-auto=validate`.

## Recuperación y verificación por correo

La aplicación incluye enlaces de un solo uso para verificar el correo y restablecer la contraseña. Configura un servidor SMTP antes de activar el envío:

| Variable | Uso |
| --- | --- |
| `APP_MAIL_ENABLED` | Activa el envío de correos (`true`/`false`). |
| `REQUIRE_EMAIL_VERIFICATION` | Exige verificar el correo antes de iniciar sesión. |
| `APP_EMAIL_VERIFICATION_REQUIRED` | Nombre de configuración Spring para activar la verificación en IntelliJ o al ejecutar el JAR; también se admite el alias `REQUIRE_EMAIL_VERIFICATION`. |
| `SESSION_COOKIE_SECURE` | Marca la cookie de sesión como `Secure`; actívalo cuando el navegador acceda por HTTPS. |
| `APP_PUBLIC_URL` | URL pública usada para construir los enlaces. |
| `APP_SECURITY_TRUST_CLOUDFLARE_HEADER` | Por defecto `false`. Solo actívala (`true`) cuando el tráfico llegue siempre a través de Cloudflare Tunnel para que el limitador use `CF-Connecting-IP`. |
| `MAIL_FROM` | Remitente de los correos. |
| `SMTP_HOST`, `SMTP_PORT` | Servidor y puerto SMTP. |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | Credenciales SMTP. Mantén la contraseña fuera del repositorio. |
| `SMTP_AUTH`, `SMTP_STARTTLS` | Activa autenticación y STARTTLS según el proveedor. |

En local, la verificación obligatoria está desactivada por defecto para conservar el flujo de desarrollo. Con SMTP activo, el registro muestra una página de verificación pendiente y el envío se procesa en segundo plano; si el mensaje tarda o no llega, se puede solicitar otro desde esa página o desde el inicio de sesión. Abrir el enlace del correo muestra una pantalla de confirmación; la cuenta solo se verifica al pulsar el botón.

Las solicitudes de recuperación de contraseña muestran el mismo mensaje tanto si el correo existe como si no. El trabajo de correo se procesa en segundo plano para reducir diferencias de respuesta y evitar que un proveedor SMTP lento bloquee el formulario.

Al compartir la aplicación por HTTPS con Cloudflare Quick Tunnel, inicia el JAR con `--server.servlet.session.cookie.secure=true`. Para volver al acceso local por HTTP, quita esa opción o usa `--server.servlet.session.cookie.secure=false`; con una cookie segura, el navegador no la enviará por HTTP.

## Importación bancaria

En **Movimientos → Importar CSV** puedes importar extractos CSV UTF-8. Se detectan separadores punto y coma, coma y tabulador; se aceptan columnas de importe con signo o columnas separadas de cargo/abono. El flujo omite filas ya importadas con la misma fecha, concepto e importe, admite hasta 2.000 filas y 5 MB, y crea categorías personales al encontrar nombres nuevos.

## Variables de entorno

| Variable | Uso |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | Perfil Spring: `dev` o `prod`. Por defecto, `dev`. |
| `DB_URL` | URL JDBC de MySQL. |
| `DB_USERNAME` | Usuario de MySQL. |
| `DB_PASSWORD` | Contraseña de MySQL. |
| `PORT` | Puerto HTTP en producción. Por defecto, `8080`. |
| `APP_MAIL_ENABLED`, `REQUIRE_EMAIL_VERIFICATION` | Envío de correo y obligatoriedad de verificación. |
| `APP_PUBLIC_URL`, `MAIL_FROM`, `SMTP_*` | URL, remitente y datos del servidor SMTP. |
| `SESSION_COOKIE_SECURE` | Cookie segura por HTTPS; desactivada por defecto en `dev` y activada en `prod`. |

No guardes credenciales reales en el repositorio.

Los formularios de inicio de sesión (10 intentos), recuperación de contraseña y reenvío de verificación (5 intentos cada uno) tienen un límite por dirección cada 15 minutos. El contador vive en memoria y se reinicia al reiniciar la aplicación. Si expones temporalmente la aplicación mediante Cloudflare Tunnel, puedes definir `APP_SECURITY_TRUST_CLOUDFLARE_HEADER=true` para distinguir las direcciones de origen; hazlo solo si Cloudflare es el único camino hacia la aplicación. Si la aplicación se ejecuta directamente en local, deja esta opción desactivada.

Cloudflare Quick Tunnel (`trycloudflare.com`) crea un enlace accesible para cualquiera que lo conozca; no añade una lista de invitados. La aplicación sigue requiriendo iniciar sesión para acceder a datos privados. No compartas el enlace si no quieres que otras personas abran la pantalla de acceso o intenten registrarse.

Cambiar el correo, cambiar la contraseña o restablecerla invalida las sesiones anteriores. Después de cambiar la contraseña, tendrás que iniciar sesión otra vez.

## Docker Compose

1. Copia `.env.example` a `.env` y cambia las contraseñas de ejemplo.
2. Desde la raíz del proyecto, ejecuta:

   ```powershell
   docker compose up --build
   ```

3. Abre `http://localhost:8080`. La aplicación y MySQL solo publican puertos en localhost; MySQL estará disponible desde este equipo en el puerto `3307`. Los datos persisten en el volumen `smartfinance-mysql`.
4. Para detener los contenedores, ejecuta `docker compose down`. Para borrar también la base de datos local del volumen, ejecuta `docker compose down -v`.

## Tests

```powershell
.\mvnw.cmd test
```

Los tests usan H2 y no necesitan una instancia local de MySQL.

## Actuator

- `GET /actuator/health` está disponible sin iniciar sesión y no muestra detalles internos.
- `GET /actuator/info` requiere autenticación.

## CI

GitHub Actions compila y ejecuta los tests en cada pull request y en los cambios enviados a `main` o `master`.
