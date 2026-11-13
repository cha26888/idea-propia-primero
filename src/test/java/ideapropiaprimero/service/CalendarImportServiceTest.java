package ideapropiaprimero.service;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CalendarImportServiceTest {
    @Test void interpretaLineasPlegadasYConvierteUtcAGuatemala() {
        var result=CalendarImportService.parse("BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nUID:one\r\nSUMMARY:Examen de\r\n álgebra\\, unidad 2\r\nDTSTART:20261113T010000Z\r\nEND:VEVENT\r\nEND:VCALENDAR");
        assertEquals(1,result.events().size());assertEquals("Examen deálgebra, unidad 2",result.events().get(0).title());assertEquals("2026-11-12",result.events().get(0).day());
    }
    @Test void omiteRecurrenciasYFechasInvalidasSinInventarEventos() {
        var result=CalendarImportService.parse("""
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:weekly
            SUMMARY:Clase semanal
            DTSTART;VALUE=DATE:20261112
            RRULE:FREQ=WEEKLY
            END:VEVENT
            BEGIN:VEVENT
            UID:bad
            SUMMARY:Fecha imposible
            DTSTART;VALUE=DATE:20260230
            END:VEVENT
            BEGIN:VTODO
            UID:homework
            SUMMARY:Entrega
            DUE;VALUE=DATE:20261114
            END:VTODO
            END:VCALENDAR
            """);
        assertEquals(2,result.skipped());assertEquals(1,result.events().size());assertEquals("2026-11-14",result.events().get(0).day());
    }
    @Test void ignoraAlarmasYRechazaArchivosIncompletos() {
        var result=CalendarImportService.parse("BEGIN:VCALENDAR\nBEGIN:VEVENT\nUID:one\nSUMMARY:Examen\nDTSTART;VALUE=DATE:20261112\nBEGIN:VALARM\nSUMMARY:No reemplazar título\nEND:VALARM\nEND:VEVENT\nEND:VCALENDAR");
        assertEquals("Examen",result.events().get(0).title());
        assertThrows(IllegalArgumentException.class,()->CalendarImportService.parse("BEGIN:VCALENDAR\nBEGIN:VEVENT\nEND:VCALENDAR"));
        assertThrows(IllegalArgumentException.class,()->CalendarImportService.parse("x".repeat(400001)));
    }
    @Test void canvasDistingueEntregasDeClasesPorMetadatosYConservaLaHoraLocal() {
        var result=CalendarImportService.parse("""
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:event-assignment-123
            SUMMARY:Entrega POO
            DTSTART:20261113T055900Z
            END:VEVENT
            BEGIN:VEVENT
            UID:event-calendar-event-99
            SUMMARY:Clase de inglés
            DTSTART;TZID=America/Guatemala:20261112T090000
            END:VEVENT
            BEGIN:VEVENT
            UID:another
            SUMMARY:Entrega por URL
            URL:https://uvg.instructure.com/courses/10/assignments/42
            DTSTART;VALUE=DATE:20261112
            END:VEVENT
            BEGIN:VEVENT
            UID:query
            SUMMARY:Otra entrega
            URL:https://uvg.instructure.com/calendar?include_contexts=course_10&event_id=assignment_77
            DTSTART;VALUE=DATE:20261112
            END:VEVENT
            BEGIN:VEVENT
            UID:cancelled
            SUMMARY:Cancelado
            STATUS:CANCELLED
            DTSTART;VALUE=DATE:20261112
            END:VEVENT
            END:VCALENDAR
            """);
        assertEquals(4,result.events().size());
        assertEquals("TASK",result.events().get(0).kind());assertEquals("2026-11-12",result.events().get(0).day());assertEquals("23:59",result.events().get(0).time());
        assertEquals("EVENT",result.events().get(1).kind());assertEquals("09:00",result.events().get(1).time());
        assertEquals("TASK",result.events().get(2).kind());assertEquals("",result.events().get(2).time());assertEquals("TASK",result.events().get(3).kind());
    }
}
