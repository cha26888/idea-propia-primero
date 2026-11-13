package ideapropiaprimero.repository;

import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/** Datos privados de estudio. Todas las lecturas y mutaciones incluyen al propietario. */
@Repository
public class StudyRepository {
    public record Note(long id, String kind, String content, Long sourceChatId, String updatedAt) { }
    public record Task(long id, String title, String course, String dueDate, boolean done, String sourceUid) { }
    public record Journal(long id, String day, String learned, String difficult, String nextStep) { }
    public record Guide(long id, String title, String content, Long sourceChatId, String createdAt) { }
    public record DailyAttempt(String day, String exerciseId, int choice, String reason, boolean correct, String feedback) { }
    public record CalendarTask(String uid, String title, String day) { }
    private final String url;

    public StudyRepository(@Value("${app.database.path:data/idea-propia-primero.db}") String path) {
        try {
            Path file = Path.of(path).toAbsolutePath();
            Files.createDirectories(file.getParent());
            url = "jdbc:sqlite:" + file;
            try (var c = connect(); var s = c.createStatement()) {
                s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS study_notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL,
                        kind TEXT NOT NULL CHECK(kind IN ('PROFILE','PROJECT')), content TEXT NOT NULL,
                        source_chat_id INTEGER, updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE(user_id,kind,content), FOREIGN KEY(user_id) REFERENCES users(id),
                        FOREIGN KEY(source_chat_id) REFERENCES chats(id) ON DELETE SET NULL
                    )
                    """);
                s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS study_tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL,
                        title TEXT NOT NULL, course TEXT NOT NULL DEFAULT '', due_date TEXT NOT NULL,
                        done INTEGER NOT NULL DEFAULT 0 CHECK(done IN (0,1)), source_uid TEXT,
                        UNIQUE(user_id,source_uid), FOREIGN KEY(user_id) REFERENCES users(id)
                    )
                    """);
                s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS study_journal (
                        id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL, day TEXT NOT NULL,
                        learned TEXT NOT NULL, difficult TEXT NOT NULL, next_step TEXT NOT NULL,
                        UNIQUE(user_id,day), FOREIGN KEY(user_id) REFERENCES users(id)
                    )
                    """);
                s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS study_guides (
                        id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL,
                        title TEXT NOT NULL, content TEXT NOT NULL, source_chat_id INTEGER,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY(user_id) REFERENCES users(id),
                        FOREIGN KEY(source_chat_id) REFERENCES chats(id) ON DELETE SET NULL
                    )
                    """);
                s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS daily_attempts (
                        user_id INTEGER NOT NULL, day TEXT NOT NULL, exercise_id TEXT NOT NULL,
                        choice INTEGER NOT NULL, reason TEXT NOT NULL, correct INTEGER NOT NULL CHECK(correct IN (0,1)),
                        feedback TEXT NOT NULL, PRIMARY KEY(user_id,day), FOREIGN KEY(user_id) REFERENCES users(id)
                    )
                    """);
                s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_notes_owner ON study_notes(user_id,kind,id)");
                s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_tasks_owner_date ON study_tasks(user_id,done,due_date)");
                s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_guides_owner ON study_guides(user_id,id)");
            }
        } catch (Exception e) { throw new IllegalStateException("No se pudo iniciar el espacio de estudio.", e); }
    }

    private Connection connect() throws SQLException {
        var c = DriverManager.getConnection(url);
        try (var s = c.createStatement()) {
            s.execute("PRAGMA busy_timeout=5000"); s.execute("PRAGMA foreign_keys=ON");
        } catch (SQLException e) { c.close(); throw e; }
        return c;
    }
    @FunctionalInterface private interface Mapper<T> { T read(ResultSet r) throws SQLException; }
    private <T> List<T> select(String sql, Mapper<T> mapper, Object... args) {
        try (var c = connect(); var s = c.prepareStatement(sql)) {
            bind(s, args); var list = new ArrayList<T>();
            try (var r = s.executeQuery()) { while (r.next()) list.add(mapper.read(r)); }
            return list;
        } catch (SQLException e) { throw new IllegalStateException("No se pudieron consultar los datos de estudio.", e); }
    }
    private int update(String sql, Object... args) {
        try (var c = connect(); var s = c.prepareStatement(sql)) {
            bind(s, args); return s.executeUpdate();
        } catch (SQLException e) { throw new IllegalStateException("No se pudieron guardar los datos de estudio.", e); }
    }
    private static void bind(PreparedStatement s, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) s.setObject(i + 1, args[i]);
    }
    private static Long nullableLong(ResultSet r, String name) throws SQLException {
        long value = r.getLong(name); return r.wasNull() ? null : value;
    }

    public List<Note> notes(long user) {
        return select("SELECT * FROM study_notes WHERE user_id=? ORDER BY id DESC LIMIT 200",
                r -> new Note(r.getLong("id"), r.getString("kind"), r.getString("content"),
                        nullableLong(r,"source_chat_id"), r.getString("updated_at")), user);
    }
    public void addNote(long user, String kind, String content, Long source) {
        update("""
                INSERT INTO study_notes(user_id,kind,content,source_chat_id)
                SELECT ?,?,?,? WHERE ? IS NULL OR EXISTS(SELECT 1 FROM chats WHERE id=? AND user_id=?)
                ON CONFLICT(user_id,kind,content) DO NOTHING
                """, user, kind, content, source, source, source, user);
    }
    public boolean editNote(long user, long id, String content) {
        return update("UPDATE OR IGNORE study_notes SET content=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND user_id=?", content,id,user)==1;
    }
    public boolean deleteNote(long user, long id) { return update("DELETE FROM study_notes WHERE id=? AND user_id=?",id,user)==1; }

    public List<Task> tasks(long user) {
        return select("SELECT * FROM study_tasks WHERE user_id=? ORDER BY done,due_date,id LIMIT 500",
                r -> new Task(r.getLong("id"),r.getString("title"),r.getString("course"),r.getString("due_date"),r.getBoolean("done"),r.getString("source_uid")),user);
    }
    public void addTask(long user, String title, String course, String day) {
        update("INSERT INTO study_tasks(user_id,title,course,due_date) VALUES(?,?,?,?)",user,title,course,day);
    }
    public boolean completeTask(long user,long id,boolean done) {
        return update("UPDATE study_tasks SET done=? WHERE id=? AND user_id=?",done?1:0,id,user)==1;
    }
    public boolean deleteTask(long user,long id) { return update("DELETE FROM study_tasks WHERE id=? AND user_id=?",id,user)==1; }
    public int importTasks(long user,String course,List<CalendarTask> events) {
        try (var c=connect()) {
            c.setAutoCommit(false);
            try (var s=c.prepareStatement("""
                    INSERT INTO study_tasks(user_id,title,course,due_date,source_uid) VALUES(?,?,?,?,?)
                    ON CONFLICT(user_id,source_uid) DO UPDATE SET title=excluded.title,course=excluded.course,due_date=excluded.due_date
                    """)) {
                for (var event:events) { bind(s,user,event.title(),course,event.day(),event.uid()); s.executeUpdate(); }
                c.commit(); return events.size();
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
        } catch (SQLException e) { throw new IllegalStateException("No se pudo importar el calendario.",e); }
    }

    public List<Journal> journal(long user) {
        return select("SELECT * FROM study_journal WHERE user_id=? ORDER BY day DESC LIMIT 100",
                r -> new Journal(r.getLong("id"),r.getString("day"),r.getString("learned"),r.getString("difficult"),r.getString("next_step")),user);
    }
    public void saveJournal(long user,String day,String learned,String difficult,String next) {
        update("""
                INSERT INTO study_journal(user_id,day,learned,difficult,next_step) VALUES(?,?,?,?,?)
                ON CONFLICT(user_id,day) DO UPDATE SET learned=excluded.learned,difficult=excluded.difficult,next_step=excluded.next_step
                """,user,day,learned,difficult,next);
    }
    public boolean deleteJournal(long user,long id) { return update("DELETE FROM study_journal WHERE id=? AND user_id=?",id,user)==1; }

    public List<Guide> guides(long user) {
        return select("SELECT * FROM study_guides WHERE user_id=? ORDER BY id DESC LIMIT 50",
                r -> new Guide(r.getLong("id"),r.getString("title"),r.getString("content"),nullableLong(r,"source_chat_id"),r.getString("created_at")),user);
    }
    public void saveGuide(long user,String title,String content,Long source) {
        update("""
                INSERT INTO study_guides(user_id,title,content,source_chat_id)
                SELECT ?,?,?,? WHERE ? IS NULL OR EXISTS(SELECT 1 FROM chats WHERE id=? AND user_id=?)
                """,user,title,content,source,source,source,user);
    }
    public boolean deleteGuide(long user,long id) { return update("DELETE FROM study_guides WHERE id=? AND user_id=?",id,user)==1; }
    public Optional<DailyAttempt> daily(long user,String day) {
        return select("SELECT * FROM daily_attempts WHERE user_id=? AND day=?",
                r -> new DailyAttempt(r.getString("day"),r.getString("exercise_id"),r.getInt("choice"),r.getString("reason"),r.getBoolean("correct"),r.getString("feedback")),user,day).stream().findFirst();
    }
    public void saveDaily(long user,DailyAttempt attempt) {
        update("""
                INSERT INTO daily_attempts(user_id,day,exercise_id,choice,reason,correct,feedback) VALUES(?,?,?,?,?,?,?)
                ON CONFLICT(user_id,day) DO NOTHING
                """,user,attempt.day(),attempt.exerciseId(),attempt.choice(),attempt.reason(),attempt.correct()?1:0,attempt.feedback());
    }
    public List<String> practiceDays(long user) {
        return select("SELECT day FROM daily_attempts WHERE user_id=? ORDER BY day DESC LIMIT 400",r->r.getString("day"),user);
    }
}
