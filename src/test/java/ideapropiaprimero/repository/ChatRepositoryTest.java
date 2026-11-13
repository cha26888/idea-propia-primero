package ideapropiaprimero.repository;

import static org.junit.jupiter.api.Assertions.*;
import ideapropiaprimero.model.TutorMode;
import java.nio.file.Path;
import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ChatRepositoryTest {
    @TempDir Path directory;
    private Path database(boolean legacy) throws Exception {
        Path db = directory.resolve("chats.db");
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + db); var s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE users(id INTEGER PRIMARY KEY)");
            s.executeUpdate("INSERT INTO users(id) VALUES(1),(2)");
            if (legacy) {
                s.executeUpdate("CREATE TABLE chats(id INTEGER PRIMARY KEY, user_id INTEGER NOT NULL, "
                        + "title TEXT NOT NULL, created_at TEXT DEFAULT CURRENT_TIMESTAMP, "
                        + "updated_at TEXT DEFAULT CURRENT_TIMESTAMP)");
                s.executeUpdate("INSERT INTO chats(id,user_id,title) VALUES(7,1,'Anterior')");
            }
        }
        return db;
    }
    @Test void migraSinBorrarChatsYSePuedeReabrir() throws Exception {
        Path db = database(true);
        var repository = new ChatRepository(db.toString());
        assertEquals(TutorMode.PRACTICE, repository.findChat(7, 1).orElseThrow().mode());
        assertTrue(repository.updateMode(7, 1, TutorMode.CHALLENGE));
        var reopened = new ChatRepository(db.toString());
        assertEquals(TutorMode.CHALLENGE, reopened.findChat(7, 1).orElseThrow().mode());
        assertEquals("Anterior", reopened.findChat(7, 1).orElseThrow().title());
    }
    @Test void guardaParesYModoConAislamientoYRollback() throws Exception {
        var repository = new ChatRepository(database(false).toString());
        long id = repository.saveExchange(1, null, "Tema", "Pregunta", "Respuesta", TutorMode.UNDERSTAND);
        assertEquals(TutorMode.UNDERSTAND, repository.listChats(1).get(0).mode());
        assertFalse(repository.updateMode(id, 2, TutorMode.CHALLENGE));
        assertTrue(repository.findChat(id, 2).isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> repository.saveExchange(2, id, "Otro", "u", "a", TutorMode.CHALLENGE));
        assertEquals(2, repository.messages(id).size());
        assertEquals(TutorMode.UNDERSTAND, repository.findChat(id, 1).orElseThrow().mode());
        assertThrows(IllegalStateException.class,
                () -> repository.saveExchange(1, id, "Tema", "nuevo", null, TutorMode.CHALLENGE));
        assertEquals(2, repository.messages(id).size());
        assertEquals(TutorMode.UNDERSTAND, repository.findChat(id, 1).orElseThrow().mode());
    }
    @Test void conservaParInicialYVentanaRecienteSinDuplicarlos() throws Exception {
        var repository = new ChatRepository(database(false).toString());
        long id = repository.saveExchange(1, null, "Tema", "u0", "a0");
        assertEquals(2, repository.contextMessages(id).size());
        for (int i = 1; i < 15; i++) repository.saveExchange(1, id, "Tema", "u" + i, "a" + i);
        var context = repository.contextMessages(id);
        assertEquals(20, context.size());
        assertEquals("u0", context.get(0).content());
        assertEquals("a0", context.get(1).content());
        assertEquals("u6", context.get(2).content());
        assertEquals("a14", context.get(19).content());
        for (int i = 0; i < context.size(); i++) {
            assertEquals(i % 2 == 0 ? "user" : "assistant", context.get(i).role());
        }
    }
    @Test void buscarYRenombrarNuncaAlcanzaChatsAjenos() throws Exception {
        var repository=new ChatRepository(database(false).toString());long id=repository.saveExchange(1,null,"Matrices","Proyecto de matrices","Pista");
        repository.saveExchange(2,null,"Matrices privadas","Secreto de matrices","Respuesta");
        assertEquals(1,repository.search(1,"matrices").size());assertEquals(id,repository.search(1,"matrices").get(0).id());
        assertTrue(repository.search(1,"Secreto").isEmpty());assertFalse(repository.rename(2,id,"Ajeno"));assertTrue(repository.rename(1,id,"Álgebra"));
        assertEquals("Álgebra",repository.findChat(id,1).orElseThrow().title());
    }
}
