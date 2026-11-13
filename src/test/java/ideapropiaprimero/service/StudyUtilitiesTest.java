package ideapropiaprimero.service;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class StudyUtilitiesTest {
    @Test void tituloConservaTemaSinSaludoNiDeclaracionDeBloqueo() {
        assertEquals("Resolver ecuaciones lineales",ChatTitles.from("Ayúdame a resolver ecuaciones lineales. Todavía no he intentado nada."));
        assertTrue(ChatTitles.from("Un tema extenso ".repeat(30)).length()<=64);
        assertFalse(ChatTitles.from("Tema\ncon\tvarias líneas").contains("\n"));
    }
    @Test void practicaDiariaEsEstableYNoPublicaLaRespuestaEnElEjercicio() {
        var date=LocalDate.of(2026,11,12);assertEquals(DailyPractice.exercise(date),DailyPractice.exercise(date));
        int correct=0;for(int choice=0;choice<3;choice++)if(DailyPractice.correct(date,choice))correct++;
        assertEquals(1,correct);assertFalse(DailyPractice.exercise(date).id().equals(DailyPractice.exercise(date.plusDays(1)).id()));
    }
    @Test void validaLimitesYFechasAntesDeGuardar() {
        assertThrows(IllegalArgumentException.class,()->StudyService.text(" ",100,false));
        assertThrows(IllegalArgumentException.class,()->StudyService.text("a".repeat(801),800,false));
        assertThrows(IllegalArgumentException.class,()->StudyService.date("2026-02-30"));
        assertEquals("2026-11-12",StudyService.date("2026-11-12"));
    }
}
