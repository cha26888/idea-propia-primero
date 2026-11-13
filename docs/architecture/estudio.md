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
| Compartir | Copia pública de solo lectura de los mensajes existentes al crear el enlace. Token aleatorio de 256 bits, del que se guarda SHA-256. Caduca a los 30 días; se puede revocar. Crear otro enlace revoca el anterior. No incluye perfil, diario o pendientes ni actualizaciones futuras. |
| Práctica diaria | Banco inicial de ocho ejercicios, en rotación diaria, con elección y justificación antes de ver la explicación. Guarda el primer intento y una racha de participación. No evalúa automáticamente la calidad del razonamiento ni acredita dominio. |
| Diario | Una reflexión editable por fecha: lo aprendido, dificultad y próximo paso. Se muestran las últimas 100 entradas. No se envía automáticamente a la IA. |
| Pendientes | Registro manual, fecha, curso, completar/reabrir y eliminar. El tutor recibe hasta tres pendientes sin completar. No se envían notificaciones. |
| Calendario | Importación manual .ics de eventos o tareas individuales. Identifica UID para actualizar sin duplicar y conserva el estado de completado. No sincroniza por OAuth, no expande recurrencias y no elimina cancelaciones anteriores. Se informa cuántos eventos se omitieron. |
| Guías | Generación explícita con tema, objetivo y chat propio opcional; usa preferencias y pendientes. Estructura solicitada: objetivos, conceptos, sesiones, ejemplo distinto, práctica sin resolver, autoevaluación y repasos. Guardar, leer, descargar Markdown, imprimir o guardar PDF desde el navegador. El contenido depende del modelo y debe revisarse. |

## Datos y límites de costo

StudyRepository gestiona study_notes, study_tasks, study_journal, study_guides y daily_attempts. ShareRepository gestiona chat_shares y shared_messages. Cada consulta o mutación privada incluye user_id; los enlaces públicos consultan exclusivamente la copia compartida.

No se ejecuta una segunda generación para extraer recuerdos, clasificar habilidades, titular, buscar, guardar reflexiones o preparar la práctica diaria. La memoria añade como máximo 5200 caracteres y comparte el presupuesto de 32000 caracteres de conversación; se seleccionan pares completos. Este es un límite de caracteres, no de tokens: las cuotas concretas del proveedor siguen aplicándose. Las guías hacen una generación solo al solicitarlas.

El perfil utiliza hasta seis declaraciones recientes; el contexto de proyectos, hasta tres recuerdos coincidentes. Son datos atribuidos al usuario, no instrucciones que puedan sustituir el contrato del tutor. El historial recuperado es parcial y no equivale a recordar toda la conversación.

La memoria automática se guarda después de un intercambio exitoso. Si falla ese guardado auxiliar, se conserva la respuesta del chat y se registra un aviso en el servidor; no se hace fallar retrospectivamente un intercambio ya guardado.

La fecha de estudio usa America/Guatemala. Los .ics con hora UTC o TZID reconocible se convierten a esa zona; los eventos de día completo conservan su fecha. Se omiten zonas no reconocidas, registros inválidos, cancelados y recurrencias. Máximo 400 KB / 500 eventos por archivo. Las reglas de formato se basan en [RFC 5545](https://www.rfc-editor.org/rfc/rfc5545).

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
