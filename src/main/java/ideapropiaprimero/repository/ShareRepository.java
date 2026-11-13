package ideapropiaprimero.repository;

import ideapropiaprimero.model.StoredMessage;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/** Copia pública revocable. El enlace no expone cambios futuros ni datos del perfil. */
@Repository
public class ShareRepository {
    public record SharedChat(String title,String createdAt,List<StoredMessage> messages) { }
    private final String url;
    private final SecureRandom random=new SecureRandom();
    public ShareRepository(@Value("${app.database.path:data/idea-propia-primero.db}") String path) {
        try {
            var file=Path.of(path).toAbsolutePath();Files.createDirectories(file.getParent());url="jdbc:sqlite:"+file;
            try(var c=connect();var s=c.createStatement()) {
                s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS chat_shares (
                        token_hash TEXT PRIMARY KEY, chat_id INTEGER NOT NULL UNIQUE, user_id INTEGER NOT NULL,
                        title TEXT NOT NULL, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        expires_at TEXT NOT NULL DEFAULT (datetime('now','+30 days')),
                        FOREIGN KEY(chat_id) REFERENCES chats(id) ON DELETE CASCADE,
                        FOREIGN KEY(user_id) REFERENCES users(id)
                    )
                    """);
                s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS shared_messages (
                        token_hash TEXT NOT NULL, position INTEGER NOT NULL, role TEXT NOT NULL,
                        content TEXT NOT NULL, created_at TEXT NOT NULL,
                        PRIMARY KEY(token_hash,position),
                        FOREIGN KEY(token_hash) REFERENCES chat_shares(token_hash) ON DELETE CASCADE
                    )
                    """);
            }
        } catch(Exception e) {throw new IllegalStateException("No se pudo iniciar el sistema de enlaces.",e);}
    }
    private Connection connect() throws SQLException {
        var c=DriverManager.getConnection(url);
        try(var s=c.createStatement()){s.execute("PRAGMA busy_timeout=5000");s.execute("PRAGMA foreign_keys=ON");}
        catch(SQLException e){c.close();throw e;}return c;
    }
    private static String hash(String token) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    public String create(long user,long chat) {
        byte[] bytes=new byte[32];random.nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);String hash=hash(token);
        try(var c=connect()) {
            c.setAutoCommit(false);
            try {
                String title;
                try(var s=c.prepareStatement("SELECT title FROM chats WHERE id=? AND user_id=?")) {
                    s.setLong(1,chat);s.setLong(2,user);
                    try(var r=s.executeQuery()){if(!r.next())throw new IllegalArgumentException("No encontramos ese chat.");title=r.getString(1);}
                }
                try(var s=c.prepareStatement("DELETE FROM chat_shares WHERE chat_id=? AND user_id=?")){s.setLong(1,chat);s.setLong(2,user);s.executeUpdate();}
                try(var s=c.prepareStatement("INSERT INTO chat_shares(token_hash,chat_id,user_id,title) VALUES(?,?,?,?)")) {
                    s.setString(1,hash);s.setLong(2,chat);s.setLong(3,user);s.setString(4,title);s.executeUpdate();
                }
                try(var s=c.prepareStatement("""
                        INSERT INTO shared_messages(token_hash,position,role,content,created_at)
                        SELECT ?,id,role,content,created_at FROM messages WHERE chat_id=? ORDER BY id
                        """)){s.setString(1,hash);s.setLong(2,chat);s.executeUpdate();}
                c.commit();return token;
            } catch(SQLException|RuntimeException e){c.rollback();throw e;}
        } catch(SQLException e){throw new IllegalStateException("No se pudo crear el enlace.",e);}
    }
    public boolean revoke(long user,long chat) {
        try(var c=connect();var s=c.prepareStatement("DELETE FROM chat_shares WHERE chat_id=? AND user_id=?")) {
            s.setLong(1,chat);s.setLong(2,user);return s.executeUpdate()>0;
        } catch(SQLException e){throw new IllegalStateException("No se pudo desactivar el enlace.",e);}
    }
    public Optional<SharedChat> find(String token) {
        if(token==null||!token.matches("[A-Za-z0-9_-]{43}"))return Optional.empty();
        String hash=hash(token);
        try(var c=connect()) {
            c.setAutoCommit(false);
            try {
                String title,created;
                try(var s=c.prepareStatement("SELECT title,created_at FROM chat_shares WHERE token_hash=? AND expires_at>CURRENT_TIMESTAMP")) {
                    s.setString(1,hash);try(var r=s.executeQuery()){if(!r.next()){c.commit();return Optional.empty();}title=r.getString(1);created=r.getString(2);}
                }
                var messages=new ArrayList<StoredMessage>();
                try(var s=c.prepareStatement("SELECT role,content,created_at FROM shared_messages WHERE token_hash=? ORDER BY position")) {
                    s.setString(1,hash);try(var r=s.executeQuery()){while(r.next())messages.add(new StoredMessage(r.getString(1),r.getString(2),r.getString(3)));}
                }
                c.commit();return Optional.of(new SharedChat(title,created,List.copyOf(messages)));
            } catch(SQLException|RuntimeException e){c.rollback();throw e;}
        } catch(SQLException e){throw new IllegalStateException("No se pudo abrir el enlace.",e);}
    }
}
