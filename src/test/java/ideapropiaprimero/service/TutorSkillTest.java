package ideapropiaprimero.service;

import static org.junit.jupiter.api.Assertions.*;
import ideapropiaprimero.integration.ai.ChatMessage;
import ideapropiaprimero.model.TutorMode;
import java.util.List;
import org.junit.jupiter.api.Test;

class TutorSkillTest {
    private final TutorSkill skill = new TutorSkill();
    private List<ChatMessage> question(String text) {
        return List.of(new ChatMessage("user", text));
    }
    @Test void cadaModoPermiteConceptosSinCargarLosOtrosModos() {
        for (TutorMode mode : TutorMode.values()) {
            String prompt = skill.sistema(mode, question("¿Qué es x = 4?"));
            assertTrue(prompt.contains("(" + mode.name() + ")"));
            for (TutorMode other : TutorMode.values()) {
                if (other != mode) assertFalse(prompt.contains("(" + other.name() + ")"));
            }
            assertTrue(prompt.contains("Las definiciones y aclaraciones están disponibles en TODOS los modos"));
            assertTrue(prompt.split("\\s+").length < 1100);
        }
    }
    @Test void distingueBloqueoDeUnaPreguntaOIntento() {
        for (String text : List.of("no sé", "¿No entiendo?", "sigo sin entender!", "No sé cómo empezar")) {
            assertEquals(1, skill.consecutiveBlocks(question(text)), text);
        }
        for (String text : List.of("¿Qué es 2 + 2?", "no entiendo interfaces en Java",
                "creo que x = 4, pero no sé", "hazlo tú")) {
            assertEquals(0, skill.consecutiveBlocks(question(text)), text);
        }
    }
    @Test void cambiaElApoyoTrasBloqueoRepetidoYLoReiniciaAnteUnIntento() {
        var history = List.of(new ChatMessage("user", "no sé"),
                new ChatMessage("assistant", "Identifica la variable."),
                new ChatMessage("user", "sigo sin entender"));
        assertEquals(2, skill.consecutiveBlocks(history));
        assertTrue(skill.sistema(TutorMode.PRACTICE, history).contains("bloqueo repetido"));
        var attempt = List.of(new ChatMessage("user", "no sé"),
                new ChatMessage("assistant", "Probemos con otro ejemplo."),
                new ChatMessage("user", "Creo que debo sumar 4"));
        assertEquals(0, skill.consecutiveBlocks(attempt));
    }
}
