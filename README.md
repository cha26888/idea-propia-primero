# Idea propia primero

Proyecto POO UVG · Sección 30 · Grupo 3.

Aplicación web para fortalecer el razonamiento propio antes de recibir ayuda de IA.


## Requisitos y ejecución (Windows CMD)

Java 25, Git y la carpeta `.mvn` del repositorio original. Spring Boot 4.1.1 y las dependencias quedan fijados en `pom.xml`. No se instala un servidor SQLite ni Maven global.

```cmd
java -version
javac -version
echo %JAVA_HOME%
mvnw.cmd test
mvnw.cmd spring-boot:run
```

Abrir http://localhost:8080 . Detener con Ctrl+C. El primer inicio descarga dependencias y crea automáticamente `data/idea-propia-primero.db` y la tabla `users`. Mantener esa carpeta para conservar las cuentas entre reinicios. Cada computadora tiene su propia base; Git no sincroniza cuentas.

## Uso

1. Abrir Crear cuenta.
2. Escribir usuario/apodo, correo y contraseña.
3. Tras registrarse, iniciar sesión con usuario **o** correo y la contraseña.
4. La página de bienvenida es privada. Cerrar sesión con el botón.

Usuario: 3–30 letras ASCII, números, puntos, guiones o guiones bajos; sin espacios ni @. Usuario y correo son únicos y se comparan sin distinguir mayúsculas. Se conserva la escritura original para presentación. El correo se valida por formato, pero **no se verifica su propiedad por email**.

Contraseña: mínimo **4 caracteres**, sin requisitos de mayúsculas, minúsculas, números ni símbolos. No se eliminan espacios de la contraseña; no se admite una contraseña compuesta únicamente por espacios. Máximo 72 bytes UTF-8 debido a BCrypt. Las contraseñas nunca vuelven a mostrarse como valor del formulario tras un error.

## Configuración

En `src/main/resources/application.properties`:

```properties
app.auth.password-min-length=${APP_PASSWORD_MIN_LENGTH:4}
app.database.path=${APP_DATABASE_PATH:data/idea-propia-primero.db}
app.bootstrap.enabled=${APP_BOOTSTRAP_ENABLED:false}
```

Cambiar el `4` y reiniciar ajusta el mínimo de los registros nuevos. La misma configuración controla la validación del servidor y la ayuda visual. También puede cambiarse temporalmente desde CMD:

```cmd
set APP_PASSWORD_MIN_LENGTH=8
mvnw.cmd spring-boot:run
```

El cambio no invalida contraseñas ya registradas. Para volver al valor del archivo: `set APP_PASSWORD_MIN_LENGTH=` y reiniciar. No se carga `.env` automáticamente.

## Auxiliares

El registro público siempre crea STUDENT, aunque se envíe un campo role manipulado. No existe formulario público para asignar roles. Para crear una cuenta de auxiliar, detener el servidor y abrir PowerShell en la raíz del proyecto; ejecutar el script local:

```powershell
powershell -NoProfile -File .\scripts\crear-auxiliar.ps1
```

El script solicita usuario, correo y contraseña sin mostrarla en pantalla. Activa el aprovisionamiento solo en el proceso iniciado, crea la cuenta al arrancar y mantiene la aplicación abierta. Cuando termine de arrancar, detener con Ctrl+C y volver a arrancar normalmente con `mvnw.cmd spring-boot:run`. El script restaura las variables de entorno al terminar. Si el usuario o correo existe, el inicio falla y no modifica ni eleva privilegios de ninguna cuenta. No repetir para la misma cuenta.

Si la política de PowerShell impide ejecutar scripts, utilizar el procedimiento de variables de entorno descrito en `docs/architecture/autenticacion.md` según las políticas del equipo; no desactivar políticas de la computadora para ejecutar este archivo.

Un auxiliar puede iniciar sesión por el mismo formulario y acceder a `/admin`. El panel confirma el rol; la consulta de estudiantes e historiales sigue pendiente.

## Arquitectura implementada

| Archivo o paquete | Responsabilidad |
| --- | --- |
| model/User y Role | Cuenta y rol, sin exposición de hash en vistas. |
| dto/RegisterForm | Datos y validación de registro. |
| repository/UserRepository | Esquema SQLite, consultas parametrizadas y unicidad en base de datos. |
| service/RegistrationService | Alta de estudiantes y aprovisionamiento local de auxiliares. |
| service/PasswordPolicy | Política configurable de contraseñas. |
| security/SecurityConfig | Spring Security, BCrypt, sesión, rutas y logout POST. |
| controller/AuthController | Formularios y mensajes de registro. |
| controller/AccountController | Bienvenida y panel de auxiliares. |
| config/AuxiliarProvisioning | Alta local explícita, desactivada normalmente. |
| templates/auth | Login, registro y bienvenida. |
| templates/admin/index.html | Página privada de auxiliares. |
| static/css y static/js | Estilos adaptables y mostrar/ocultar contraseña. |

Spring Security procesa POST /login y POST /logout. Se conservan CSRF, protección contra fijación de sesión y escape de texto de Thymeleaf. Los errores de login son genéricos. No se agregan sesiones ni contraseñas a logs. Las sesiones expiran después de 30 minutos de inactividad.

El modelo usa `username` como apodo en lugar del `name` del diseño preliminar. Un único User con Role sustituye las cuentas duplicadas User/Admin para autenticación. Actualizar el UML con esta decisión. Los chats y relaciones restantes todavía no se implementan.

## Pruebas

```cmd
mvnw.cmd test
```

Incluye flujo de registro, login por ambos identificadores, hash BCrypt, persistencia al abrir otra conexión, duplicados sin distinción de mayúsculas, entradas inválidas, CSRF, logout, rutas privadas, rol público fijo y acceso de auxiliares. Las pruebas usan bases independientes de `data/`.

Prueba manual antes de integrar:

- Registrar un usuario con contraseña de cuatro caracteres e ingresar por usuario y por correo.
- Comprobar mensajes al repetir usuario/correo, usar tres caracteres de contraseña o equivocarse al entrar.
- Cerrar sesión y comprobar que `/welcome` solicita login.
- Con estudiante, comprobar que `/admin` devuelve acceso denegado.
- Reiniciar la aplicación y entrar con la misma cuenta para comprobar persistencia.
- Cambiar el mínimo a ocho y comprobar el rechazo de registros nuevos de cuatro caracteres.
- Crear un auxiliar local y comprobar su acceso, sin permitir elevación por el formulario público.

## Pendientes y límites

- [x] Base Spring Boot, estructura, GitHub y documentación inicial.
- [x] Página inicial original comprobada por el equipo.
- [x] Código de autenticación y SQLite preparado en esta entrega.
- [ ] Ejecutar y aprobar las pruebas de esta entrega en Java 25.
- [ ] Confirmar el flujo manual, persistencia tras reinicio y panel de auxiliar.
- [ ] Revisar e integrar el pull request de autenticación.
- [ ] Actualizar UML, investigación de persistencia y planificación.
- [ ] Implementar chats, mensajes, memoria, IA e historiales.
- [ ] Implementar consultas del auxiliar, diario y tareas según prioridad.
- [ ] Mostrar a usuarios y documentar retroalimentación.

### Validación del módulo de autenticación

- [x] Pruebas automatizadas ejecutadas con `mvnw.cmd clean test`, sin fallos ni errores.
- [x] Registro y persistencia de cuentas en SQLite.
- [x] Login mediante usuario y correo.
- [x] Validación de duplicados y longitud mínima de contraseña.
- [x] Errores visibles sin borrar la contraseña en el registro con JavaScript.
- [x] Logout y protección de páginas privadas.
- [x] Restricción del panel de auxiliares para estudiantes.
- [x] Conservación de cuentas después de reiniciar el servidor.

La política de cuatro caracteres es una decisión para el prototipo académico. Antes de publicar con usuarios reales se debe revisar esa política, incorporar limitación de intentos, recuperación y verificación de correo, y usar HTTPS con cookies Secure. Este código no afirma cubrir esas funciones. SQLite debe evaluarse para la concurrencia y despliegue previstos.

## Equipo y trabajo compartido

Joel Alejandro: usuario/autenticación. Henry: Message, Agent y AIConnector. Jade: chat y vistas asociadas. José: coordinación del agente, auxiliares e integración. Esta distribución procede de fase 2 y debe actualizarse por acuerdo al adaptar el diseño.

Trabajar en ramas de tarea, revisar cambios y pruebas, publicar la rama y abrir un pull request hacia main. No subir `data/`, `target/`, credenciales ni secretos. Conservar `.mvn` y los scripts Wrapper en el repositorio.

La fase 3 exige al menos un requisito funcional completo por integrante y más de tres commits semanales por integrante con aportes reales. Registrar tiempos, tareas, responsables, estimaciones, calendario y pendientes. El README no sustituye el documento solicitado en Canvas.

Las carpetas `docs/requirements`, `docs/architecture`, `docs/database`, `docs/planning`, `docs/time-logs`, `docs/testing` y `docs/user-feedback` conservan su propósito. Las carpetas de funciones futuras siguen como esqueleto.

## Continuar con otro chat

Compartir este README, el pom.xml, el UML y los archivos del módulo a modificar. Un enlace privado no da acceso automático al código. Revisar el código actual, respetar contratos y responsabilidades, no inventar pruebas aprobadas ni tareas terminadas, entregar comandos de la terminal utilizada y actualizar estado/documentación. Proporcionar comentario para el registro de tiempo al cerrar cada tarea.


## Actualización: interfaz compartida y registro asíncrono

Tema central en `static/css/theme.css`, componentes en `static/css/components.css` y fragmentos en `templates/fragments/ui.html`. Guía del equipo: `docs/architecture/interfaz.md`.

Con JavaScript, el registro envía a POST `/register/submit` y muestra mensajes por campo sin borrar la contraseña. Sin JavaScript, POST `/register` utiliza el formulario convencional. Las respuestas JSON contienen únicamente mensajes y estado, nunca valores de contraseña. La base y los roles se conservan.

El ZIP enviado para revisión contenía AuthController de la versión anterior junto con la prueba JSON nueva. Esta entrega incorpora el controlador actualizado y separa explícitamente las rutas HTML/JSON. Ejecutar `mvnw.cmd clean test` para recompilar todo. No se han ejecutado las pruebas Java de esta revisión en el entorno de preparación; el resultado debe verificarse en Java 25.
