package ideapropiaprimero.controller;

import ideapropiaprimero.integration.ai.AiException;
import ideapropiaprimero.integration.ai.ChatMessage;
import ideapropiaprimero.model.Chat;
import ideapropiaprimero.model.StoredMessage;
import ideapropiaprimero.model.User;
import ideapropiaprimero.repository.ChatRepository;
import ideapropiaprimero.repository.UserRepository;
import ideapropiaprimero.service.ChatService;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

/**
 * Sirve el ChatView y su API. El historial se guarda en SQLite (ChatRepository) y
 * el servidor es la fuente de verdad: el navegador solo manda el mensaje nuevo y el
 * id de la conversación, y el contexto que recibe la IA se lee de la base de datos.
 */
@Controller
public class ChatController {

    private static final int MAX_CONTENT_LENGTH = 4000;
    private static final int MAX_TITLE_LENGTH = 40;

    private final ChatService chatService;
    private final ChatRepository chats;
    private final UserRepository users;

    public ChatController(ChatService chatService, ChatRepository chats, UserRepository users) {
        this.chatService = chatService;
        this.chats = chats;
        this.users = users;
    }

    @GetMapping("/chat")
    public String chat(Principal principal, Authentication auth, Model model) {
        model.addAttribute("username", principal.getName());
        model.addAttribute("auxiliar", auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AUXILIAR")));
        return "chat/index";
    }

    /** chatId es null cuando el mensaje abre una conversación nueva. */
    public record MensajeRequest(Long chatId, String content) { }

    public record MensajeResponse(long chatId, String content) { }

    public record ChatDetail(long id, String title, List<StoredMessage> messages) { }

    public record ErrorResponse(String error) { }

    /** Historial del usuario, la conversación más reciente primero. */
    @GetMapping("/chat/api/chats")
    @ResponseBody
    public List<Chat> listChats(Principal principal) {
        return chats.listChats(currentUserId(principal));
    }

    /** Mensajes de una conversación. 404 si no existe o no es del usuario. */
    @GetMapping("/chat/api/chats/{id}")
    @ResponseBody
    public ResponseEntity<?> openChat(@PathVariable long id, Principal principal) {
        Optional<Chat> chat = chats.findChat(id, currentUserId(principal));
        if (chat.isEmpty()) return notFound();
        return ResponseEntity.ok(new ChatDetail(chat.get().id(), chat.get().title(), chats.messages(id)));
    }

    @PostMapping(value = "/chat/api/mensajes", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> enviarMensaje(@RequestBody MensajeRequest request, Principal principal) {
        if (request == null || request.content() == null || request.content().isBlank()) {
            return badRequest("Escribe un mensaje antes de enviarlo.");
        }
        String content = request.content().strip();
        if (content.length() > MAX_CONTENT_LENGTH) {
            return badRequest("El mensaje es demasiado largo (máximo " + MAX_CONTENT_LENGTH + " caracteres).");
        }

        long userId = currentUserId(principal);
        List<ChatMessage> contexto = new ArrayList<>();
        if (request.chatId() != null) {
            if (chats.findChat(request.chatId(), userId).isEmpty()) return notFound();
            for (StoredMessage m : chats.recentMessages(request.chatId(), ChatRepository.MAX_CONTEXT_MESSAGES)) {
                contexto.add(new ChatMessage(m.role(), m.content()));
            }
        }
        contexto.add(new ChatMessage("user", content));

        try {
            String respuesta = chatService.responder(contexto);
            // Se guarda solo si la IA respondió: un fallo no deja mensajes huérfanos ni chats vacíos.
            long chatId = chats.saveExchange(userId, request.chatId(), titleFrom(content), content, respuesta);
            return ResponseEntity.ok(new MensajeResponse(chatId, respuesta));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (AiException e) {
            return ResponseEntity.status(e.status()).body(new ErrorResponse(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Ocurrió un error inesperado. Intenta de nuevo."));
        }
    }

    private long currentUserId(Principal principal) {
        return users.findByLogin(principal.getName())
                .map(User::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    /** Título de la conversación: el primer mensaje, en una línea y recortado. */
    private static String titleFrom(String content) {
        String oneLine = content.replaceAll("\\s+", " ").strip();
        if (oneLine.length() <= MAX_TITLE_LENGTH) return oneLine;
        return oneLine.substring(0, MAX_TITLE_LENGTH - 1).stripTrailing() + "…";
    }

    private static ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }

    private static ResponseEntity<?> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("No encontramos esa conversación."));
    }
}
