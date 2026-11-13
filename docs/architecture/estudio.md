# Espacio de estudio: entrega funcional

Esta entrega añade funciones sobre el tutor existente. Conserva SQLite, la autenticación, las cuentas y las conversaciones. Las tablas nuevas se crean de forma aditiva al arrancar. No cambia la configuración del proveedor de IA.

## Funciones y alcance

| Función | Implementación |
| --- | --- |
| Títulos | Se generan localmente a partir del primer mensaje, quitando introducciones habituales. Se pueden renombrar. No hay una llamada extra de IA para titular. |
| Búsqueda | Busca por palabras o frases en título y mensajes, limitada al propietario. Devuelve hasta 30 coincidencias. No es búsqueda semántica ni vectorial. |
| Perfil | Recuerdos editables de objetivos y preferencias. Captura declaraciones explícitas como «prefiero…», «me ayuda…» o «estoy estudiando…», con el chat de origen. No infiere diagnósticos ni estilos fijos. |
| Proyectos | Botón Guardar recuerdo y captura de «recuerda que…» / «guarda que…». Cada recuerdo se puede revisar, editar o eliminar en Mi estudio. |
| Contexto entre chats | Al pedir recordar, buscar, retomar o hablar de un proyecto, consulta hasta cinco términos y recupera hasta tres chats propios distintos del actual. Añade enlaces al contexto recuperado. |
| Compartir | Copia pública de solo lectura de los mensajes existentes al crear el enlace. Token aleatorio de 256 bits, del que se guarda SHA-256. Caduca a los 30 días; se puede revocar. Crear otro enlace revoca el anterior. No publica tablas privadas ni mensajes futuros; si una respuesta del chat contiene fechas o recuerdos, ese texto sí forma parte de la copia compartida. |
| Práctica diaria | Banco inicial de ocho ejercicios, en rotación diaria, con elección y justificación antes de ver la explicación. Guarda el primer intento y una racha de participación. No evalúa automáticamente la calidad del razonamiento ni acredita dominio. |
| Diario | Una reflexión editable por fecha: lo aprendido, dificultad y próximo paso. Se muestran las últimas 100 entradas. No se envía automáticamente a la IA. |
| Pendientes | Registro manual, fecha, curso, completar/reabrir y eliminar. El tutor recibe hasta cinco entregas sin completar de los próximos siete días, con fecha actual y etiquetas HOY/FUTURA. Las consultas directas de agenda se responden desde los registros, sin usar IA. No se envían notificaciones. |
| Calendario | Calendario mensual, agenda del día, filtros y páginas de ocho elementos. Suscripción por URL de Canvas con actualización al abrir el panel/usar el chat, máximo una consulta automática cada cinco minutos. Botón Actualizar ahora. Reconcilia altas, cambios y elementos retirados del feed; conserva marcas por UID. Mantiene importación manual de copias .ics. |
| Guías | Generación explícita con tema, objetivo y chat propio opcional; usa preferencias y pendientes. Estructura solicitada: objetivos, conceptos, sesiones, ejemplo distinto, práctica sin resolver, autoevaluación y repasos. Guardar, leer, descargar Markdown, imprimir o guardar PDF desde el navegador. El contenido depende del modelo y debe revisarse. |

## Datos y límites de costo

StudyRepository gestiona study_notes, study_tasks, study_calendar_feeds, study_journal, study_guides y daily_attempts. ShareRepository gestiona chat_shares y shared_messages. Cada consulta o mutación privada incluye user_id; los enlaces públicos consultan exclusivamente la copia compartida.

No se ejecuta una segunda generación para extraer recuerdos, clasificar habilidades, titular, buscar, guardar reflexiones o preparar la práctica diaria. La memoria añade como máximo 5200 caracteres y comparte el presupuesto de 32000 caracteres de conversación; se seleccionan pares completos. Este es un límite de caracteres, no de tokens: las cuotas concretas del proveedor siguen aplicándose. Las guías hacen una generación solo al solicitarlas.

El perfil utiliza hasta seis declaraciones recientes; el contexto de proyectos, hasta tres recuerdos coincidentes. Son datos atribuidos al usuario, no instrucciones que puedan sustituir el contrato del tutor. El historial recuperado es parcial y no equivale a recordar toda la conversación.

La memoria automática se guarda después de un intercambio exitoso. Si falla ese guardado auxiliar, se conserva la respuesta del chat y se registra un aviso en el servidor; no se hace fallar retrospectivamente un intercambio ya guardado.

La fecha de estudio usa America/Guatemala. Los .ics con hora UTC o TZID reconocible se convierten a esa zona; los eventos de día completo conservan su fecha. Se omiten zonas no reconocidas, registros inválidos, cancelados y recurrencias. Máximo 2 MB / 1000 elementos por calendario. Se conservan horas locales y enlaces HTTPS de origen. VEVENT no significa automáticamente tarea: los metadatos de asignación (UID o URL de Canvas) y VTODO identifican entregas; el resto son eventos. Se puede corregir el tipo manualmente. Las importaciones anteriores quedan por clasificar hasta reconectar o revisarlas. Las reglas de formato se basan en [RFC 5545](https://www.rfc-editor.org/rfc/rfc5545).

## Rutas

- /study: panel privado; /study/api/dashboard: datos del propietario.
- /study/api/notes, /tasks, /journal, /guides, /daily, /calendar: operaciones de cada función bajo /study/api.
- GET /chat/api/search?q=texto: búsqueda propia.
- POST /chat/api/chats/{id}/title: renombrar.
- POST /chat/api/chats/{id}/share: crear una copia pública del chat propio.
- DELETE /chat/api/chats/{id}/share: revocar.
- GET /share/{token}: vista pública, sin edición; tokens inválidos, vencidos o revocados devuelven 404.
- /chat?chat={id}: abrir una conversación propia desde una fuente recuperada.

Las mutaciones conservan la protección CSRF existente. Los mensajes se muestran como texto seguro o con el renderizador Markdown saneado. Compartir requiere una confirmación en la interfaz que explica qué se publica. El servicio debe estar accesible desde la red del destinatario: un enlace de localhost no publica por sí solo la aplicación en internet.

## Comprobación de entrega

Realizado en el entorno de trabajo:

- Compilación del núcleo, repositorios, servicios y controladores nuevos con Java 21 y sustitutos temporales de las APIs de Spring. No es la compilación completa de Maven/Java 25.
- Pruebas ejecutadas de lógica del tutor, títulos, fechas, banco diario e importación .ics.
- SQL real extraído del código ejecutado con SQLite de Python: tablas idempotentes, propiedad, búsqueda, recuerdos sin duplicar, diario por fecha, primer intento diario, reimportación y copias revocables que no incorporan mensajes futuros. No sustituye el controlador JDBC.
- Sintaxis de JavaScript y comprobación del parche sobre su base.

Incluido para ejecutar en el equipo del usuario:

- Pruebas JUnit de los repositorios de estudio y enlaces, búsqueda/renombrado y servicio de memoria/guías.
- Pruebas HTTP/Spring Security de panel privado, CSRF, enlace público, propiedad y revocación en AuthFlowTest.

Pendiente: ejecutar ./mvnw test con dependencias reales, verificar las pantallas en navegador y generar una guía con el proveedor configurado. No se afirma que esas comprobaciones hayan pasado en este entorno.

## Corrección de calendario y agenda

- `StudyAgenda` calcula hoy/mañana/fechas explícitas/próximos siete días/esta semana/vencidos y devuelve una respuesta determinista para consultas reconocidas. Se guarda como cualquier respuesta del chat. No gasta tokens del proveedor. Una petición de resolver una tarea mantiene el flujo de tutoría.
- Hoy se calcula en `America/Guatemala`, no se fija a la fecha del desarrollo ni se confía en el reloj del navegador. Los vencidos se consultan explícitamente; los eventos se presentan en un apartado propio.
- El feed no verifica entregas ni calificaciones de Canvas. «Completado» es una marca local. Una fecha pasada sin esa marca no demuestra que el estudiante no entregó.
- El panel consulta el estado cada minuto mientras está visible; el servidor limita las descargas automáticas a cada cinco minutos. También consulta al enviar un mensaje. No hay servicio en segundo plano cuando no se usa la app ni notificaciones con la app cerrada.
- `POST /study/api/calendar/connect` recibe URL y etiqueta institucional. La conexión inicial sustituye las copias importadas anteriores; conserva las tareas manuales y marcas por UID.
- `POST /study/api/calendar/sync` actualiza inmediatamente; `DELETE /study/api/calendar/connection` desconecta conservando la copia local. `PUT /study/api/tasks/{id}/kind` corrige tarea/evento sin perder la elección en futuras actualizaciones.
- Una actualización válida reconcilia únicamente el feed del propietario dentro de una transacción. Un error de conexión/parsing no reemplaza la copia anterior y se indica en pantalla/chat. Los elementos ausentes se ocultan: el feed puede retirar fechas fuera de su ventana de publicación, no necesariamente porque se haya eliminado la tarea en Canvas.
- Las recurrencias RRULE/RDATE/EXDATE y registros incompatibles se omiten con aviso; las cancelaciones explícitas se retiran. Comprueba Canvas cuando existan omisiones. No es un lector completo de RFC 5545.
- URL privada persistida en SQLite; nunca se devuelve al frontend ni se envía al modelo. No subir la base de datos a repositorios. Solo HTTPS, ruta `/feeds/calendars/…ics`, puertos estándar y hosts `*.instructure.com` o un allowlist institucional configurado por el servidor; no sigue redirects, bloquea direcciones internas y limita descarga/tiempo/tamaño.
- Para un dominio institucional propio: variable de entorno `APP_CALENDAR_ALLOWED_HOSTS=canvas.tu-universidad.edu` (varios separados por comas). No habilitar dominios controlados por usuarios o comodines. No cambia `application.properties` ni la configuración de Groq.

Fuentes técnicas: [Canvas Calendar](https://community.instructure.com/en/kb/articles/662743-what-is-the-calendar), [Calendar Feed](https://community.instructure.com/en/kb/articles/662804-how-do-i-view-the-calendar-ical-feed-to-import-and-subscribe-to-an-external-calendar), [API de calendarios de Canvas](https://github.com/instructure/canvas-lms/blob/master/app/controllers/calendar_events_api_controller.rb), [RFC 5545](https://www.rfc-editor.org/rfc/rfc5545).
