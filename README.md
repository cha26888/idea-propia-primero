# Idea propia primero

Proyecto de Programación Orientada a Objetos de la Universidad del Valle de Guatemala, segundo semestre de 2026. Sección 30, grupo 3.

Aplicación web que busca fortalecer el razonamiento propio antes de recibir ayuda de inteligencia artificial. El estudiante comparte su intento o evidencia de razonamiento y el agente solicita aclaraciones o proporciona orientación según el contexto.

Repositorio: https://github.com/cha26888/idea-propia-primero

## Estado actual

**Fase 3: preparación técnica e implementación inicial.** Última actualización: 22 de septiembre de 2026.

El repositorio contiene el proyecto base Spring Boot y la estructura de carpetas. Todavía no contiene las funcionalidades completas de usuario, chat, agente o auxiliar.

### Completado

- [x] Análisis y diseño documentados en las entregas 1 y 2.
- [x] Repositorio privado creado y clonado.
- [x] Seis invitaciones enviadas a colaboradores; aceptación pendiente de verificar individualmente.
- [x] Git, JDK 25.0.3 y JAVA_HOME comprobados en la computadora de Joel Alejandro.
- [x] Paquetes de extensiones Java y Spring Boot instalados en esa computadora.
- [x] Proyecto base generado con Maven Wrapper.
- [x] Ejecución de `mvnw.cmd test`: una prueba, cero fallos, cero errores y BUILD SUCCESS.
- [x] Estructura de paquetes, vistas, recursos, pruebas y documentación publicada en main.

Esta prueba inicial comprueba el arranque del contexto de Spring; no demuestra que existan funciones de negocio. La estructura contiene archivos `.gitkeep` para conservar carpetas vacías.

### Pendiente

- [ ] Confirmar las versiones y dependencias exactas del `pom.xml` como referencia común del equipo.
- [ ] Verificar el arranque del servidor web y crear una página inicial.
- [ ] Preparar y comprobar el entorno de cada integrante.
- [ ] Implementar registro, login, logout y control de acceso por rol.
- [ ] Implementar chats, mensajes e historial persistente.
- [ ] Implementar evaluación de evidencia, memoria por conversación y conexión con IA.
- [ ] Implementar consultas del auxiliar con permisos adecuados.
- [ ] Integrar SQLite, su esquema y la estrategia de acceso a datos.
- [ ] Completar la comparación y justificación de tecnologías de persistencia.
- [ ] Actualizar planificación, responsables, fechas y registros de tiempo.
- [ ] Probar requisitos completos y mostrar el prototipo a usuarios.
- [ ] Registrar retroalimentación y ajustes.
- [ ] Evaluar diario y tareas como ampliaciones de menor prioridad.

## Equipo y distribución de referencia

La siguiente distribución procede de la segunda entrega. Debe actualizarse mediante acuerdo del equipo para la fase 3; no significa que las tareas estén implementadas.

| Integrante | Trabajo planificado en la segunda entrega |
| --- | --- |
| Joel Alejandro Chávez Pérez | Usuario, autenticación y preparación de la demostración. |
| Henry Emanuel Vicente Alvarado | Message, Agent, AIConnector y muestra a usuarios. |
| Jade Rocío Monserrath García Salic | Chat, ChatController, ChatView y Admin. |
| José David Fernando Ixcot Estrada | AgentController, AdminController, AdminView, integración y pruebas integrales. |

Todos deben programar y participar en análisis y diseño. Las fechas antiguas deben replanificarse; no se consideran compromisos vigentes automáticamente.

## Tecnologías

| Tecnología | Uso y estado |
| --- | --- |
| Java | Lenguaje principal. JDK 25.0.3 comprobado en el equipo inicial. |
| Spring Boot y Spring MVC | Base de la aplicación web. La versión real está en `pom.xml`. |
| Maven Wrapper | Compilación, dependencias, pruebas y ejecución con la versión definida en `.mvn/wrapper/maven-wrapper.properties`. |
| Thymeleaf | Vistas HTML del servidor, seleccionado para el proyecto. Verificar dependencia en `pom.xml`. |
| HTML, CSS y JavaScript | Estructura, presentación e interacción del navegador. |
| Validation | Validación de entradas, seleccionada al generar el proyecto. Verificar dependencia en `pom.xml`. |
| SQLite | Persistencia seleccionada en fase 2; integración pendiente. |
| Proveedor de IA | Pendiente de elección e integración; AIConnector debe aislar al resto de la aplicación del proveedor. |
| Git, GitHub y VS Code | Control de versiones, colaboración y desarrollo. |

No cambiar versiones ni agregar frameworks sin comunicarlo al equipo y actualizar la documentación. El `pom.xml` y el Wrapper son la fuente de verdad técnica.

## Preparación y ejecución en Windows

Los comandos de esta sección corresponden a **CMD**, ejecutados uno por uno. No pegar la sintaxis `%JAVA_HOME%` directamente en PowerShell o Git Bash.

### 1. Requisitos

- Aceptar la invitación al repositorio privado.
- Instalar Git y un JDK compatible con la versión Java indicada en `pom.xml`; el entorno inicial usa JDK 25.
- Configurar `JAVA_HOME` con la carpeta real del JDK, sin agregar `bin` al valor.
- Disponer de conexión a Internet para la primera descarga de Maven y dependencias.
- Instalar VS Code si se utiliza como editor.

Comprobar:

```cmd
git --version
java -version
javac -version
echo %JAVA_HOME%
"%JAVA_HOME%\bin\javac" -version
```

La ruta del JDK varía según la computadora. Si Java funciona pero JAVA_HOME falla, localizar la instalación activa con:

```cmd
java -XshowSettings:properties -version 2>&1 | findstr /C:"java.home"
```

Configurar la variable con esa ruta real y volver a abrir terminales y VS Code. No copiar automáticamente la ruta personal de otro integrante.

### 2. Clonar

Abrir CMD en la carpeta donde se alojará el proyecto:

```cmd
git clone https://github.com/cha26888/idea-propia-primero.git
cd idea-propia-primero
code .
```

Si el repositorio ya está clonado, abrir esa carpeta. Cada integrante trabaja en su propia copia local del mismo repositorio.

### 3. Extensiones de VS Code

```cmd
code --install-extension vscjava.vscode-java-pack
code --install-extension vmware.vscode-boot-dev-pack
```

### 4. Identidad de Git

Comprobar dentro del repositorio:

```cmd
git config user.name
git config user.email
```

Si faltan o son incorrectos, sustituir los ejemplos por los datos propios:

```cmd
git config user.name "TU NOMBRE"
git config user.email "TU CORREO ASOCIADO A GITHUB"
```

Estos comandos configuran la identidad para este repositorio. Cada integrante debe usar su cuenta e identidad.

### 5. Ejecutar pruebas

Desde la carpeta que contiene `pom.xml` y `mvnw.cmd`:

```cmd
mvnw.cmd test
```

Resultado esperado: `BUILD SUCCESS`. La primera ejecución puede tardar por las descargas. El Wrapper permite utilizar Maven sin instalarlo globalmente.

### 6. Iniciar el servidor

```cmd
mvnw.cmd spring-boot:run
```

Esperar el mensaje de inicio de la aplicación y comprobar el puerto indicado en la consola. Con la configuración predeterminada, abrir http://localhost:8080 . Detener con **Ctrl + C**.

**Estado de verificación:** este arranque web todavía está pendiente de comprobar. Como no hay página inicial ni controlador de inicio implementados, una respuesta HTTP 404 en `/` puede ser normal aunque el servidor haya arrancado. No hay cuentas, pantallas funcionales ni credenciales de demostración disponibles todavía.

### 7. Empaquetar

```cmd
mvnw.cmd clean package
```

Genera los resultados en `target/`. Este comando queda como procedimiento de uso; no se ha documentado aún su ejecución en el equipo. No subir `target/` al repositorio.

## Estructura y responsabilidades

Paquete raíz: `gt.edu.uvg.ideapropiaprimero`. Mantener la clase principal en este paquete para que Spring pueda descubrir los componentes de los subpaquetes.

Las rutas Java de la tabla son relativas a `src/main/java/gt/edu/uvg/ideapropiaprimero/`.

| Paquete | Responsabilidad |
| --- | --- |
| `model/` | Objetos del dominio: usuarios, chats, mensajes y otros conceptos del sistema. |
| `controller/` | Recibir peticiones HTTP, validar entradas y delegar operaciones. |
| `service/` | Casos de uso y lógica de negocio; coordinar persistencia e integración. |
| `repository/` | Acceso a datos y consultas; elección de JDBC u otra estrategia pendiente. |
| `dto/` | Datos de entrada y salida de formularios o peticiones, cuando sean necesarios. |
| `config/` | Configuración de componentes de la aplicación. |
| `security/` | Autenticación, autorización y protección de contraseñas. |
| `exception/` | Excepciones y manejo consistente de errores. |
| `integration/ai/` | Conexión con el proveedor de IA y adaptación de solicitudes y respuestas. |

| Ruta de recursos | Contenido previsto |
| --- | --- |
| `src/main/resources/templates/auth/` | Login y registro. |
| `src/main/resources/templates/chat/` | Conversación e historial. |
| `src/main/resources/templates/admin/` | Consultas del auxiliar. |
| `src/main/resources/templates/journal/` | Diario, ampliación pendiente. |
| `src/main/resources/templates/task/` | Tareas, ampliación pendiente. |
| `src/main/resources/templates/fragments/` | Fragmentos Thymeleaf compartidos. |
| `src/main/resources/templates/error/` | Páginas de error. |
| `src/main/resources/static/css/` | Estilos. |
| `src/main/resources/static/js/` | Código del navegador. |
| `src/main/resources/static/images/` | Imágenes públicas de la interfaz. |
| `src/main/resources/application.properties` | Configuración de la aplicación sin secretos. |

Las pruebas están en `src/test/java/gt/edu/uvg/ideapropiaprimero/`, organizadas en `model`, `controller`, `service`, `repository` e `integration`. Los datos ficticios de prueba van en `src/test/resources/fixtures/`.

| Carpeta documental | Contenido previsto |
| --- | --- |
| `docs/requirements/` | Requisitos, prioridades y criterios de aceptación. |
| `docs/architecture/` | UML actualizado y decisiones de arquitectura. |
| `docs/database/` | Investigación, esquema y decisiones de persistencia. |
| `docs/planning/` | Tareas, responsables, estimaciones y calendario. |
| `docs/time-logs/` | Registro individual de tiempo. |
| `docs/testing/` | Plan de pruebas, resultados y evidencia. |
| `docs/user-feedback/` | Demostraciones, observaciones y ajustes. |

Estas carpetas son un esqueleto, no evidencia de módulos terminados. Retirar `.gitkeep` al agregar contenido real en cada carpeta.

## Adaptación del diseño a Spring MVC

El diseño previo usa UserView, ChatView y AdminView como responsabilidades de presentación. En la implementación web estas responsabilidades se materializarán principalmente en plantillas Thymeleaf.

La propuesta para implementar mantiene controladores HTTP pequeños y traslada la lógica de negocio a servicios. Por ejemplo, la coordinación de Agent y AIConnector puede residir en un servicio del agente. Cualquier cambio respecto de AgentController u otras clases del UML debe documentarse y reflejarse en el diagrama actualizado.

- Las vistas presentan datos; no consultan directamente la base de datos ni al proveedor de IA.
- Los controladores delegan reglas de negocio a servicios.
- Los repositorios encapsulan el acceso a datos.
- AIConnector se ubica en la integración con IA.
- Crear clases cuando exista una responsabilidad concreta; no llenar todas las carpetas con clases vacías.
- Usar atributos privados, nombres claros e inyección de dependencias por constructor en componentes Spring.

## Alcance funcional y prioridades

Las prioridades siguientes reflejan los documentos previos. Deben confirmarse al planificar la fase 3.

| Prioridad | Funciones previstas |
| --- | --- |
| 1 | Registro, login por rol, protección y validación de contraseñas, acceso al chat, envío de evidencia y flujo principal del agente con proveedor de IA. |
| 2 | Historial de chats y memoria dentro de la conversación. |
| 3 | Logout, lista de estudiantes y consulta de historial por auxiliar. |
| 4 | Diario y tareas del documento de apoyo. |

Aunque logout figura con prioridad 3 en el documento, debe incorporarse al completar el flujo de autenticación. La autorización debe implementarse junto con cada función protegida.

Un requisito está terminado cuando funciona de extremo a extremo, valida errores y permisos, conserva los datos que correspondan, tiene pruebas pertinentes y puede demostrarse. Una clase aislada no equivale a un requisito completo.

## Persistencia y seguridad

SQLite fue seleccionado en la segunda entrega. Todavía falta comparar alternativas, justificar su ajuste al prototipo y definir esquema, inicialización y acceso desde Java.

- No almacenar contraseñas en texto plano; usar una función adecuada de hashing de contraseñas.
- No permitir que el registro público conceda el rol de auxiliar.
- Verificar en el servidor la propiedad de chats y los permisos para consultar historiales.
- Mantener la memoria del agente separada por usuario y conversación.
- No subir claves de API, credenciales, bases de datos con datos reales ni conversaciones privadas.
- Incorporar exclusiones de los archivos SQLite locales al configurar su ubicación.
- Guardar secretos mediante variables de entorno. Sus nombres se documentarán al implementar cada integración.
- Spring Boot no carga automáticamente un archivo `.env` por el solo hecho de existir; cualquier mecanismo para cargarlo debe configurarse y documentarse.
- Manejar fallos y tiempos de espera del proveedor de IA sin mostrar secretos al usuario.

Actualmente no hay proveedor de IA configurado ni variables de API obligatorias definidas.

## Trabajo en equipo con Git

Los commits iniciales se hicieron en `main`. Para el desarrollo de funcionalidades, utilizar ramas por tarea y revisión mediante pull request.

Antes de empezar una tarea, con la copia local limpia:

```cmd
git switch main
git pull --ff-only origin main
git switch -c feat/registro-usuario
```

El nombre de rama es un ejemplo; usar uno distinto para cada tarea. Al terminar, ejecutar pruebas y revisar los cambios:

```cmd
mvnw.cmd test
git status
git diff
```

Preparar explícitamente los archivos de la tarea con `git add`, revisar `git diff --cached` y crear un commit descriptivo. Publicar la rama y abrir un pull request hacia `main`:

```cmd
git push -u origin feat/registro-usuario
```

Si Git informa de conflictos, resolverlos antes de continuar. No usar force push sobre `main`. Acordar cambios de contratos compartidos antes de modificar métodos usados por otro integrante.

## Requisitos de la tercera entrega

- Implementar al menos un requisito funcional completo por integrante: al menos cuatro para este equipo.
- Mantener más de tres commits semanales por integrante: al menos cuatro, con aportes reales y significativos.
- La guía también advierte que menos de dos contribuciones significativas semanales impide obtener calificación; esto no sustituye el objetivo anterior.
- Incluir tareas de programación, análisis y diseño para todos.
- Describir tareas, prioridad del requisito, horas estimadas, responsable y fecha de terminación.
- Preparar calendario y registros individuales de tiempo.
- Mostrar el sistema a usuarios y documentar retroalimentación.
- Entregar el documento solicitado en Canvas y la dirección del repositorio.

Los commits de estructura y documentación ayudan a organizar el trabajo, pero no sustituyen la implementación exigida. Este README tampoco sustituye el documento de entrega.

Registro sugerido de tiempo:

| Fecha | Inicio | Fin | Interrupciones (min) | Tiempo trabajado (min) | Tarea | Comentarios |
| --- | --- | --- | --- | --- | --- | --- |
| Completar | Completar | Completar | Completar | Completar | Completar | Completar |

Tiempo trabajado = duración entre inicio y fin menos interrupciones. Registrar tiempos reales.

## Indicaciones para continuar con ayuda de un chat

Compartir este README, el `pom.xml`, el UML vigente y los archivos relacionados con la tarea. Un chat no obtiene acceso al código por recibir únicamente el enlace de este repositorio privado.

1. Revisar los archivos actuales antes de proponer cambios; no asumir que las carpetas contienen funcionalidades.
2. Mantener Java, Spring Boot, MVC, Thymeleaf y la decisión de persistencia, salvo cambio acordado y justificado.
3. Distinguir decisiones de las entregas anteriores, propuestas de adaptación e implementación comprobada.
4. Indicar rutas exactas y explicar el propósito de cada archivo modificado.
5. Respetar el reparto del equipo y los contratos de clases compartidas.
6. Entregar comandos adecuados para la terminal utilizada; el flujo inicial del equipo usa CMD en Windows.
7. No inventar resultados de pruebas, credenciales, requisitos cumplidos ni tiempos de trabajo.
8. Actualizar pendientes y documentación cuando se complete una función.
9. Al cerrar una tarea, proporcionar una descripción y comentario para el registro individual de tiempo.

## Documentos de referencia

- Primera Entrega Proyecto POO: problema, usuarios y propuesta.
- POO-SegundaEntregaProyecto: requisitos, diseño, tecnologías y planificación.
- Guía de la Tercera Entrega del proyecto 2026: requisitos de entrega y evaluación.
- Tercera entrega POO apoyo: resumen de diseño y ampliaciones de diario y tareas.

Estos documentos fueron consultados durante la preparación; su incorporación a `docs/` queda pendiente. No se incluyen enlaces relativos a archivos que todavía no están en el repositorio.
