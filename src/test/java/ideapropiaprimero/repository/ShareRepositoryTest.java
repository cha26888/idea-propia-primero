package ideapropiaprimero.repository;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ShareRepositoryTest {
    @TempDir Path directory;
    @Test void comparteCopiaAisladaRevocableSinExponerMensajesFuturos() throws Exception {
        String path=directory.resolve("share.db").toString();
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+path);var s=c.createStatement()){s.execute("CREATE TABLE users(id INTEGER PRIMARY KEY)");s.execute("INSERT INTO users VALUES(1),(2)");}
        var chats=new ChatRepository(path);var shares=new ShareRepository(path);long chat=chats.saveExchange(1,null,"Álgebra","u1","a1");
        String token=shares.create(1,chat);assertEquals(43,token.length());assertEquals(2,shares.find(token).orElseThrow().messages().size());
        chats.saveExchange(1,chat,"Álgebra","privado después","otra respuesta");assertEquals(2,shares.find(token).orElseThrow().messages().size());
        assertThrows(IllegalArgumentException.class,()->shares.create(2,chat));assertFalse(shares.revoke(2,chat));assertTrue(shares.find(token).isPresent());
        String replacement=shares.create(1,chat);assertTrue(shares.find(token).isEmpty());assertEquals(4,shares.find(replacement).orElseThrow().messages().size());
        assertTrue(shares.revoke(1,chat));assertTrue(shares.find(replacement).isEmpty());assertTrue(shares.find("inventado").isEmpty());
    }
    @Test void elEnlaceCaduca() throws Exception {
        String path=directory.resolve("expiry.db").toString();
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+path);var s=c.createStatement()){s.execute("CREATE TABLE users(id INTEGER PRIMARY KEY)");s.execute("INSERT INTO users VALUES(1)");}
        var chats=new ChatRepository(path);var shares=new ShareRepository(path);String token=shares.create(1,chats.saveExchange(1,null,"Tema","u","a"));
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+path);var s=c.createStatement()){s.execute("UPDATE chat_shares SET expires_at='2000-01-01'");}
        assertTrue(shares.find(token).isEmpty());
    }
}
