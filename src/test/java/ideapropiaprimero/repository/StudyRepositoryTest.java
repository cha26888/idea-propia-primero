package ideapropiaprimero.repository;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StudyRepositoryTest {
    @TempDir Path directory;
    private String database() throws Exception {
        String path=directory.resolve("study.db").toString();
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+path);var s=c.createStatement()){s.execute("CREATE TABLE users(id INTEGER PRIMARY KEY)");s.execute("INSERT INTO users VALUES(1),(2)");}
        new ChatRepository(path);return path;
    }
    @Test void notasYDiarioSeAislanPorUsuarioYSePuedenBorrar() throws Exception {
        var store=new StudyRepository(database());store.addNote(1,"PROFILE","Prefiero ejemplos de Java",null);store.addNote(1,"PROFILE","Prefiero ejemplos de Java",null);
        assertEquals(1,store.notes(1).size());assertTrue(store.notes(2).isEmpty());long id=store.notes(1).get(0).id();
        assertFalse(store.editNote(2,id,"Ajeno"));assertFalse(store.deleteNote(2,id));assertTrue(store.editNote(1,id,"Ejemplos breves"));
        store.saveJournal(1,"2026-11-12","Aprendí","Duda","Repasar");store.saveJournal(1,"2026-11-12","Actualizado","","Otro paso");
        assertEquals(1,store.journal(1).size());assertEquals("Actualizado",store.journal(1).get(0).learned());assertTrue(store.journal(2).isEmpty());assertTrue(store.deleteNote(1,id));
    }
    @Test void importarDosVecesActualizaSinDuplicarNiDesmarcar() throws Exception {
        var store=new StudyRepository(database());store.importTasks(1,"Álgebra",List.of(new StudyRepository.CalendarTask("unique","Examen","2026-11-12")));
        long id=store.tasks(1).get(0).id();store.completeTask(1,id,true);
        store.importTasks(1,"Álgebra",List.of(new StudyRepository.CalendarTask("unique","Examen movido","2026-11-15")));
        assertEquals(1,store.tasks(1).size());assertTrue(store.tasks(1).get(0).done());assertEquals("2026-11-15",store.tasks(1).get(0).dueDate());
        assertFalse(store.deleteTask(2,id));assertFalse(store.completeTask(2,id,false));assertTrue(store.tasks(2).isEmpty());
    }
    @Test void conservaPrimerIntentoDiarioYProtegeGuias() throws Exception {
        var store=new StudyRepository(database());var first=new StudyRepository.DailyAttempt("2026-11-12","logic-1",0,"Mi razonamiento",false,"Explicación");
        store.saveDaily(1,first);store.saveDaily(1,new StudyRepository.DailyAttempt("2026-11-12","logic-1",2,"Copia posterior",true,"Otra"));
        assertEquals(first,store.daily(1,"2026-11-12").orElseThrow());assertTrue(store.daily(2,"2026-11-12").isEmpty());
        store.saveGuide(1,"Tema","# Guía",null);long id=store.guides(1).get(0).id();assertFalse(store.deleteGuide(2,id));assertTrue(store.guides(2).isEmpty());assertTrue(store.deleteGuide(1,id));
    }
    @Test void sincronizacionActualizaRetiraYConservaLasMarcasSinTocarOtrosUsuarios() throws Exception {
        var store=new StudyRepository(database());
        store.addTask(1,"Manual","POO","2026-11-12");store.addTask(2,"Ajeno","POO","2026-11-12");
        var imported=new StudyRepository.CalendarTask("assignment-1","Entrega","2026-11-12","TASK","23:59","");
        var removed=new StudyRepository.CalendarTask("event-1","Clase","2026-11-12","EVENT","10:00","");
        store.importTasks(1,"UVG",List.of(imported,removed));
        long id=store.tasks(1).stream().filter(t->t.title().equals("Entrega")).findFirst().orElseThrow().id();store.completeTask(1,id,true);
        store.syncCalendar(1,"https://uvg.instructure.com/feeds/calendars/demo.ics","UVG",List.of(imported),0,"2026-11-12T12:00:00Z",true);
        assertEquals(2,store.tasks(1).size());assertEquals(1,store.tasks(2).size());
        var changed=new StudyRepository.CalendarTask("assignment-1","Entrega movida","2026-11-15","TASK","20:00","");
        store.classifyTask(1,id,"EVENT");assertFalse(store.classifyTask(2,id,"TASK"));
        store.syncCalendar(1,"https://uvg.instructure.com/feeds/calendars/demo.ics","UVG",List.of(changed),0,"2026-11-12T13:00:00Z",false);
        var task=store.tasks(1).stream().filter(t->t.id()==id).findFirst().orElseThrow();
        assertTrue(task.done());assertEquals("2026-11-15",task.dueDate());assertEquals("EVENT",task.kind());
        store.syncCalendar(1,"https://uvg.instructure.com/feeds/calendars/demo.ics","UVG",List.of(),0,"2026-11-12T14:00:00Z",false);
        assertEquals(1,store.tasks(1).size());assertEquals("Manual",store.tasks(1).get(0).title());
        store.disconnectCalendar(1);assertTrue(store.calendarFeed(1).isEmpty());
    }
    @Test void migracionNoConvierteLasClasesAntiguasEnEntregas() throws Exception {
        String path=database();
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+path);var s=c.createStatement()){
            s.execute("CREATE TABLE study_tasks(id INTEGER PRIMARY KEY,user_id INTEGER,title TEXT,course TEXT,due_date TEXT,done INTEGER DEFAULT 0,source_uid TEXT,UNIQUE(user_id,source_uid))");
            s.execute("INSERT INTO study_tasks VALUES(1,1,'Clase','UVG','2026-11-11',0,'legacy'),(2,1,'Personal','','2026-11-12',0,NULL)");
        }
        var store=new StudyRepository(path);assertEquals("REVIEW",store.tasks(1).get(0).kind());assertEquals("TASK",store.tasks(1).get(1).kind());
        new StudyRepository(path);assertEquals(2,store.tasks(1).size());
    }
}
