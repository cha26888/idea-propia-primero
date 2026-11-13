/** Markdown y matemáticas del tutor. El contenido original sigue guardado como texto. */
(() => {
  'use strict';

  const escapeHtml = text => String(text).replace(/[&<>"']/g, character => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  })[character]);

  function parserFor(formulas) {
    const placeholder = (token, block) => {
      const index = formulas.push(token) - 1;
      const tag = block ? 'div' : 'span';
      return `<${tag} data-tutor-math="${index}"></${tag}>`;
    };

    // Las fórmulas se reconocen ANTES de Markdown: así no pierde las barras
    // de \(…\) ni interpreta los guiones bajos de LaTeX como cursiva.
    return new window.marked.Marked({
      gfm: true,
      breaks: false,
      async: false,
      renderer: {
        html: ({ text }) => escapeHtml(text),
        image: ({ text }) => escapeHtml(text || 'Imagen'),
      },
      extensions: [
        {
          name: 'tutorMathBlock',
          level: 'block',
          start: source => source.search(/(?:^|\n) {0,3}(?:\\\[|\$\$)/),
          tokenizer(source) {
            const match = /^(?: {0,3}\\\[([\s\S]*?)\\\]| {0,3}\$\$([\s\S]*?)\$\$)[ \t]*(?:\n|$)/.exec(source);
            if (match) return {
              type: 'tutorMathBlock', raw: match[0],
              text: (match[1] ?? match[2]).trim(), display: true,
            };
          },
          renderer: token => placeholder(token, true) + '\n',
        },
        {
          name: 'tutorMathInline',
          level: 'inline',
          start: source => source.search(/\\[([]|\$\$/),
          tokenizer(source) {
            const match = /^(?:\\\(([^\n]*?)\\\)|\\\[([\s\S]*?)\\\]|\$\$([\s\S]*?)\$\$)/.exec(source);
            if (match) return {
              type: 'tutorMathInline', raw: match[0],
              text: (match[1] ?? match[2] ?? match[3]).trim(),
              display: match[1] === undefined,
            };
          },
          renderer: token => placeholder(token, false),
        },
      ],
    });
  }

  function render(element, source) {
    const text = String(source ?? '');
    element.textContent = text;
    element.classList.remove('msg__bubble--formatted');
    // Si no carga alguna dependencia, conserva una respuesta legible y segura.
    if (!window.marked?.Marked || !window.DOMPurify?.isSupported) return;

    try {
      const formulas = [];
      const html = parserFor(formulas).parse(text);
      const fragment = window.DOMPurify.sanitize(html, {
        RETURN_DOM_FRAGMENT: true,
        ALLOWED_TAGS: ['p', 'br', 'strong', 'em', 'del', 'h1', 'h2', 'h3',
          'h4', 'h5', 'h6', 'ul', 'ol', 'li', 'blockquote', 'pre', 'code',
          'hr', 'a', 'table', 'thead', 'tbody', 'tr', 'th', 'td', 'div', 'span'],
        ALLOWED_ATTR: ['href', 'title', 'start', 'align', 'data-tutor-math'],
        ALLOW_DATA_ATTR: false,
        ALLOW_ARIA_ATTR: false,
      });

      // Solo enlaces web; el contenido nunca puede iniciar scripts o cargar imágenes.
      for (const link of fragment.querySelectorAll('a')) {
        const href = link.getAttribute('href') || '';
        if (/^\/chat\?chat=\d+$/.test(href)) continue;
        if (!/^https?:\/\//i.test(href)) link.removeAttribute('href');
        else {
          link.setAttribute('target', '_blank');
          link.setAttribute('rel', 'noopener noreferrer');
        }
      }
      for (const node of fragment.querySelectorAll('[data-tutor-math]')) {
        const formula = formulas[Number(node.getAttribute('data-tutor-math'))];
        node.removeAttribute('data-tutor-math');
        if (!formula) continue;
        node.textContent = formula.raw.trim();
        if (!window.katex?.render) continue;
        try {
          window.katex.render(formula.text, node, {
            displayMode: formula.display,
            output: 'htmlAndMathml',
            throwOnError: true,
            trust: false,
            strict: 'ignore',
            maxExpand: 1000,
            maxSize: 20,
            macros: {},
          });
        } catch {
          // Una fórmula incompleta no debe ocultar el resto de la explicación.
          node.textContent = formula.raw.trim();
        }
      }
      element.replaceChildren(fragment);
      element.classList.add('msg__bubble--formatted');
    } catch {
      element.textContent = text;
    }
  }

  window.ChatFormatting = Object.freeze({ render });
})();
