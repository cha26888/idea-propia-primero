package ideapropiaprimero.controller;

import ideapropiaprimero.integration.ai.AiException;
import ideapropiaprimero.integration.ai.ChatMessage;
import ideapropiaprimero.model.Chat;
import ideapropiaprimero.model.StoredMessage;
import ideapropiaprimero.model.User;
import ideapropiaprimero.model.TutorMode;
import ideapropiaprimero.repository.ChatRepository;
import ideapropiaprimero.repository.UserRepository;
import ideapropiaprimero.repository.ShareRepository;
import ideapropiaprimero.service.ChatService;
import ideapropiaprimero.service.ChatTitles;
import ideapropiaprimero.service.StudyService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.server.ResponseStatusException;

/**
 * Sirve el ChatView y su API. El historial se guarda en SQLite (ChatRepository) y
 * el servidor es la fuente de verdad: el navegador solo manda el mensaje nuevo y el
 * id de la conversación, y el contexto que recibe la IA se lee de la base de datos.
 */
@Controller
public class ChatController {

    private static final int MAX_CONTENT_LENGTH = 4000;

    private final ChatService chatService;
    private final ChatRepository chats;
    private final UserRepository users;
    private final StudyService studies;
    private final ShareRepository shares;

    public ChatController(ChatService chatService, ChatRepository chats, UserRepository users, StudyService studies, ShareRepository shares) {
        this.chatService = chatService;
        this.chats = chats;
        this.users = users;
        this.studies = studies;
        this.shares = shares;
    }

    @GetMapping("/chat")
    public String chat(Principal principal, Authentication auth, Model model) {
        model.addAttribute("username", principal.getName());
        model.addAttribute("auxiliar", auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AUXILIAR")));
        return "chat/index";
    }

    /** chatId es null cuando el mensaje abre una conversación nueva. */
    public record MensajeRequest(Long chatId, String content, String mode) { }

    public record MensajeResponse(long chatId, String content, TutorMode mode) { }

    public record ChatDetail(long id, String title, TutorMode mode, List<StoredMessage> messages) { }

    public record ModeRequest(String mode) { }
    public record ModeResponse(TutorMode mode) { }

    public record ErrorResponse(String error) { }

    /** Historial del usuario, la conversación más reciente primero. */
    @GetMapping("/chat/api/chats")
    @ResponseBody
    public List<Chat> listChats(Principal principal) {
        return chats.listChats(currentUserId(principal));
    }

    @GetMapping("/chat/api/search") @ResponseBody
    public List<ChatRepository.SearchHit> search(@RequestParam String q, Principal principal) {
        return chats.search(currentUserId(principal), q);
    }
    public record TitleRequest(String title) { }
    @PostMapping("/chat/api/chats/{id}/title") @ResponseBody
    public ResponseEntity<?> rename(@PathVariable long id,@RequestBody TitleRequest request,Principal principal) {
        try {
            String title=StudyService.text(request==null?null:request.title(),80,false).replaceAll("\\s+"," ");
            if(!chats.rename(currentUserId(principal),id,title))return notFound();
            return ResponseEntity.ok(java.util.Map.of("title",title));
        } catch(IllegalArgumentException e){return badRequest(e.getMessage());}
    }
    @PostMapping("/chat/api/chats/{id}/share") @ResponseBody
    public ResponseEntity<?> share(@PathVariable long id,Principal principal) {
        long user=currentUserId(principal);
        if(chats.findChat(id,user).isEmpty())return notFound();
        return ResponseEntity.ok(java.util.Map.of("path","/share/"+shares.create(user,id)));
    }
    @DeleteMapping("/chat/api/chats/{id}/share") @ResponseBody
    public ResponseEntity<?> unshare(@PathVariable long id,Principal principal) {
        long user=currentUserId(principal);if(chats.findChat(id,user).isEmpty())return notFound();
        shares.revoke(user,id);return ResponseEntity.ok(java.util.Map.of("revoked",true));
    }

    /** Mensajes de una conversación. 404 si no existe o no es del usuario. */
    @GetMapping("/chat/api/chats/{id}")
    @ResponseBody
    public ResponseEntity<?> openChat(@PathVariable long id, Principal principal) {
        Optional<Chat> chat = chats.findChat(id, currentUserId(principal));
        if (chat.isEmpty()) return notFound();
        return ResponseEntity.ok(new ChatDetail(chat.get().id(), chat.get().title(), chat.get().mode(), chats.messages(id)));
    }

    @PostMapping(value = "/chat/api/chats/{id}/mode", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> changeMode(@PathVariable long id, @RequestBody ModeRequest request, Principal principal) {
        try {
            TutorMode mode = TutorMode.parse(request == null ? null : request.mode());
            if (!chats.updateMode(id, currentUserId(principal), mode)) return notFound();
            return ResponseEntity.ok(new ModeResponse(mode));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
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
        TutorMode mode = TutorMode.PRACTICE;
        if (request.chatId() != null) {
            Optional<Chat> existing = chats.findChat(request.chatId(), userId);
            if (existing.isEmpty()) return notFound();
            mode = existing.get().mode();
            for (StoredMessage m : chats.contextMessages(request.chatId())) {
                contexto.add(new ChatMessage(m.role(), m.content()));
            }
        }
        contexto.add(new ChatMessage("user", content));

        try {
            if (request.mode() != null) mode = TutorMode.parse(request.mode());
            var support = studies.context(userId, request.chatId(), content);
            String respuesta = support.text().isBlank() ? chatService.responder(contexto, mode)
                    : chatService.responder(contexto, mode, support.text());
            if(!support.sources().isEmpty())respuesta=studies.appendSources(respuesta,support);
            // Se guarda solo si la IA respondió: un fallo no deja mensajes huérfanos ni chats vacíos.
            long chatId = chats.saveExchange(userId, request.chatId(), titleFrom(content), content, respuesta, mode);
            try { studies.observe(userId,chatId,content); }
            catch(RuntimeException e) { java.util.logging.Logger.getLogger(ChatController.class.getName()).warning("El chat se guardó, pero no se pudo actualizar la memoria automática."); }
            return ResponseEntity.ok(new MensajeResponse(chatId, respuesta, mode));
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
        return ChatTitles.from(content);
    }

    private static ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }

    private static ResponseEntity<?> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("No encontramos esa conversación."));
    }
}
