package ideapropiaprimero.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import ideapropiaprimero.repository.StudyRepository;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class CanvasCalendarServiceTest {
    final StudyRepository store=mock(StudyRepository.class);
    final String url="https://uvg.instructure.com/feeds/calendars/private_secret.ics";
    final String feed="BEGIN:VCALENDAR\nBEGIN:VEVENT\nUID:event-assignment-12\nSUMMARY:POO\nDTSTART;VALUE=DATE:20261112\nEND:VEVENT\nEND:VCALENDAR";
    @Test void conexionImportaDesdeLaUrlPeroElEstadoPublicoNoExponeElToken() {
        var service=new CanvasCalendarService(store,""){@Override String download(URI uri){assertEquals(url,uri.toString());return feed;}};
        service.connect(1,url,"UVG");
        verify(store).syncCalendar(eq(1L),eq(url),eq("UVG"),argThat(events->events.size()==1&&events.get(0).kind().equals("TASK")),eq(0),anyString(),eq(true));
        when(store.calendarFeed(1)).thenReturn(Optional.of(new StudyRepository.CalendarFeed(url,"UVG",Instant.now().toString(),Instant.now().toString(),"",0)));
        assertFalse(service.status(1).toString().contains("private_secret"));
    }
    @Test void falloDeDescargaNoReemplazaLaUltimaCopia() {
        var service=new CanvasCalendarService(store,""){@Override String download(URI uri){throw new IllegalArgumentException("Canvas no disponible");}};
        when(store.calendarFeed(1)).thenReturn(Optional.of(new StudyRepository.CalendarFeed(url,"UVG","2026-11-01T00:00:00Z","2026-11-01T00:00:00Z","",0)));
        service.refresh(1,false);
        verify(store).calendarFailure(eq(1L),anyString(),eq("Canvas no disponible"));
        verify(store,never()).syncCalendar(anyLong(),anyString(),anyString(),anyList(),anyInt(),anyString(),anyBoolean());
        assertThrows(IllegalArgumentException.class,()->service.refresh(1,true));
    }
    @Test void consultasRepetidasNoDescarganMientrasLaCopiaEsReciente() {
        var service=new CanvasCalendarService(store,""){@Override String download(URI uri){throw new AssertionError("No debería descargar");}};
        when(store.calendarFeed(1)).thenReturn(Optional.of(new StudyRepository.CalendarFeed(url,"UVG",Instant.now().toString(),Instant.now().toString(),"",0)));
        service.refresh(1,false);verify(store,never()).calendarFailure(anyLong(),anyString(),anyString());
    }
}
