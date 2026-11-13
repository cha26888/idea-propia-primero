package ideapropiaprimero.service;
import static org.junit.jupiter.api.Assertions.*;
import ideapropiaprimero.repository.StudyRepository.Task;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class StudyAgendaTest {
    private final LocalDate today=LocalDate.of(2026,11,12);
    private Task item(long id,String title,String day,String kind,boolean done){return new Task(id,title,"UVG",day,done,"uid-"+id,kind,"18:00","");}
    @Test void hoyNoIncluyeClasesDeAyerNiTareasDeHaceUnMes() {
        var tasks=List.of(item(1,"Lab viejo","2026-10-12","TASK",false),item(2,"English ayer","2026-11-11","EVENT",false),
                item(3,"Guat If viejo","2026-10-15","TASK",false),item(4,"Entrega POO","2026-11-12","TASK",false),
                item(5,"Actividad cultural","2026-11-12","EVENT",false),item(6,"Tarea ya terminada","2026-11-12","TASK",true));
        var reply=StudyAgenda.reply(tasks,"hoy empiezo, dime que pendientes tengo para el día de hoy?",today).orElseThrow();
        assertTrue(reply.contains("Entrega POO"));assertTrue(reply.contains("Eventos y clases"));assertTrue(reply.contains("Actividad cultural"));
        assertFalse(reply.contains("Lab viejo"));assertFalse(reply.contains("English ayer"));assertFalse(reply.contains("Guat If viejo"));assertFalse(reply.contains("Tarea ya terminada"));
        assertTrue(reply.contains("12/11/2026"));
    }
    @Test void mananaYFechasExplicitasNoSeConfundenConHoy() {
        var tasks=List.of(item(1,"Hoy POO","2026-11-12","TASK",false),item(2,"Mañana parcial","2026-11-13","TASK",false));
        var next=StudyAgenda.reply(tasks,"¿Qué entregas tengo mañana?",today).orElseThrow();assertTrue(next.contains("Mañana parcial"));assertFalse(next.contains("Hoy POO"));
        var fixed=StudyAgenda.reply(tasks,"Dime las tareas para 12/11/26",today.plusDays(3)).orElseThrow();assertTrue(fixed.contains("Hoy POO"));assertFalse(fixed.contains("Mañana parcial"));
        assertTrue(StudyAgenda.reply(tasks,"Dime tareas para 31/02/2026",today).orElseThrow().contains("no es válida"));
    }
    @Test void unaConsultaDeTutorNoSeInterceptaComoConsultaDeAgenda() {
        assertTrue(StudyAgenda.reply(List.of(),"Ayúdame a resolver mi tarea de ecuaciones",today).isEmpty());
        assertTrue(StudyAgenda.reply(List.of(),"¿Qué es la herencia en Java?",today).isEmpty());
        assertTrue(StudyAgenda.reply(List.of(),"Qué pendientes tengo hoy",today).orElseThrow().contains("No hay entregas"));
    }
    @Test void vencidosSonUnaVistaExplicitaYLosImportadosAntiguosRequierenClasificacion() {
        var tasks=List.of(item(1,"Pasada","2026-10-12","TASK",false),item(2,"Antiguo sin tipo","2026-11-12","REVIEW",false));
        assertTrue(StudyAgenda.reply(tasks,"Dime mis pendientes vencidos",today).orElseThrow().contains("Pasada"));
        var reply=StudyAgenda.reply(tasks,"Dime mis pendientes hoy",today).orElseThrow();assertTrue(reply.contains("sin clasificar"));assertFalse(reply.contains("Pasada"));
    }
}
