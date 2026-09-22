# Autenticación y decisiones

Usuario único por apodo o correo normalizados con Locale.ROOT. Los apodos no admiten @ para evitar ambigüedad entre identificadores. Contraseñas BCrypt, mínimo configurable de cuatro y límite explícito de 72 bytes. Consultas parametrizadas y restricciones UNIQUE en SQLite evitan duplicados incluso ante altas concurrentes.

La autenticación unifica estudiante y auxiliar en User + Role. No se crea una clase Admin duplicada; actualizar el UML. El registro ignora campos ajenos al DTO y siempre crea STUDENT. Las vistas se implementan con Thymeleaf y los casos de uso con servicios.

Aprovisionamiento alternativo en CMD, solo en computadora autorizada: definir APP_BOOTSTRAP_ENABLED=true, AUX_USERNAME, AUX_EMAIL y AUX_PASSWORD con los datos reales; ejecutar mvnw.cmd spring-boot:run. Detener y cerrar esa terminal después del alta. No guardar los comandos con la contraseña en documentación, capturas o archivos del repositorio. Es preferible el script que la solicita sin mostrarla.

APP_BOOTSTRAP_ENABLED está desactivado por defecto. Si se activa, los tres datos son obligatorios y el alta nunca modifica cuentas existentes. Ante colisión, corregir los datos y volver a intentar. El alta ocurre al iniciar; comprobar el arranque completo antes de considerar exitosa la operación.

SQLite es local por computadora. El esquema inicial se crea si no existe; futuras modificaciones requieren migraciones, no editar una tabla existente suponiendo que CREATE TABLE IF NOT EXISTS la actualiza.

No se ha implementado limitación de intentos, recuperación de contraseña ni verificación de correo. El panel de auxiliares aún no muestra datos de estudiantes.
