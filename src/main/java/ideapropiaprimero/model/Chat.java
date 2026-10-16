package ideapropiaprimero.model;

/**
 * Conversación guardada de un usuario. {@code updatedAt} llega de SQLite en UTC
 * con formato "yyyy-MM-dd HH:mm:ss".
 */
public record Chat(long id, String title, String updatedAt) { }
