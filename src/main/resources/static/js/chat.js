/**
 * ChatView.
 *
 * El historial vive en el servidor (SQLite). Este archivo solo:
 *   - pide la lista de conversaciones y las muestra en el sidebar,
 *   - abre una conversación guardada,
 *   - envía el mensaje nuevo junto con el id de la conversación (null = chat nuevo).
 * El servidor arma el contexto para la IA leyendo su propia base de datos.
 */
(() => {
  const root = document.querySelector('[data-chat-root]');
  if (!root) return;

  const log = root.querySelector('[data-chat-log]');
  const intro = root.querySelector('[data-chat-intro]');
  const form = root.querySelector('[data-composer-form]');
  const textarea = root.querySelector('[data-composer-input]');
  const sendButton = root.querySelector('[data-composer-send]');
  const historyList = document.querySelector('[data-chat-history]');
  const menuButton = document.querySelector('[data-sidebar-toggle]');
  const overlay = document.querySelector('[data-sidebar-overlay]');
  const newChatButton = document.querySelector('[data-new-chat]');

  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

  let currentChatId = null; // null = conversación nueva, todavía sin guardar
  let viewVersion = 0;      // cambia al abrir otro chat; evita pintar respuestas en la vista equivocada
  let sending = false;

  /* ---------------- Sidebar móvil ---------------- */
  const closeSidebar = () => document.body.classList.remove('sidebar-open');
  menuButton?.addEventListener('click', () => document.body.classList.toggle('sidebar-open'));
  overlay?.addEventListener('click', closeSidebar);

  /* ---------------- Composer ---------------- */
  const resizeTextarea = () => {
    textarea.style.height = 'auto';
    textarea.style.height = `${Math.min(textarea.scrollHeight, 192)}px`;
    sendButton.disabled = sending || textarea.value.trim().length === 0;
  };
  textarea?.addEventListener('input', resizeTextarea);
  textarea?.addEventListener('keydown', event => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      form.requestSubmit();
    }
  });

  /* ---------------- API ---------------- */
  async function api(path, options = {}) {
    const headers = { ...(options.headers || {}) };
    if (options.body) headers['Content-Type'] = 'application/json';
    if (options.method && options.method !== 'GET' && csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;

    const response = await fetch(path, { credentials: 'same-origin', ...options, headers });

    // Sesión vencida: Spring redirige al login y fetch lo sigue en silencio.
    if (response.redirected && new URL(response.url).pathname.startsWith('/login')) {
      window.location.assign('/login');
      throw new Error('Tu sesión expiró. Inicia sesión de nuevo.');
    }

    let body = null;
    try { body = await response.json(); } catch { /* sin cuerpo JSON */ }

    if (!response.ok) {
      if (response.status === 403 && !body?.error) {
        throw new Error('Tu sesión expiró o la página caducó. Recarga e inicia sesión de nuevo.');
      }
      throw new Error(body?.error || `Algo salió mal (${response.status}).`);
    }
    return body;
  }

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
    intro?.setAttribute('hidden', '');
    const wrapper = document.createElement('div');
    wrapper.className = 'msg msg--agent msg--typing';
    wrapper.innerHTML = '<div class="msg__bubble"><span class="dot"></span><span class="dot"></span><span class="dot"></span></div>';
    log.appendChild(wrapper);
    scrollToBottom();
    return wrapper;
  }

  /* ---------------- Historial (sidebar) ---------------- */
  // SQLite entrega UTC como "yyyy-MM-dd HH:mm:ss".
  function formatWhen(value) {
    const date = new Date(`${String(value).replace(' ', 'T')}Z`);
    if (Number.isNaN(date.getTime())) return '';
    if (date.toDateString() === new Date().toDateString()) {
      return `Hoy · ${date.toLocaleTimeString('es', { hour: '2-digit', minute: '2-digit' })}`;
    }
    return date.toLocaleDateString('es', { day: 'numeric', month: 'short' });
  }

  function setActive() {
    historyList.querySelectorAll('[data-chat-id]').forEach(button => {
      if (Number(button.dataset.chatId) === currentChatId) button.setAttribute('aria-current', 'true');
      else button.removeAttribute('aria-current');
    });
  }

  function renderHistoryMessage(text) {
    historyList.replaceChildren();
    const item = document.createElement('li');
    item.className = 'sidebar__empty';
    item.textContent = text;
    historyList.appendChild(item);
  }

  function renderHistory(chats) {
    if (!chats.length) {
      renderHistoryMessage('Aún no tienes conversaciones. Escribe tu primer mensaje para empezar.');
      return;
    }
    historyList.replaceChildren();
    for (const chat of chats) {
      const item = document.createElement('li');
      item.className = 'sidebar__item';

      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'sidebar__link';
      button.dataset.chatId = chat.id;

      const title = document.createElement('span');
      title.className = 'sidebar__link-title';
      title.textContent = chat.title;

      const meta = document.createElement('span');
      meta.className = 'sidebar__link-meta';
      meta.textContent = formatWhen(chat.updatedAt);

      button.append(title, meta);
      item.appendChild(button);
      historyList.appendChild(item);
    }
    setActive();
  }

  async function loadHistory() {
    try {
      renderHistory((await api('/chat/api/chats')) || []);
    } catch (err) {
      renderHistoryMessage('No se pudo cargar el historial.');
    }
  }

  async function openChat(id) {
    closeSidebar();
    if (id === currentChatId) return;

    const version = ++viewVersion;
    currentChatId = id;
    setActive();
    log.replaceChildren();
    showTyping();

    try {
      const chat = await api(`/chat/api/chats/${id}`);
      if (version !== viewVersion) return;
      log.replaceChildren();
      for (const message of chat.messages) appendMessage(message.role, message.content);
      scrollToBottom();
    } catch (err) {
      if (version !== viewVersion) return;
      log.replaceChildren();
      appendMessage('assistant', err.message || 'No se pudo abrir la conversación.', { error: true });
    }
  }

  function newChat() {
    viewVersion++;
    currentChatId = null;
    log.replaceChildren();
    intro?.removeAttribute('hidden');
    setActive();
    textarea.focus();
    closeSidebar();
  }

  historyList.addEventListener('click', event => {
    const button = event.target.closest('[data-chat-id]');
    if (button) openChat(Number(button.dataset.chatId));
  });
  newChatButton?.addEventListener('click', newChat);

  /* ---------------- Envío ---------------- */
  form?.addEventListener('submit', async event => {
    event.preventDefault();
    const text = textarea.value.trim();
    if (!text || sending) return;

    log.querySelectorAll('.msg--error').forEach(node => node.remove());
    const version = viewVersion;
    const chatIdAtSend = currentChatId;

    sending = true;
    textarea.disabled = true;
    const userBubble = appendMessage('user', text);
    textarea.value = '';
    resizeTextarea();
    const typing = showTyping();

    try {
      const body = await api('/chat/api/mensajes', {
        method: 'POST',
        body: JSON.stringify({ chatId: chatIdAtSend, content: text }),
      });
      if (!body?.content) throw new Error('La IA no devolvió contenido.');

      if (version === viewVersion) {
        typing.remove();
        appendMessage('assistant', body.content);
        if (currentChatId === null) currentChatId = body.chatId;
      }
      await loadHistory();
    } catch (err) {
      if (version === viewVersion) {
        typing.remove();
        userBubble.remove(); // no se guardó: se quita y se devuelve el texto para reenviarlo
        if (!textarea.value) textarea.value = text;
        appendMessage('assistant', err.message || 'No se pudo contactar al agente. Intenta de nuevo.', { error: true });
      }
    } finally {
      sending = false;
      textarea.disabled = false;
      textarea.focus();
      resizeTextarea();
    }
  });

  loadHistory();
  resizeTextarea();
})();
