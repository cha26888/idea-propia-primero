package ideapropiaprimero.model;

/** Mensaje guardado dentro de una conversación. {@code role} es "user" o "assistant". */
public record StoredMessage(String role, String content, String createdAt) { }
