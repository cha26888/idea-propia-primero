# Interfaz compartida

## Archivos y reglas

- `static/css/theme.css`: fuente única de colores, espaciado, tipografía, radios y ancho del formulario. Modificar variables `--color-*`, `--space-*`, etc. para todo el proyecto.
- `static/css/components.css`: clases reutilizables `.card`, `.title`, `.field`, `.input`, `.button`, `.alert` y variantes. Evitar estilos inline y colores independientes por página.
- `templates/fragments/ui.html`: fragmentos de cabecera, marca, campo, contraseña y logout.
- `static/js/auth.js`: mostrar contraseña y envío progresivo del registro.
- `static/css/auth.css`: archivo anterior, ahora sin referencias en estas pantallas. Puede eliminarse después de confirmar que ninguna vista adicional lo utiliza.

Fragmentos: `head(title)`, `brand`, `field(id,label,type,value,autocomplete,help,error)`, `password(autocomplete,help,error)`, `logout`. El fragmento de contraseña se utiliza una sola vez por página y nunca recibe un valor de contraseña. El campo compartido está diseñado para usuario y correo; ampliar sus parámetros si aparece otro tipo de campo con límites diferentes.

Ejemplo de cabecera:

```html
<head th:replace="~{fragments/ui :: head('Mi pantalla')}"></head>
```

Botón:

```html
<button class="button button--primary" type="submit">Guardar</button>
```

Título: usar un solo h1 con clase `title` y enlazar la sección mediante aria-labelledby. Los errores tienen texto explícito, borde, aria-invalid, aria-describedby y un resumen con role=alert. No depender únicamente del color. Los enlaces y botones tienen foco visible y los campos etiquetas.

## Registro sin recarga

El formulario se envía a POST /register/submit con Accept: application/json, incluye el token CSRF generado por Thymeleaf y devuelve únicamente estado y mensajes. Ante error se conserva la contraseña en el input del navegador; nunca en localStorage, sessionStorage, cookies, logs o JSON de respuesta. Al completar el registro se limpia el formulario y se redirige al login.

400: errores de campos o duplicado. 403: aviso de sesión/CSRF y necesidad de recargar. Fallo de red, timeout o error interno: aviso sin borrar los campos. Se desactiva el botón mientras está enviando para evitar dobles clics. No se reintenta automáticamente una petición que podría haber creado ya la cuenta.

Sin JavaScript funciona el formulario HTML convencional y se vacía la contraseña al renderizar errores. Se mantiene la validación del servidor como autoridad. El registro usa novalidate para mostrar los mensajes del servidor de forma consistente. El login conserva su flujo convencional de Spring Security y muestra el error con el mismo componente visual.

La consulta previa permite identificar el campo duplicado; las restricciones UNIQUE de SQLite siguen evitando carreras. Si otra petición inserta el mismo dato simultáneamente, el error global también se muestra al usuario.

## Verificación pendiente en Java 25

Ejecutar mvnw.cmd test. Las pruebas adicionales comprueban JSON de alta, duplicado por usuario y por correo, longitud y ausencia de contraseña en respuestas. No se ejecutaron aquí por falta de Java 25 y descargas Maven.

Comprobar manualmente: correo repetido conserva contraseña y muestra error bajo correo; usuario repetido muestra error bajo usuario; corregir permite crear la cuenta; botón Mostrar/Ocultar; navegación con Tab y móvil; error de conexión; registro sin JavaScript; login y logout; mismo tema en inicio, bienvenida y auxiliar. Reiniciar Spring Boot tras copiar los archivos y recargar con Ctrl+F5.

La ruta HTML POST /register se mantiene como alternativa sin JavaScript. La ruta POST /register/submit devuelve JSON; ambas llaman a la misma validación y servicio. No se selecciona el método según Accept.
