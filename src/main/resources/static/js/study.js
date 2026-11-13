(() => {
  'use strict';
  const status=document.querySelector('#study-status');
  const forms=Object.fromEntries(['task','calendar','feed','guide','journal','note'].map(n=>[n,document.querySelector(`#${n}-form`)]));
  const csrf=document.querySelector('meta[name="_csrf"]')?.content;
  const csrfHeader=document.querySelector('meta[name="_csrf_header"]')?.content;
  let state=null, initialized=false, selectedDay=null, month=null, taskPage=0, loadVersion=0;
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
  const parseDay=day=>new Date(`${day}T12:00:00`);
  const dayKey=date=>`${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
  const plusDays=(day,n)=>{const d=parseDay(day);d.setDate(d.getDate()+n);return dayKey(d);};
  const prettyDay=day=>parseDay(day).toLocaleDateString('es-GT',{day:'numeric',month:'long',year:'numeric'});
  function showSection(){
    const names={tasks:'Calendario y entregas',daily:'Gimnasio mental',guides:'Guías de estudio',journal:'Mi diario',memory:'Perfil y recuerdos'};
    const current=names[location.hash.slice(1)]?location.hash.slice(1):'tasks';
    for(const panel of document.querySelectorAll('.study-panel'))panel.hidden=panel.id!==current;
    for(const link of document.querySelectorAll('.study-nav a')){const active=link.hash===`#${current}`;link.classList.toggle('is-active',active);if(active)link.setAttribute('aria-current','page');else link.removeAttribute('aria-current');}
    document.querySelector('#section-title').textContent=names[current];
  }
  function renderConnection(){
    const c=state.calendar;
    document.querySelector('#connection-title').textContent=c.connected?`Canvas · ${c.host}`:'Calendario local';
    let text=c.message;
    if(c.connected&&c.syncedAt)text=`Última actualización: ${new Date(c.syncedAt).toLocaleString('es-GT',{timeZone:'America/Guatemala'})}. ${text}`;
    if(c.error)text+=` ${c.error}`;
    if(c.skipped)text+=` ${c.skipped} elementos omitidos (recurrencias o datos incompatibles); comprueba también Canvas.`;
    document.querySelector('#connection-status').textContent=text;
    document.querySelector('.calendar-connection').classList.toggle('has-error',Boolean(c.error));
    document.querySelector('#sync-calendar').hidden=!c.connected;
    document.querySelector('#disconnect-calendar').hidden=!c.connected;
    document.querySelector('#open-connection').textContent=c.connected?'Conexión Canvas':'Conectar Canvas';
  }
  function taskCard(task,compact=false){
    const item=el('article',undefined,`study-item agenda-item kind-${task.kind.toLowerCase()}${task.done?' study-done':''}`);
    const type=task.kind==='TASK'?'Entrega':task.kind==='EVENT'?'Evento / clase':'Por clasificar';
    item.append(el('span',`${type}${task.done?' · completada aquí':''}`,'item-type'),el('h3',task.title));
    item.append(el('p',`${task.dueTime||'Sin hora indicada'} · ${compact?'':prettyDay(task.dueDate)+' · '}${task.course||'Personal'}`,'study-muted'));
    const actions=el('div',undefined,'study-actions');
    if(task.kind==='TASK')actions.append(action(task.done?'Reabrir':'Ya lo terminé',button=>run(()=>api(`/study/api/tasks/${task.id}`,'PUT',{done:!task.done}),button)));
    if(task.sourceUrl){try{const url=new URL(task.sourceUrl);if(url.protocol==='https:'){const link=el('a','Ver origen');link.href=url.href;link.target='_blank';link.rel='noopener noreferrer';actions.append(link);}}catch(_){/* enlace inválido */}}
    const more=el('details',undefined,'item-options');more.append(el('summary','Opciones'));
    for(const kind of ['TASK','EVENT'])if(kind!==task.kind)more.append(action(kind==='TASK'?'Es una entrega':'Es un evento',button=>run(()=>api(`/study/api/tasks/${task.id}/kind`,'PUT',{kind}),button,'Clasificación guardada.')));
    if(!task.sourceUid)more.append(action('Eliminar',button=>remove('tasks',task.id,button)));
    if(more.children.length>1)actions.append(more);item.append(actions);return item;
  }
  function renderCalendar(){
    const tasks=state.tasks;
    const stats=document.querySelector('#agenda-stats');stats.replaceChildren();
    for(const [label,value] of [ ['Entregas de hoy',tasks.filter(t=>t.kind==='TASK'&&!t.done&&t.dueDate===state.today).length],['Próximos 7 días',tasks.filter(t=>t.kind==='TASK'&&!t.done&&t.dueDate>=state.today&&t.dueDate<=plusDays(state.today,6)).length],['Eventos de hoy',tasks.filter(t=>t.kind==='EVENT'&&t.dueDate===state.today).length] ]){const card=el('div',undefined,'stat-card');card.append(el('strong',String(value)),el('span',label));stats.append(card);}
    document.querySelector('#month-label').textContent=month.toLocaleDateString('es-GT',{month:'long',year:'numeric'});
    const grid=document.querySelector('#calendar-grid');grid.replaceChildren();
    const first=new Date(month.getFullYear(),month.getMonth(),1,12);const offset=(first.getDay()+6)%7;first.setDate(1-offset);
    for(let i=0;i<42;i++){
      const day=new Date(first);day.setDate(first.getDate()+i);const key=dayKey(day);const entries=tasks.filter(t=>t.dueDate===key);
      const button=action('',()=>{selectedDay=key;renderCalendar();});button.className='calendar-day';
      button.classList.toggle('outside-month',day.getMonth()!==month.getMonth());button.classList.toggle('is-today',key===state.today);button.classList.toggle('is-selected',key===selectedDay);
      button.setAttribute('aria-pressed',String(key===selectedDay));button.setAttribute('aria-label',`${prettyDay(key)}, ${entries.length} elementos${key===state.today?', hoy':''}`);
      button.append(el('span',String(day.getDate()),'day-number'));
      for(const task of entries.slice(0,2))button.append(el('span',task.title,`calendar-chip kind-${task.kind.toLowerCase()}${task.done?' chip-done':''}`));
      if(entries.length>2)button.append(el('small',`+${entries.length-2} más`));grid.append(button);
    }
    document.querySelector('#selected-date').textContent=prettyDay(selectedDay);
    const agenda=document.querySelector('#day-agenda');agenda.replaceChildren();
    for(const [kind,label] of [['TASK','Entregas'],['EVENT','Eventos y clases'],['REVIEW','Por clasificar']]){
      const entries=tasks.filter(t=>t.dueDate===selectedDay&&t.kind===kind);
      if(entries.length){agenda.append(el('h3',label,'agenda-group'));entries.forEach(t=>agenda.append(taskCard(t,true)));}
    }empty(agenda,'Un día despejado. No hay elementos registrados para esta fecha.');
  }
  function renderTasks(){
    const filter=document.querySelector('#task-filter').value;
    const week=t=>t.dueDate>=state.today&&t.dueDate<=plusDays(state.today,6);
    const tests={today:t=>t.kind==='TASK'&&!t.done&&t.dueDate===state.today,upcoming:t=>t.kind==='TASK'&&!t.done&&week(t),future:t=>t.kind==='TASK'&&!t.done&&t.dueDate>=state.today,events:t=>t.kind==='EVENT'&&week(t),overdue:t=>t.kind==='TASK'&&!t.done&&t.dueDate<state.today,done:t=>t.kind==='TASK'&&t.done,review:t=>t.kind==='REVIEW'};
    let tasks=state.tasks.filter(tests[filter]);if(filter==='overdue'||filter==='done')tasks=tasks.slice().reverse();
    const pages=Math.max(1,Math.ceil(tasks.length/8));taskPage=Math.min(taskPage,pages-1);
    const list=document.querySelector('#task-list');list.replaceChildren();
    if(filter==='overdue')list.append(el('p','Fechas pasadas sin completar en esta app. No significa que no hayas entregado en Canvas.','study-muted'));
    tasks.slice(taskPage*8,taskPage*8+8).forEach(t=>list.append(taskCard(t)));
    if(!tasks.length)list.append(el('p','No hay elementos en esta vista.','study-muted'));
    const pagination=document.querySelector('#task-pagination');pagination.replaceChildren();
    pagination.append(el('span',`${tasks.length} elementos · Página ${taskPage+1} de ${pages}`,'study-muted'));
    if(taskPage>0)pagination.append(action('Anterior',()=>{taskPage--;renderTasks();}));
    if(taskPage+1<pages)pagination.append(action('Siguiente',()=>{taskPage++;renderTasks();}));
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
  async function load(calendarOnly=false){
    const version=++loadVersion;const previous=state;const next=await api('/study/api/dashboard');
    if(version!==loadVersion)return;state=next;
    if(!initialized){selectedDay=state.today;month=parseDay(state.today);month.setDate(1);forms.task.elements.dueDate.value=state.today;fillJournal(state.journal.find(e=>e.day===state.today)||{day:state.today});initialized=true;}
    document.querySelector('#today-label').textContent=`${prettyDay(state.today)} · Guatemala`;
    if(!calendarOnly||previous?.today!==state.today||JSON.stringify(previous?.tasks)!==JSON.stringify(state.tasks)){renderTasks();renderCalendar();}
    renderConnection();
    // Una actualización automática no borra el intento que estás escribiendo en el gimnasio.
    if(!calendarOnly){renderNotes();renderJournal();renderGuides();renderDaily();}
  }
  function bind(name,path,prepare,after,message){forms[name].addEventListener('submit',event=>{event.preventDefault();const form=event.currentTarget;run(async()=>{const data=await prepare(Object.fromEntries(new FormData(form)),form);const result=await api(path,'POST',data);if(after)after(form);return result;},event.submitter,message);});}
  bind('note','/study/api/notes',data=>data,form=>form.reset(),'Recuerdo guardado.');
  bind('task','/study/api/tasks',data=>data,form=>{form.elements.title.value='';},'Pendiente añadido.');
  bind('journal','/study/api/journal',data=>data,null,'Reflexión guardada.');
  bind('guide','/study/api/guides',data=>({...data,sourceChatId:data.sourceChatId?Number(data.sourceChatId):null}),null,'Guía generada y guardada.');
  bind('calendar','/study/api/calendar',async(data,form)=>{const file=form.elements.calendar.files[0];if(!file||file.size>2000000)throw new Error('Selecciona un archivo .ics de hasta 2 MB.');return {content:await file.text(),course:data.course};},null,result=>`${result.imported} eventos importados o actualizados; ${result.skipped} omitidos. ${result.message}`);
  bind('feed','/study/api/calendar/connect',data=>data,form=>{form.elements.url.value='';document.querySelector('#connection-details').open=false;},'Canvas conectado. Fechas actualizadas.');
  document.querySelector('#task-filter').addEventListener('change',()=>{taskPage=0;renderTasks();});
  document.querySelector('#previous-month').addEventListener('click',()=>{month.setMonth(month.getMonth()-1);renderCalendar();});
  document.querySelector('#next-month').addEventListener('click',()=>{month.setMonth(month.getMonth()+1);renderCalendar();});
  document.querySelector('#calendar-today').addEventListener('click',()=>{month=parseDay(state.today);month.setDate(1);selectedDay=state.today;renderCalendar();});
  for(const [button,id] of [['open-task','new-task-details'],['open-connection','connection-details']])document.querySelector(`#${button}`).addEventListener('click',()=>{const details=document.querySelector(`#${id}`);details.open=true;details.querySelector('input')?.focus();});
  document.querySelector('#sync-calendar').addEventListener('click',event=>run(()=>api('/study/api/calendar/sync','POST'),event.currentTarget,'Canvas actualizado.'));
  document.querySelector('#disconnect-calendar').addEventListener('click',event=>run(()=>api('/study/api/calendar/connection','DELETE'),event.currentTarget,'Conexión eliminada. Se conserva una copia local.'));
  window.addEventListener('hashchange',showSection);showSection();
  setInterval(()=>{if(initialized&&!document.hidden)load(true).catch(error=>say(error.message,true));},60000);
  forms.journal.elements.day.addEventListener('change',()=>fillJournal(state?.journal.find(e=>e.day===forms.journal.elements.day.value)||{day:forms.journal.elements.day.value}));
  (async()=>{try{await load();const chats=await api('/chat/api/chats');const select=document.querySelector('#guide-chat');for(const chat of chats){const option=el('option',chat.title);option.value=String(chat.id);select.append(option);}const selected=new URLSearchParams(location.search).get('chat');if(selected&&chats.some(c=>String(c.id)===selected)){select.value=selected;forms.guide.elements.topic.value=chats.find(c=>String(c.id)===selected).title;}say('Tu espacio está actualizado.');}catch(error){say(error.message,true);}})();
})();
