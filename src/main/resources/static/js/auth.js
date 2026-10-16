/* ==========================================
   MOSTRAR Y OCULTAR CONTRASEÑA
========================================== */

document.querySelectorAll('[data-toggle-password]').forEach(button => {

  button.addEventListener('click', () => {

    // Obtener el campo de contraseña
    const input = document.getElementById(
      button.getAttribute('aria-controls')
    );

    if (!input) return;

    // Comprobar si la contraseña está oculta
    const show = input.type === 'password';

    // Mostrar u ocultar contraseña
    input.type = show ? 'text' : 'password';

    // Actualizar el estado del botón
    button.setAttribute('aria-pressed', String(show));

    // Actualizar el texto accesible
    button.setAttribute(
      'aria-label',
      show ? 'Ocultar contraseña' : 'Mostrar contraseña'
    );

    // Actualizar el texto emergente
    button.setAttribute(
      'title',
      show ? 'Ocultar contraseña' : 'Mostrar contraseña'
    );

  });

});

const registerForm = document.querySelector('[data-register]');
if (registerForm && window.fetch) {
  let submitting = false;
  registerForm.addEventListener('submit', async event => {
    event.preventDefault();
    if (submitting) return;
    submitting = true;
    const submit = registerForm.querySelector('[data-submit]');
    const alert = registerForm.querySelector('[data-form-alert]');
    const message = registerForm.querySelector('[data-form-message]');
    const showMessage = text => {
      message.textContent = text;
      alert.hidden = false;
      alert.focus();
    };
    registerForm.querySelectorAll('[data-server-error]').forEach(node => node.remove());
    // Limpiar y ocultar errores anteriores
    registerForm.querySelectorAll('[data-error-for]').forEach(node => {

        node.textContent = '';

        node.hidden = true;

    });
    registerForm.querySelectorAll('[aria-invalid]').forEach(node => node.setAttribute('aria-invalid', 'false'));
    alert.hidden = true;
    submit.disabled = true;
    submit.textContent = 'Creando cuenta…';
    registerForm.setAttribute('aria-busy', 'true');
    const abort = new AbortController();
    const timeout = setTimeout(() => abort.abort(), 20000);
    try {
      const response = await fetch(registerForm.dataset.submitUrl, {
        method: 'POST', body: new FormData(registerForm),
        headers: { Accept: 'application/json' }, credentials: 'same-origin', signal: abort.signal
      });
      if (response.status === 403) {
        showMessage('La sesión del formulario venció. Recarga la página y vuelve a intentarlo; al recargar tendrás que escribir la contraseña otra vez.');
        return;
      }
      if (!(response.headers.get('content-type') || '').includes('application/json')) throw new Error('Unexpected response');
      const result = await response.json();
      if (response.ok && result.success === true) {
        registerForm.reset();
        window.location.assign(registerForm.dataset.loginUrl);
        return;
      }
      if (response.status !== 400) throw new Error('Server error');
      // Mostrar errores específicos debajo de cada campo
    for (const field of ['username', 'email', 'password']) {

        const error = result.errors && result.errors[field];

        if (typeof error !== 'string') continue;

        // Buscar el campo correspondiente
        const input = registerForm.elements.namedItem(field);

        // Buscar el mensaje de error
        const errorElement = registerForm.querySelector(
            `[data-error-for="${field}"]`
        );

        // Marcar el campo como inválido
        if (input) {
            input.setAttribute('aria-invalid', 'true');
        }

        // Mostrar el mensaje específico
        if (errorElement) {
            errorElement.textContent = '⚠️ ' + error;
            errorElement.hidden = false;
        }

    }
      showMessage(result.message || 'Revisa tus datos e inténtalo nuevamente.');
    } catch (error) {
      showMessage('No pudimos confirmar el registro. Tus datos siguen aquí. Comprueba tu conexión e inténtalo de nuevo; si la cuenta ya se creó, inicia sesión.');
    } finally {
      clearTimeout(timeout);
      submitting = false;
      submit.disabled = false;
      submit.textContent = 'Crear cuenta';
      registerForm.removeAttribute('aria-busy');
    }
  });
}

/* ==========================================
   VALIDACIONES DEL FORMULARIO LOGIN
========================================== */

const loginForm = document.querySelector(
    'form[action$="/login"]'
);

if (loginForm) {

    const usernameInput = loginForm.querySelector(
        '#username'
    );

    const passwordInput = loginForm.querySelector(
        '#password'
    );

    const usernameError = document.getElementById(
        'username-error'
    );

    const passwordError = document.getElementById(
        'password-error'
    );

    // Mostrar errores debajo de cada campo
    function showLoginError(input, errorElement, message) {

        if (!input || !errorElement) return;

        errorElement.textContent = message;
        errorElement.hidden = false;

        input.setAttribute('aria-invalid', 'true');
    }

    // Eliminar errores cuando se corrigen
    function clearLoginError(input, errorElement) {

        if (!input || !errorElement) return;

        errorElement.textContent = '';
        errorElement.hidden = true;

        input.setAttribute('aria-invalid', 'false');
    }

    // Validar usuario o correo
    function validateUsername() {

        if (!usernameInput) return true;

        const username = usernameInput.value.trim();

        if (username.length === 0) {

            showLoginError(
                usernameInput,
                usernameError,
                '⚠️ Ingresa tu usuario o correo electrónico.'
            );

            return false;
        }

        clearLoginError(usernameInput, usernameError);

        return true;
    }

    // Validar contraseña
    function validatePassword() {

        if (!passwordInput) return true;

        if (passwordInput.value.length === 0) {

            showLoginError(
                passwordInput,
                passwordError,
                '⚠️ Ingresa tu contraseña.'
            );

            return false;
        }

        clearLoginError(passwordInput, passwordError);

        return true;
    }

    // Validar al enviar el formulario
    loginForm.addEventListener('submit', function(event) {

        const validUsername = validateUsername();
        const validPassword = validatePassword();

        if (!validUsername || !validPassword) {

            event.preventDefault();

            const firstInvalid = loginForm.querySelector(
                '[aria-invalid="true"]'
            );

            if (firstInvalid) {
                firstInvalid.focus();
            }
        }

    });

    // Eliminar errores mientras el usuario escribe
    if (usernameInput) {

        usernameInput.addEventListener('input', () => {

            if (usernameInput.value.trim().length > 0) {
                clearLoginError(usernameInput, usernameError);
            }

        });
    }

    if (passwordInput) {

        passwordInput.addEventListener('input', () => {

            if (passwordInput.value.length > 0) {
                clearLoginError(passwordInput, passwordError);
            }

        });
    }

}

/* ==========================================
   CONSERVAR USUARIO DESPUÉS DE LOGIN FALLIDO
========================================== */

const loginUsername = document.querySelector(
    '#username'
);

const loginFormPersistence = document.querySelector(
    'form[action$="/login"]'
);

if (loginUsername && loginFormPersistence) {

    const storageKey = 'idea-propia-login-username';

    // Recuperar usuario cuando falla el login
    const params = new URLSearchParams(window.location.search);

    if (params.has('error')) {

        const savedUsername = sessionStorage.getItem(storageKey);

        if (savedUsername) {
            loginUsername.value = savedUsername;
        }

    } else {

        // Eliminar datos temporales cuando no hay error
        sessionStorage.removeItem(storageKey);

    }

    // Guardar usuario antes de enviar el formulario
    loginFormPersistence.addEventListener('submit', (event) => {

        if (event.defaultPrevented) {
            return;
        }

        sessionStorage.setItem(
            storageKey,
            loginUsername.value.trim()
        );

    });

}