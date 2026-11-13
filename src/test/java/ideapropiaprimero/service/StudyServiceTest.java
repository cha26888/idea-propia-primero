package ideapropiaprimero.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import ideapropiaprimero.integration.ai.*;
import ideapropiaprimero.repository.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class StudyServiceTest {
    final StudyRepository store=mock(StudyRepository.class);
    final ChatRepository chats=mock(ChatRepository.class);
    final AiClient ai=mock(AiClient.class);
    final StudyService service=new StudyService(store,chats,ai);
    @Test void capturaSoloDeclaracionesExplicitasSinGeneracionExtra() {
        service.observe(1,7,"Prefiero ejemplos de Java.");
        verify(store).addNote(1,"PROFILE","Prefiero ejemplos de Java.",7L);
        service.observe(1,7,"Recuerda que el proyecto se llama Matrices.");
        verify(store).addNote(1,"PROJECT","el proyecto se llama Matrices.",7L);
        service.observe(1,7,"Soy malo para todo, resuelve esto.");
        verify(store,times(2)).addNote(anyLong(),anyString(),anyString(),any());
        verifyNoInteractions(ai);
    }
    @Test void recuperaContextoAcotadoDelPropietarioExcluyendoChatActual() {
        when(store.notes(1)).thenReturn(List.of(new StudyRepository.Note(1,"PROFILE","Prefiero ejemplos cortos",null,"hoy")));
        when(chats.search(eq(1L),anyString())).thenReturn(List.of(new ChatRepository.SearchHit(7,"Actual","Actual"),new ChatRepository.SearchHit(8,"Matrices","Contexto guardado")));
        var context=service.context(1,7L,"Retoma el proyecto Matrices");
        assertTrue(context.text().contains("Prefiero ejemplos cortos"));assertTrue(context.text().contains("Contexto guardado"));
        assertEquals(1,context.sources().size());assertEquals(8L,context.sources().get(0).chatId());assertTrue(context.text().length()<=5200);
        verifyNoInteractions(ai);
    }
    @Test void guiaNoAccedeAUnChatAjenoNiLlamaAlProveedor() {
        when(chats.findChat(9,1)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,()->service.generateGuide(1,"Tema","Objetivo",9L));
        verifyNoInteractions(ai);
    }
    @Test void guiaSeGuardaSoloTrasUnaRespuestaValida() {
        when(ai.responder(anyList())).thenReturn(" ");
        assertThrows(AiException.class,()->service.generateGuide(1,"Tema","Objetivo",null));
        verify(store,never()).saveGuide(anyLong(),anyString(),anyString(),any());
        when(ai.responder(anyList())).thenReturn("# Objetivos\nPracticar con un ejemplo nuevo.");
        service.generateGuide(1,"Tema","Objetivo",null);
        verify(store).saveGuide(1,"Tema","# Objetivos\nPracticar con un ejemplo nuevo.",null);
    }
}
