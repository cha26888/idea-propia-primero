(() => {
  const root = document.querySelector('[data-chat-root]');
  if (!root) return;

  const log = root.querySelector('[data-chat-log]');
  const intro = root.querySelector('[data-chat-intro]');
  const form = root.querySelector('[data-composer-form]');
  const textarea = root.querySelector('[data-composer-input]');
  const sendButton = root.querySelector('[data-composer-send]');
  const menuButton = document.querySelector('[data-sidebar-toggle]');
  const overlay = document.querySelector('[data-sidebar-overlay]');
  const newChatButton = document.querySelector('[data-new-chat]');

  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

  /* ---------------- Sidebar móvil ---------------- */
  const closeSidebar = () => document.body.classList.remove('sidebar-open');
  menuButton?.addEventListener('click', () => document.body.classList.toggle('sidebar-open'));
  overlay?.addEventListener('click', closeSidebar);

  /* ---------------- Autoresize del textarea ---------------- */
  const resizeTextarea = () => {
    textarea.style.height = 'auto';
    textarea.style.height = `${Math.min(textarea.scrollHeight, 192)}px`;
    sendButton.disabled = textarea.value.trim().length === 0;
  };
  textarea?.addEventListener('input', resizeTextarea);
  textarea?.addEventListener('keydown', event => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      form.requestSubmit();
    }
  });

  /* ---------------- Render de mensajes ---------------- */
  const scrollToBottom = () => {
    root.querySelector('[data-chat-scroll]').scrollTop = 1e9;
  };

  function appendMessage(role, content, { error = false } = {}) {
    intro?.setAttribute('hidden', '');
    const wrapper = document.createElement('div');
    wrapper.className = `msg msg--${role === 'user' ? 'user' : 'agent'}${error ? ' msg--error' : ''}`;

    const meta = document.createElement('p');
    meta.className = 'msg__meta';
    meta.textContent = role === 'user' ? 'Tú' : 'Agente';
    wrapper.appendChild(meta);

    const bubble = document.createElement('div');
    bubble.className = 'msg__bubble';
    bubble.textContent = content;
    wrapper.appendChild(bubble);

    log.appendChild(wrapper);
    scrollToBottom();
    return wrapper;
  }

  function showTyping() {
    const wrapper = document.createElement('div');
    wrapper.className = 'msg msg--agent msg--typing';
    wrapper.innerHTML = '<div class="msg__bubble"><span class="dot"></span><span class="dot"></span><span class="dot"></span></div>';
    log.appendChild(wrapper);
    scrollToBottom();
    return wrapper;
  }

  /* ---------------- Historial (en memoria) ---------------- */
  let history = []; // [{role:'user'|'assistant', content:string}, ...]

  function trimHistory() {
    // ChatService acepta como máximo 10 pares previos + la pregunta nueva (21 mensajes).
    while (history.length > 20) history.splice(0, 2);
  }

  /* ---------------- Llamada al backend ---------------- */
  async function askAgent() {
    const headers = { 'Content-Type': 'application/json' };
    if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;

    const response = await fetch('/chat/api/mensajes', {
      method: 'POST',
      credentials: 'same-origin',
      headers,
      body: JSON.stringify({ historial: history }),
    });

    let body = null;
    try { body = await response.json(); } catch { /* respuesta sin cuerpo JSON */ }

    if (!response.ok) {
      const message = body?.error || `La IA respondió con un error (${response.status}).`;
      throw new Error(message);
    }
    if (!body?.content) throw new Error('La IA no devolvió contenido.');
    return body.content;
  }

  /* ---------------- Envío ---------------- */
  form?.addEventListener('submit', async event => {
    event.preventDefault();
    const text = textarea.value.trim();
    if (!text || sendButton.disabled) return;

    history.push({ role: 'user', content: text });
    appendMessage('user', text);
    textarea.value = '';
    resizeTextarea();
    sendButton.disabled = true;
    textarea.disabled = true;

    const typing = showTyping();
    try {
      const reply = await askAgent();
      typing.remove();
      appendMessage('assistant', reply);
      history.push({ role: 'assistant', content: reply });
      trimHistory();
    } catch (err) {
      typing.remove();
      history.pop(); // no dejar una pregunta del usuario "colgada" sin respuesta en el historial
      appendMessage('assistant', err.message || 'No se pudo contactar al agente. Intenta de nuevo.', { error: true });
    } finally {
      textarea.disabled = false;
      textarea.focus();
      resizeTextarea();
    }
  });

  /* ---------------- Nuevo chat ---------------- */
  newChatButton?.addEventListener('click', () => {
    log.innerHTML = '';
    intro?.removeAttribute('hidden');
    history = [];
    textarea.value = '';
    resizeTextarea();
    textarea.focus();
    closeSidebar();
  });

  resizeTextarea();
})();
