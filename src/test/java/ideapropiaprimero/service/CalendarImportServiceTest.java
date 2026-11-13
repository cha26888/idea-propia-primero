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
}
