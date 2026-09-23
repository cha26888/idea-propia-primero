package ideapropiaprimero.integration.ai;

import java.util.List;

public interface AiClient {
    String responder(List<ChatMessage> messages);
}
