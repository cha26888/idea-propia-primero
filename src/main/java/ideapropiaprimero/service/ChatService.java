package ideapropiaprimero.service;

import ideapropiaprimero.integration.ai.AiClient;
import ideapropiaprimero.integration.ai.AiException;
import ideapropiaprimero.integration.ai.ChatMessage;
import ideapropiaprimero.model.TutorMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    /** Presupuesto de caracteres, no una medición de tokens del proveedor. */
    static final int MAX_CONTEXT_CHARACTERS = 32_000;
    private final AiClient ai;
    private final TutorSkill skill;

    public ChatService(AiClient ai, TutorSkill skill) {
        this.ai = ai;
        this.skill = skill;
    }

    public String responder(List<ChatMessage> history) {
        return responder(history, TutorMode.PRACTICE);
    }

    public String responder(List<ChatMessage> history, TutorMode mode) {
        return responder(history, mode, "");
    }

    public String responder(List<ChatMessage> history, TutorMode mode, String studyContext) {
        validate(history);
        if (mode == null) throw new IllegalArgumentException("Selecciona un modo de acompañamiento.");
        var opening = TutorOpening.responseFor(mode, history.get(history.size() - 1).content());
        if (opening.isPresent()) return opening.get();
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", skill.sistema(mode, history, studyContext)));
        int memoryLength = studyContext == null ? 0 : Math.min(5200, studyContext.length());
        messages.addAll(selectContext(history, MAX_CONTEXT_CHARACTERS - memoryLength));
        // Una generación: los ejemplos no se penalizan por contener cálculos.
        String response = ai.responder(List.copyOf(messages));
        if (response == null || response.isBlank() || response.length() > 20_000) {
            throw new AiException(502, "La IA no devolvió una respuesta de texto utilizable.");
        }
        return response;
    }

    /** Pares completos recientes y, si cabe, el inicial; nunca corta código. */
    List<ChatMessage> selectContext(List<ChatMessage> history) {
        return selectContext(history, MAX_CONTEXT_CHARACTERS);
    }

    private List<ChatMessage> selectContext(List<ChatMessage> history, int maxCharacters) {
        int last = history.size() - 1;
        int budget = maxCharacters - history.get(last).content().length();
        int start = last;
        while (start >= 2) {
            int length = history.get(start - 2).content().length()
                    + history.get(start - 1).content().length();
            if (length > budget) break;
            budget -= length;
            start -= 2;
        }
        List<ChatMessage> selected = new ArrayList<>();
        if (start > 0 && history.get(0).content().length() + history.get(1).content().length() <= budget) {
            selected.add(history.get(0));
            selected.add(history.get(1));
        }
        selected.addAll(history.subList(start, history.size()));
        return List.copyOf(selected);
    }

    private static void validate(List<ChatMessage> history) {
        if (history == null || history.isEmpty() || history.size() > 21 || history.size() % 2 == 0) {
            throw new IllegalArgumentException("Envía hasta 10 pares de mensajes previos y una pregunta nueva.");
        }
        for (int i = 0; i < history.size(); i++) {
            ChatMessage message = history.get(i);
            String role = i % 2 == 0 ? "user" : "assistant";
            int limit = role.equals("user") ? 4000 : 20_000;
            if (message == null || !role.equals(message.role()) || message.content() == null
                    || message.content().isBlank() || message.content().length() > limit) {
                throw new IllegalArgumentException("Mensajes inválidos: alterna user y assistant, terminando en user. "
                        + "Máximo 4000 caracteres por pregunta y 20000 por respuesta previa.");
            }
        }
    }
}
