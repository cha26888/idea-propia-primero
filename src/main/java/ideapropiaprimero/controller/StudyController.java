package ideapropiaprimero.controller;

import ideapropiaprimero.integration.ai.AiException;
import ideapropiaprimero.repository.*;
import ideapropiaprimero.service.*;
import java.security.Principal;
import java.util.*;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class StudyController {
    private final StudyRepository store;private final StudyService studies;private final UserRepository users;private final CanvasCalendarService calendar;
    public StudyController(StudyRepository store,StudyService studies,UserRepository users,CanvasCalendarService calendar){this.store=store;this.studies=studies;this.users=users;this.calendar=calendar;}
    private long owner(Principal p){return users.findByLogin(p.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED)).getId();}
    private static void found(boolean ok){if(!ok)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"No encontramos ese elemento o ya existe un recuerdo igual.");}
    @GetMapping("/study") public String view(){return "study/index";}
    @GetMapping("/study/api/dashboard") @ResponseBody
    public Map<String,Object> dashboard(Principal p){long u=owner(p);calendar.refresh(u,false);return Map.of("calendar",calendar.status(u),"today",StudyService.today().toString(),"notes",store.notes(u),"tasks",store.tasks(u),"journal",store.journal(u),"guides",store.guides(u),"daily",studies.daily(u));}
    public record NoteRequest(String kind,String content,Long sourceChatId) { }
    @PostMapping("/study/api/notes") @ResponseBody
    public Map<String,Boolean> addNote(@RequestBody NoteRequest r,Principal p){required(r);studies.addNote(owner(p),r.kind(),r.content(),r.sourceChatId());return Map.of("saved",true);}
    @PutMapping("/study/api/notes/{id}") @ResponseBody
    public Map<String,Boolean> editNote(@PathVariable long id,@RequestBody NoteRequest r,Principal p){required(r);found(store.editNote(owner(p),id,StudyService.text(r.content(),800,false)));return Map.of("saved",true);}
    @DeleteMapping("/study/api/notes/{id}") @ResponseBody
    public Map<String,Boolean> deleteNote(@PathVariable long id,Principal p){found(store.deleteNote(owner(p),id));return Map.of("deleted",true);}
    public record TaskRequest(String title,String course,String dueDate) { }
    @PostMapping("/study/api/tasks") @ResponseBody
    public Map<String,Boolean> addTask(@RequestBody TaskRequest r,Principal p){required(r);store.addTask(owner(p),StudyService.text(r.title(),160,false),StudyService.text(r.course(),80,true),StudyService.date(r.dueDate()));return Map.of("saved",true);}
    public record DoneRequest(Boolean done) { }
    @PutMapping("/study/api/tasks/{id}") @ResponseBody
    public Map<String,Boolean> done(@PathVariable long id,@RequestBody DoneRequest r,Principal p){required(r);if(r.done()==null)throw new IllegalArgumentException("Indica si está completada.");found(store.completeTask(owner(p),id,r.done()));return Map.of("saved",true);}
    @DeleteMapping("/study/api/tasks/{id}") @ResponseBody
    public Map<String,Boolean> deleteTask(@PathVariable long id,Principal p){found(store.deleteTask(owner(p),id));return Map.of("deleted",true);}
    public record CalendarRequest(String content,String course) { }
    @PostMapping("/study/api/calendar") @ResponseBody
    public Map<String,Object> calendar(@RequestBody CalendarRequest r,Principal p){required(r);var parsed=CalendarImportService.parse(r.content());int count=store.importTasks(owner(p),StudyService.text(r.course(),80,true),parsed.events());return Map.of("imported",count,"skipped",parsed.skipped(),"message","Importación manual. Se omiten recurrencias, cancelados y eventos no compatibles; no es una sincronización automática.");}
    public record FeedRequest(String url,String course) { }
    @PostMapping("/study/api/calendar/connect") @ResponseBody
    public Map<String,Object> connect(@RequestBody FeedRequest r,Principal p){required(r);long u=owner(p);calendar.connect(u,r.url(),r.course());return calendar.status(u);}
    @PostMapping("/study/api/calendar/sync") @ResponseBody
    public Map<String,Object> sync(Principal p){long u=owner(p);calendar.refresh(u,true);return calendar.status(u);}
    @DeleteMapping("/study/api/calendar/connection") @ResponseBody
    public Map<String,Boolean> disconnect(Principal p){calendar.disconnect(owner(p));return Map.of("disconnected",true);}
    public record KindRequest(String kind) { }
    @PutMapping("/study/api/tasks/{id}/kind") @ResponseBody
    public Map<String,Boolean> classify(@PathVariable long id,@RequestBody KindRequest r,Principal p){required(r);found(store.classifyTask(owner(p),id,StudyService.text(r.kind(),10,false)));return Map.of("saved",true);}
    public record JournalRequest(String day,String learned,String difficult,String nextStep) { }
    @PostMapping("/study/api/journal") @ResponseBody
    public Map<String,Boolean> journal(@RequestBody JournalRequest r,Principal p){required(r);store.saveJournal(owner(p),StudyService.date(r.day()),StudyService.text(r.learned(),3000,false),StudyService.text(r.difficult(),3000,true),StudyService.text(r.nextStep(),1500,true));return Map.of("saved",true);}
    @DeleteMapping("/study/api/journal/{id}") @ResponseBody
    public Map<String,Boolean> deleteJournal(@PathVariable long id,Principal p){found(store.deleteJournal(owner(p),id));return Map.of("deleted",true);}
    public record GuideRequest(String topic,String goal,Long sourceChatId) { }
    @PostMapping("/study/api/guides") @ResponseBody
    public Map<String,Boolean> guide(@RequestBody GuideRequest r,Principal p){required(r);studies.generateGuide(owner(p),r.topic(),r.goal(),r.sourceChatId());return Map.of("saved",true);}
    @DeleteMapping("/study/api/guides/{id}") @ResponseBody
    public Map<String,Boolean> deleteGuide(@PathVariable long id,Principal p){found(store.deleteGuide(owner(p),id));return Map.of("deleted",true);}
    public record DailyRequest(String exerciseId,int choice,String reason) { }
    @PostMapping("/study/api/daily") @ResponseBody
    public Map<String,Object> daily(@RequestBody DailyRequest r,Principal p){required(r);long u=owner(p);studies.answerDaily(u,r.exerciseId(),r.choice(),r.reason());return studies.daily(u);}
    private static void required(Object r){if(r==null)throw new IllegalArgumentException("Faltan los datos del formulario.");}
    @ExceptionHandler(IllegalArgumentException.class) @ResponseBody
    public ResponseEntity<?> invalid(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));}
    @ExceptionHandler(AiException.class) @ResponseBody
    public ResponseEntity<?> aiError(AiException e){return ResponseEntity.status(e.status()).body(Map.of("error",e.getMessage()));}
    @ExceptionHandler(IllegalStateException.class) @ResponseBody
    public ResponseEntity<?> storageError(IllegalStateException e){return ResponseEntity.status(500).body(Map.of("error","No se pudo completar la operación. Intenta de nuevo."));}
}
