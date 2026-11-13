package ideapropiaprimero.model;

/** Conversación del usuario. updatedAt es UTC de SQLite. */
public record Chat(long id, String title, String updatedAt, TutorMode mode) { }
