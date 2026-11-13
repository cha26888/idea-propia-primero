# Recorrido de demostración

Ejecutar primero ./mvnw test y arrancar con ./mvnw spring-boot:run. Recargar el navegador con Ctrl+Shift+R.

1. En un chat nuevo escribir «Prefiero ejemplos breves de Java». Abrir Perfil y recuerdos: debe aparecer la declaración, editable y con su origen.
2. En el mismo chat escribir «Recuerda que mi proyecto Atlas consiste en un gestor de tareas en Java». Comprobar el recuerdo. Renombrar el chat a «Proyecto Atlas».
3. Abrir otro chat y pedir «Retoma el proyecto Atlas: ¿qué información habíamos guardado?». Debe recibir contexto de ese proyecto y mostrar enlaces al contexto recuperado. Buscar «Atlas» en el historial también debe encontrarlo.
4. Entrar a Mi estudio. Añadir una entrega pendiente y marcarla completada; reabrirla. Importar calendario-demo.ics dos veces: debe haber dos eventos importados, no cuatro.
5. Realizar la práctica diaria: una opción y una razón de al menos 15 caracteres. La explicación aparece tras enviar; recargar conserva el primer intento.
6. Guardar una reflexión en el diario, editarla y recargar. Debe quedar una sola entrada para esa fecha.
7. Crear una guía sobre ecuaciones lineales con tres sesiones de 20 minutos. Abrirla, descargarla y probar Imprimir / PDF. Revisar que incluya práctica y no una tarea del estudiante resuelta.
8. Desde un chat, Compartir y aceptar la confirmación. Abrir el enlace en incógnito: lectura sin cuenta. Añadir otro mensaje en el chat original: no debe aparecer en la copia. Desactivar el enlace: debe devolver 404.
9. Entrar con otra cuenta: perfil, diario, pendientes, guías e historial deben estar vacíos o contener solo sus propios datos. Intentar abrir /chat?chat=ID_AJENO no debe mostrar la conversación.

## Límites a explicar en la entrega

La adaptación parte de preferencias explícitas, no de un diagnóstico automático. La recuperación usa palabras, no embeddings. Los títulos son locales y editables. La práctica diaria utiliza un banco inicial de ocho ejercicios. El calendario admite URL de Canvas y copia manual .ics. La URL se actualiza al usar la app (cada cinco minutos como máximo) o al pulsar Actualizar ahora; no hay avisos por correo. La guía es generada por IA y necesita revisión. Mi estudio usa cinco secciones independientes, calendario mensual y listas paginadas.

## Recorrido del calendario corregido

1. Detener la aplicación y aplicar `calendario-canvas-correccion.patch` sobre la entrega del ecosistema. Ejecutar `./mvnw test` y arrancar de nuevo. Conservar la base de datos: la migración añade columnas y preserva datos.
2. Abrir `/study#tasks` y recargar con Ctrl+Shift+R. Solo debe aparecer el calendario; Gimnasio, Guías, Diario y Perfil se abren desde la navegación lateral.
3. Pulsar **Conectar Canvas**, pegar la URL de **Canvas → Calendario → Calendar Feed** en el formulario de la app y conectar. No pegarla en chats o repositorios. Comprobar dominio y última actualización, sin que el token aparezca en pantalla.
4. Seleccionar **12 de noviembre de 2026** en el calendario: comprobar la entrega de POO según los datos reales del feed. Las clases y actividades deben aparecer bajo Eventos; las tareas de octubre no deben contarse como entregas de ese día.
5. Preguntar en un chat nuevo: «Dime qué pendientes tengo para 12/11/2026». Luego «¿Qué entregas tengo hoy?» y «¿Qué tengo mañana?». Comparar las fechas, no asumir que hoy sigue siendo 12/11. Estas consultas no llaman al modelo.
6. Marcar una entrega como completada, actualizar Canvas y verificar que no se desmarca. Si se cambió la fecha en Canvas, debe actualizarse conservando esa marca. Si el feed la retira, deja de aparecer. Confirmar la hora local si la fecha UTC cruza medianoche.
7. Pulsar **Opciones → Es un evento / Es una entrega** si hace falta. La elección debe conservarse tras sincronizar. El tipo no se deduce por palabras del título.
8. Revisar filtros Vencidas, Completadas y Por clasificar. Se muestran ocho elementos por página. Las importaciones antiguas permanecen por clasificar hasta reconectar o corregirlas.
9. Comprobar **Actualizar ahora** y la actualización automática mientras está visible. Si Canvas falla, debe verse el aviso sin borrar la copia. Probar también un usuario distinto: no debe leer ni modificar ese calendario.
10. Navegar por Gimnasio, Guías, Diario y Perfil. Confirmar que las entradas/formularios anteriores siguen funcionando. Probar ancho móvil y navegación con teclado.

Validado en el entorno de preparación: 27 pruebas aisladas de Java (13 de tutor y 14 de utilidades, agenda, importación y política de URL), SQL real con SQLite de Python, sintaxis JavaScript y correspondencia HTML/selectores. Compilación de código productivo con sustitutos de Spring. Se incluyen además pruebas JUnit/Mockito/JDBC de sincronización, migración y respuesta de agenda sin IA para ejecutar con Maven. El navegador del entorno no pudo arrancar por restricción de sockets; no se afirma validación visual ni acceso real al feed privado del usuario.
