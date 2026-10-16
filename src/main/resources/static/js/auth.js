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
    registerForm.querySelectorAll('[data-error-for]').forEach(node => { node.textContent = ''; });
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
      for (const field of ['username', 'email', 'password']) {
        const error = result.errors && result.errors[field];
        if (typeof error !== 'string') continue;
        registerForm.querySelector(`[data-error-for="${field}"]`).textContent = error;
        registerForm.elements.namedItem(field).setAttribute('aria-invalid', 'true');
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
