package ideapropiaprimero.controller;

import ideapropiaprimero.integration.ai.AiException;
import ideapropiaprimero.integration.ai.ChatMessage;
import ideapropiaprimero.service.ChatService;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * {@link ChatService#responder(List)}.
 */
@Controller
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/chat")
    public String chat(Principal principal, Authentication auth, Model model) {
        model.addAttribute("username", principal.getName());
        model.addAttribute("auxiliar", auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AUXILIAR")));
        return "chat/index";
    }
 
    public record MensajeRequest(List<ChatMessage> historial) { }

    public record MensajeResponse(String content) { }

    public record ErrorResponse(String error) { }

    @PostMapping(value = "/chat/api/mensajes", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> enviarMensaje(@RequestBody MensajeRequest request) {
        try {
            String respuesta = chatService.responder(request == null ? null : request.historial());
            return ResponseEntity.ok(new MensajeResponse(respuesta));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (AiException e) {
            return ResponseEntity.status(e.status()).body(new ErrorResponse(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Ocurrió un error inesperado. Intenta de nuevo."));
        }
    }
}
