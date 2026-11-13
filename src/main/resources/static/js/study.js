(() => {
  'use strict';
  const status=document.querySelector('#study-status');
  const forms=Object.fromEntries(['task','calendar','guide','journal','note'].map(n=>[n,document.querySelector(`#${n}-form`)]));
  const csrf=document.querySelector('meta[name="_csrf"]')?.content;
  const csrfHeader=document.querySelector('meta[name="_csrf_header"]')?.content;
  let state=null, initialized=false;
  const el=(tag,text,className)=>{const node=document.createElement(tag);if(text!==undefined)node.textContent=text;if(className)node.className=className;return node;};
  const say=(text,error=false)=>{status.textContent=text;status.classList.toggle('is-error',error);};
  async function api(path,method='GET',data) {
    const headers={};if(data!==undefined)headers['Content-Type']='application/json';if(method!=='GET'&&csrf&&csrfHeader)headers[csrfHeader]=csrf;
    const response=await fetch(path,{method,headers,credentials:'same-origin',body:data===undefined?undefined:JSON.stringify(data)});
    if(response.redirected&&new URL(response.url).pathname.startsWith('/login')){location.assign('/login');throw new Error('Tu sesión expiró.');}
    const body=await response.json().catch(()=>null);if(!response.ok)throw new Error(body?.error||`No se pudo completar la operación (${response.status}).`);return body;
  }
  async function run(action,button,message='Guardado.') {
    if(button)button.disabled=true;say('Procesando…');
    try {const result=await action();await load();say(typeof message==='function'?message(result):message);}
    catch(error){say(error.message||'No se pudo completar la operación.',true);}
    finally{if(button)button.disabled=false;}
  }
  function action(label,fn){const b=el('button',label);b.type='button';b.addEventListener('click',()=>fn(b));return b;}
  function empty(container,text){if(!container.children.length)container.append(el('p',text,'study-muted'));}
  function source(id){const a=el('a','Abrir chat de origen');a.href=`/chat?chat=${id}`;return a;}
  function remove(kind,id,button){if(confirm('¿Eliminar este elemento?'))run(()=>api(`/study/api/${kind}/${id}`,'DELETE'),button,'Elemento eliminado.');}
  function renderNotes(){
    const list=document.querySelector('#note-list');list.replaceChildren();
    for(const note of state.notes){const item=el('article',undefined,'study-item');item.append(el('strong',note.kind==='PROFILE'?'Perfil de estudio':'Proyecto'),el('p',note.content));
      const actions=el('div',undefined,'study-actions');if(note.sourceChatId)actions.append(source(note.sourceChatId));
      actions.append(action('Editar',button=>{const content=prompt('Edita este recuerdo:',note.content);if(content!==null&&content.trim())run(()=>api(`/study/api/notes/${note.id}`,'PUT',{content}),button);}),action('Eliminar',button=>remove('notes',note.id,button)));item.append(actions);list.append(item);
    }empty(list,'Todavía no hay recuerdos guardados.');
  }
  function renderTasks(){const list=document.querySelector('#task-list');list.replaceChildren();
    for(const task of state.tasks){const item=el('article',undefined,`study-item${task.done?' study-done':''}`);item.append(el('h3',task.title),el('p',`${task.course||'Sin curso'} · ${task.dueDate}${task.sourceUid?' · Importado':''}`,!task.done&&task.dueDate<state.today?'study-overdue':'study-muted'));
      const actions=el('div',undefined,'study-actions');actions.append(action(task.done?'Marcar pendiente':'Ya lo terminé',button=>run(()=>api(`/study/api/tasks/${task.id}`,'PUT',{done:!task.done}),button)),action('Eliminar',button=>remove('tasks',task.id,button)));item.append(actions);list.append(item);
    }empty(list,'No tienes pendientes registrados.');
  }
  function fillJournal(entry){for(const field of ['day','learned','difficult','nextStep'])forms.journal.elements[field].value=entry[field]||'';}
  function renderJournal(){const list=document.querySelector('#journal-list');list.replaceChildren();
    for(const entry of state.journal){const item=el('article',undefined,'study-item');item.append(el('h3',entry.day),el('p',entry.learned));if(entry.difficult)item.append(el('p',`Dificultad: ${entry.difficult}`));if(entry.nextStep)item.append(el('p',`Siguiente paso: ${entry.nextStep}`));
      const actions=el('div',undefined,'study-actions');actions.append(action('Editar',()=>{fillJournal(entry);forms.journal.scrollIntoView({behavior:'smooth'});}),action('Eliminar',button=>remove('journal',entry.id,button)));item.append(actions);list.append(item);
    }empty(list,'Escribe tu primera reflexión.');
  }
  function download(guide){const blob=new Blob([`# ${guide.title}\n\n${guide.content}`],{type:'text/markdown;charset=utf-8'});const url=URL.createObjectURL(blob);const a=el('a');a.href=url;a.download=(guide.title.replace(/[^\p{L}\p{N} _-]/gu,'').slice(0,60)||'guia')+'.md';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);}
  function renderGuides(){const list=document.querySelector('#guide-list');list.replaceChildren();
    for(const guide of state.guides){const item=el('article',undefined,'study-item');const details=el('details',undefined,'study-guide');details.append(el('summary',guide.title));const body=el('div',undefined,'msg__bubble');if(window.ChatFormatting)window.ChatFormatting.render(body,guide.content);else body.textContent=guide.content;details.append(body);
      const actions=el('div',undefined,'study-actions');if(guide.sourceChatId)actions.append(source(guide.sourceChatId));actions.append(action('Descargar',()=>download(guide)),action('Imprimir / PDF',()=>{details.open=true;item.classList.add('print-target');document.body.classList.add('printing-guide');window.print();document.body.classList.remove('printing-guide');item.classList.remove('print-target');}),action('Eliminar',button=>remove('guides',guide.id,button)));item.append(details,actions);list.append(item);
    }empty(list,'Todavía no has generado guías.');
  }
  function renderDaily(){const daily=state.daily;const host=document.querySelector('#daily-content');host.replaceChildren();host.append(el('p',`${daily.day} · ${daily.streak} días de práctica consecutiva`,'study-muted'),el('h3',daily.exercise.skill),el('p',daily.exercise.question));
    if(daily.attempt){host.append(el('p',`Tu elección: ${daily.exercise.options[daily.attempt.choice]}`),el('p',`Tu razonamiento: ${daily.attempt.reason}`),el('p',daily.attempt.feedback,'study-feedback'),el('p','Práctica de hoy registrada. Mañana habrá otra. La explicación no evalúa automáticamente la calidad de tu razonamiento.','study-muted'));return;}
    const form=el('form',undefined,'study-form');const options=el('fieldset',undefined,'daily-options');options.append(el('legend','Elige una opción'));
    daily.exercise.options.forEach((text,index)=>{const label=el('label');const input=el('input');input.type='radio';input.name='choice';input.value=String(index);input.required=true;label.append(input,document.createTextNode(text));options.append(label);});
    const label=el('label','Explica por qué elegiste esa respuesta');const reason=el('textarea');reason.name='reason';reason.required=true;reason.minLength=15;reason.maxLength=1500;reason.rows=3;label.append(reason);const button=el('button','Registrar intento y ver explicación','button button--primary');form.append(options,label,button);
    form.addEventListener('submit',event=>{event.preventDefault();const data=new FormData(form);run(()=>api('/study/api/daily','POST',{exerciseId:daily.exercise.id,choice:Number(data.get('choice')),reason:data.get('reason')}),button,'Práctica registrada.');});host.append(form);
  }
  async function load(){state=await api('/study/api/dashboard');renderNotes();renderTasks();renderJournal();renderGuides();renderDaily();if(!initialized){forms.task.elements.dueDate.value=state.today;fillJournal(state.journal.find(e=>e.day===state.today)||{day:state.today});initialized=true;}}
  function bind(name,path,prepare,after,message){forms[name].addEventListener('submit',event=>{event.preventDefault();const form=event.currentTarget;run(async()=>{const data=await prepare(Object.fromEntries(new FormData(form)),form);const result=await api(path,'POST',data);if(after)after(form);return result;},event.submitter,message);});}
  bind('note','/study/api/notes',data=>data,form=>form.reset(),'Recuerdo guardado.');
  bind('task','/study/api/tasks',data=>data,form=>{form.elements.title.value='';},'Pendiente añadido.');
  bind('journal','/study/api/journal',data=>data,null,'Reflexión guardada.');
  bind('guide','/study/api/guides',data=>({...data,sourceChatId:data.sourceChatId?Number(data.sourceChatId):null}),null,'Guía generada y guardada.');
  bind('calendar','/study/api/calendar',async(data,form)=>{const file=form.elements.calendar.files[0];if(!file||file.size>400000)throw new Error('Selecciona un archivo .ics de hasta 400 KB.');return {content:await file.text(),course:data.course};},null,result=>`${result.imported} eventos importados o actualizados; ${result.skipped} omitidos. ${result.message}`);
  forms.journal.elements.day.addEventListener('change',()=>fillJournal(state?.journal.find(e=>e.day===forms.journal.elements.day.value)||{day:forms.journal.elements.day.value}));
  (async()=>{try{await load();const chats=await api('/chat/api/chats');const select=document.querySelector('#guide-chat');for(const chat of chats){const option=el('option',chat.title);option.value=String(chat.id);select.append(option);}const selected=new URLSearchParams(location.search).get('chat');if(selected&&chats.some(c=>String(c.id)===selected)){select.value=selected;forms.guide.elements.topic.value=chats.find(c=>String(c.id)===selected).title;}say('Tu espacio está actualizado.');}catch(error){say(error.message,true);}})();
})();
