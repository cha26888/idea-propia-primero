# Tutor: modos y adaptación local

Estado: implementación basada en el ZIP adjunto; no se ha publicado en GitHub. El usuario confirmó BUILD SUCCESS de Maven para la entrega inicial y una respuesta real de Groq. Esta corrección de autonomía requiere volver a ejecutar las pruebas y validar el comportamiento con el modelo.

## Comportamiento

| Modo de API | Etiqueta | Ayuda |
| --- | --- | --- |
| UNDERSTAND | Comprender | Explicaciones directas y ejemplos distintos resueltos; el estudiante aplica el método a su ejercicio. |
| PRACTICE | Practicar | Primera idea del estudiante, pistas progresivas y revisión del intento. Es el valor inicial. |
| CHALLENGE | Estricto | Estrategia propia antes de pistas del ejercicio, ayuda mínima y revisión del razonamiento. |

«Tu idea primero» es la regla común: ningún modo autoriza a completar el ejercicio o la entrega del estudiante. La ayuda cambia de intensidad y forma. Las definiciones están disponibles en todos los modos. Un cálculo o un fragmento de código en una explicación no son motivos para bloquear la ayuda.

Se pueden resolver ejemplos inventados o análogos con otros datos y otro resultado. Si el estudiante trae una tarea concreta, la IA explica el método con otro ejemplo y deja que él ejecute su solución. No se permite completar la misma tarea por fragmentos, cambiarle solo nombres para llamarla ejemplo, ni desbloquear la respuesta por insistencia, bloqueo, cambio de modo o abandono del intento. Una respuesta anterior del tutor que haya incumplido esta regla no se convierte en autorización.

Revisar una solución propia permite confirmar un resultado correcto, explicar un error y orientar su corrección. No implica reemplazar un intento incompleto o incorrecto por una respuesta terminada. Ante falta de base se enseña el prerrequisito o un ejemplo distinto; no se exige pensar indefinidamente sin ayuda.

Estricto conserva el valor CHALLENGE de API y base de datos, y el archivo modo-reto.md. La etiqueta cambia sin migrar conversaciones. Estas reglas son decisiones del producto para fomentar autonomía; no son una garantía científica de aprendizaje ni un bloqueo determinista del modelo.

### Corrección de la apertura sin intento

La prueba real mostró una regresión: el tutor preguntaba qué operación hacer y contestaba esa pregunta en un paréntesis («restar 2x»). Evitar solo el resultado final no preservaba la decisión del estudiante.

TutorOpening devuelve una apertura controlada en Practicar/Estricto cuando el mensaje actual declara explícitamente que no ha intentado una tarea. Esa ruta no consulta al modelo: Practicar orienta a identificar objetivo/datos y Estricto pide una primera idea sin pista. La detección es conservadora y textual; no cubre todas las paráfrasis. Consultas conceptuales o intentos visibles continúan hacia la IA, igual que Comprender. No se usan mensajes previos de la IA como intentos del usuario.

Las instrucciones del modo se ubican al final del mensaje de sistema y prohíben incluir en una pregunta la decisión que se pide tomar. Comprender enseña con otro ejemplo y no agrega el primer paso de la tarea original. Las nuevas respuestas muestran el modo confirmado por el servidor, por ejemplo «Agente · Estricto». Los mensajes antiguos no conservan un modo individual, por lo que al reabrir no se les atribuye uno retrospectivamente.

La garantía de la apertura controlada se limita a las entradas que reconoce TutorOpening. Las respuestas restantes dependen del modelo y requieren validación real; no se declara resuelto el cumplimiento general por haber pasado las pruebas locales.

La adaptación es local al contexto de la conversación: el prompt distingue concepto, intento, error y asistencia recibida. Una señal conservadora reconoce expresiones cortas de bloqueo; si se repiten, pide cambiar la estrategia. Esta señal no mide conocimiento ni clasifica estudiantes. La interpretación pedagógica sigue dependiendo del modelo y requiere las pruebas manuales.

Todavía no hay perfil persistente de aprendizaje, recuperación entre conversaciones ni medición de dominio. El único dato nuevo guardado es el modo del chat.

## Contrato para frontend y backend

- POST /chat/api/mensajes acepta chatId, content y mode (opcional). Si se omite mode, conserva el del chat existente; un chat nuevo usa PRACTICE.
- La respuesta devuelve chatId, content y mode.
- GET /chat/api/chats/{id} incluye mode.
- POST /chat/api/chats/{id}/mode acepta un objeto con mode y devuelve el modo guardado. Requiere sesión, propiedad del chat y la protección CSRF existente. No consulta la IA.
- Un valor desconocido recibe 400; una conversación ajena recibe 404.
- Un fallo de generación no guarda el par ni el modo enviado junto a ese mensaje. Un cambio explícito previo del selector sí permanece guardado.

SQLite recibe una migración aditiva de chats.tutor_mode, con PRACTICE como valor de las conversaciones antiguas. Al reiniciar se comprueba la columna; no se borran cuentas ni mensajes.

## Contexto y costo

Se solicita el primer par y hasta nueve pares recientes sin duplicados. ChatService selecciona pares completos, priorizando los últimos, hasta 32,000 caracteres de conversación más el prompt. No corta respuestas a 600 caracteres ni agrega instrucciones al texto del usuario.

Este límite es de caracteres, no una medición exacta de tokens. La ventana puede costar más que la anterior y debe medirse con el proveedor. En chats extensos puede faltar información intermedia; el tutor debe pedirla cuando la necesite. No es memoria completa ni un resumen persistente.

Cada turno realiza como máximo una generación; una apertura controlada no consume tokens. Se eliminó el reintento por detectar cálculos, que también afectaba ejemplos válidos y no detectaba una pista como «restar 2x». Las pruebas de formato no prueban el cumplimiento pedagógico del modelo.

## Fundamentación

La retirada gradual de ayuda y el ajuste al conocimiento previo orientan los modos, pero no validan por sí solos esta implementación:

- [Renkl et al., 2002: transición de ejemplos a resolución](https://doi.org/10.1080/00220970209599510).
- [Sinha y Kapur, 2021: intento seguido de instrucción y sus condiciones](https://doi.org/10.3102/00346543211019105).
- [Yang et al., 2021: práctica de recuperación](https://doi.org/10.1037/bul0000309).
- [Clinton-Lisell y Litzinger, 2024: límites del emparejamiento por estilos](https://doi.org/10.3389/fpsyg.2024.1428732).
- [Bastani et al., 2025: desempeño con IA y aprendizaje posterior](https://doi.org/10.1073/pnas.2422633122).

Los nombres de los modos, el límite de contexto y la señal de bloqueo son decisiones de ingeniería. No se presentan como parámetros científicamente validados.

## Verificación realizada

| Comprobación | Resultado y límite |
| --- | --- |
| Núcleo Java | Trece métodos de prueba ejecutados con Java 21 y sustitutos temporales de Spring/JUnit. Pasaron. Incluyen apertura sin llamada al proveedor, excepciones conceptuales, revisión de intentos y presupuesto compartido con memoria. No equivale a ejecutar Maven/JUnit real. |
| Compilación aislada | Núcleo, repositorio y controlador compilaron con sustitutos temporales de las APIs ausentes. |
| SQL | Consultas del código comprobadas en SQLite de Python: base nueva/anterior, migración, modos válidos, selección de contexto, predicado de propietario y rollback. No prueba el controlador JDBC. |
| JavaScript | Sintaxis verificada con Node. |
| Diff | Sin errores de espacios; el parche se verifica contra una extracción limpia del ZIP. |
| Suite completa | BUILD SUCCESS de la entrega inicial confirmado por el usuario. Repetir tras esta corrección; este entorno solo dispone de la comprobación aislada. |
| Navegador | Pendiente: Chromium no pudo iniciar por restricciones del entorno. |
| Proveedor y aprendizaje | El usuario recibió una explicación de Groq. Sigue pendiente evaluar los límites de ayuda de esta corrección y realizar un estudio de aprendizaje. |

Las pruebas del tutor cubren skill/servicio, repositorio y controlador. La entrega del espacio de estudio añade pruebas de memoria, búsqueda, calendario y enlaces. Las pruebas unitarias de controlador invocan métodos directamente; AuthFlowTest incluye comprobaciones HTTP/CSRF con la aplicación arrancada.

## Cierre manual

| Caso | Resultado esperado |
| --- | --- |
| Definición en los tres modos | Explica sin exigir un intento. |
| Definición con números o código | No bloquea por el formato. |
| Ejercicio sin intento en Practicar | Pista concreta y siguiente acción posible. |
| Dos mensajes de bloqueo | Cambia de apoyo; no repite una negativa. |
| Intento parcialmente correcto | Conserva lo acertado y aborda el error relevante. |
| Resultado correcto con razón errónea | Comprueba la justificación; no declara dominio. |
| Evidencia de autonomía | Reduce ayuda sobre ese tema. |
| Cambio de tema/modo | Atiende la nueva intención y el modo visible. |
| Reapertura y chat largo | Conserva modo y contexto disponible; no inventa lo omitido. |
| Fallo de IA | No deja mensajes huérfanos; permite reintentar. |
| Pedir ayuda en Estricto | Pide una estrategia propia, ofrece ayuda ante bloqueo y revisa sin completar el ejercicio. |
| Dos usuarios | No permite leer ni modificar conversaciones ajenas. |

Ejecutar con Java 25: ./mvnw test (Windows: .\mvnw.cmd test). Después iniciar la aplicación y repetir los casos con el modelo configurado, usando paráfrasis. Registrar ejemplos fallidos antes de dar el bloque por cerrado.

Para comprobar específicamente la autonomía, seguir [la matriz de evaluación](../testing/tutoria-autonomia.md). Las pruebas de ensamblaje de prompts no sustituyen estas conversaciones con el modelo real.
