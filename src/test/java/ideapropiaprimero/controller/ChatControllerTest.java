package ideapropiaprimero.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import ideapropiaprimero.integration.ai.AiException;
import ideapropiaprimero.model.Chat;
import ideapropiaprimero.model.TutorMode;
import ideapropiaprimero.model.User;
import ideapropiaprimero.repository.ChatRepository;
import ideapropiaprimero.repository.UserRepository;
import ideapropiaprimero.service.ChatService;
import ideapropiaprimero.service.StudyService;
import ideapropiaprimero.service.CanvasCalendarService;
import ideapropiaprimero.repository.ShareRepository;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChatControllerTest {
    final ChatService service = mock(ChatService.class);
    final ChatRepository chats = mock(ChatRepository.class);
    final UserRepository users = mock(UserRepository.class);
    final StudyService studies = mock(StudyService.class);
    final ShareRepository shares = mock(ShareRepository.class);
    final ChatController controller = new ChatController(service, chats, users, studies, shares, mock(CanvasCalendarService.class));
    final Principal principal = () -> "student";
    @BeforeEach void account() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(users.findByLogin("student")).thenReturn(Optional.of(user));
        when(studies.context(anyLong(), any(), anyString())).thenReturn(new StudyService.Context("",List.of()));
    }
    @Test void unClienteAnteriorRecuperaElModoGuardado() {
        when(chats.findChat(7, 1)).thenReturn(Optional.of(new Chat(7, "Tema", "fecha", TutorMode.CHALLENGE)));
        when(chats.contextMessages(7)).thenReturn(List.of());
        when(service.responder(anyList(), eq(TutorMode.CHALLENGE))).thenReturn("Pista");
        when(chats.saveExchange(eq(1L), eq(7L), anyString(), anyString(), eq("Pista"), eq(TutorMode.CHALLENGE))).thenReturn(7L);
        var response = controller.enviarMensaje(new ChatController.MensajeRequest(7L, "Pregunta", null), principal);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(TutorMode.CHALLENGE, ((ChatController.MensajeResponse) response.getBody()).mode());
    }
    @Test void rechazaModoInvalidoYChatsAjenosAntesDeLlamarALaIa() {
        assertEquals(400, controller.enviarMensaje(
                new ChatController.MensajeRequest(null, "Pregunta", "invalid"), principal).getStatusCode().value());
        when(chats.findChat(7, 1)).thenReturn(Optional.empty());
        assertEquals(404, controller.enviarMensaje(
                new ChatController.MensajeRequest(7L, "Pregunta", "UNDERSTAND"), principal).getStatusCode().value());
        verifyNoInteractions(service);
    }
    @Test void unErrorDeIaNoGuardaMensajesNiModo() {
        when(service.responder(anyList(), any())).thenThrow(new AiException(504, "Timeout"));
        assertEquals(504, controller.enviarMensaje(
                new ChatController.MensajeRequest(null, "Pregunta", "UNDERSTAND"), principal).getStatusCode().value());
        verify(chats, never()).saveExchange(anyLong(), any(), anyString(), anyString(), anyString(), any());
    }
    @Test void cambiarModoExigePropiedadYNoLlamaALaIa() {
        when(chats.updateMode(7, 1, TutorMode.UNDERSTAND)).thenReturn(false);
        assertEquals(404, controller.changeMode(7,
                new ChatController.ModeRequest("UNDERSTAND"), principal).getStatusCode().value());
        assertEquals(400, controller.changeMode(7,
                new ChatController.ModeRequest("ADMIN"), principal).getStatusCode().value());
        verifyNoInteractions(service);
    }
    @Test void agendaSeRespondeYGuardaSinLlamarALaIa() {
        when(studies.scheduleReply(eq(1L),anyString())).thenReturn(Optional.of("Agenda del día: entrega de POO"));
        when(chats.saveExchange(eq(1L),isNull(),anyString(),anyString(),anyString(),any())).thenReturn(8L);
        var result=controller.enviarMensaje(new ChatController.MensajeRequest(null,"Dime qué pendientes tengo hoy","PRACTICE"),principal);
        assertEquals(200,result.getStatusCode().value());assertTrue(((ChatController.MensajeResponse)result.getBody()).content().contains("entrega de POO"));
        verifyNoInteractions(service);verify(studies,never()).context(anyLong(),any(),anyString());
    }
}
