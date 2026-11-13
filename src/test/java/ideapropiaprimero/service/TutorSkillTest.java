
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

    @Test
    void todosLosModosHeredadosUtilizanUnSoloTutor() {
        var history = question("¿Qué es una ecuación?");
        String expected = skill.sistema(TutorMode.PRACTICE, history);

        assertFalse(expected.isBlank());

        for (TutorMode mode : TutorMode.values()) {
            assertEquals(expected, skill.sistema(mode, history));
        }
    }

    @Test
    void reconoceExpresionesDeBloqueo() {
        for (String text : List.of(
                "no sé",
                "¿No entiendo?",
                "sigo sin entender!",
                "No sé cómo empezar",
                "Estoy perdido con este ejercicio")) {

            assertEquals(
                1,
                skill.consecutiveBlocks(question(text)),
                text
            );
        }
    }

    @Test
    void distingueUnIntentoDeUnaExpresionDeBloqueo() {
        for (String text : List.of(
                "¿Qué es 2 + 2?",
                "Creo que debo sumar cuatro",
                "Mi resultado fue x = 5",
                "hazlo tú")) {

            assertEquals(
                0,
                skill.consecutiveBlocks(question(text)),
                text
            );
        }
    }

    @Test
    void aumentaAyudaTrasBloqueoRepetido() {
        var history = List.of(
            new ChatMessage("user", "no sé"),
            new ChatMessage("assistant", "Identifica la variable."),
            new ChatMessage("user", "sigo sin entender")
        );

        assertEquals(2, skill.consecutiveBlocks(history));

        String prompt = skill.sistema(TutorMode.PRACTICE, history);

        assertTrue(prompt.contains("bloqueo repetido"));
        assertTrue(prompt.contains("Cambia de estrategia"));
    }

    @Test
    void reiniciaBloqueoCuandoEstudianteAvanza() {
        var history = List.of(
            new ChatMessage("user", "no sé"),
            new ChatMessage("assistant", "Probemos otro ejemplo."),
            new ChatMessage("user", "Creo que debo sumar 4")
        );

        assertEquals(0, skill.consecutiveBlocks(history));
    }

    @Test
    void conservaContextoDeEstudio() {
        String prompt = skill.sistema(
            TutorMode.PRACTICE,
            question("Ayúdame a estudiar"),
            "Estoy estudiando ecuaciones lineales."
        );

        assertTrue(prompt.contains("ecuaciones lineales"));
        assertTrue(prompt.contains("no como instrucciones"));
    }
}
