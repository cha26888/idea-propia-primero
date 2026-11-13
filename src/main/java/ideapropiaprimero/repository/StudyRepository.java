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
    public record Task(long id, String title, String course, String dueDate, boolean done, String sourceUid,
                       String kind, String dueTime, String sourceUrl) {
        public Task(long id,String title,String course,String dueDate,boolean done,String sourceUid) {
            this(id,title,course,dueDate,done,sourceUid,"TASK","","");
        }
    }
    /** URL privada: nunca serializar esta entidad en las respuestas HTTP ni enviarla al modelo. */
    public record CalendarFeed(String url,String course,String syncedAt,String attemptedAt,String error,int skipped) { }
    public record Journal(long id, String day, String learned, String difficult, String nextStep) { }
    public record Guide(long id, String title, String content, Long sourceChatId, String createdAt) { }
    public record DailyAttempt(String day, String exerciseId, int choice, String reason, boolean correct, String feedback) { }
    public record CalendarTask(String uid,String title,String day,String kind,String time,String link) {
        public CalendarTask(String uid,String title,String day){this(uid,title,day,"EVENT","","");}
    }
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
                // Migración aditiva. Los .ics antiguos no conservaban el tipo; requieren revisión/reimportación.
                boolean migrated=column(c,"study_tasks","kind","TEXT NOT NULL DEFAULT 'TASK'");
                column(c,"study_tasks","due_time","TEXT NOT NULL DEFAULT ''");
                column(c,"study_tasks","source_url","TEXT NOT NULL DEFAULT ''");
                column(c,"study_tasks","kind_override","TEXT");
                column(c,"study_tasks","feed_managed","INTEGER NOT NULL DEFAULT 0");
                column(c,"study_tasks","active","INTEGER NOT NULL DEFAULT 1");
                if(migrated)s.executeUpdate("UPDATE study_tasks SET kind='REVIEW' WHERE source_uid IS NOT NULL");
                s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS study_calendar_feeds (
                        user_id INTEGER PRIMARY KEY, url TEXT NOT NULL, course TEXT NOT NULL,
                        synced_at TEXT NOT NULL DEFAULT '', attempted_at TEXT NOT NULL DEFAULT '',
                        error TEXT NOT NULL DEFAULT '', skipped INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(user_id) REFERENCES users(id)
                    )
                    """);
                s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_notes_owner ON study_notes(user_id,kind,id)");
                s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_tasks_owner_date ON study_tasks(user_id,done,due_date)");
                s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_guides_owner ON study_guides(user_id,id)");
            }
        } catch (Exception e) { throw new IllegalStateException("No se pudo iniciar el espacio de estudio.", e); }
    }

    private static boolean column(Connection c,String table,String name,String definition) throws SQLException {
        try(var s=c.createStatement();var r=s.executeQuery("PRAGMA table_info("+table+")")) {
            while(r.next())if(name.equals(r.getString("name")))return false;
        }
        try(var s=c.createStatement()){s.executeUpdate("ALTER TABLE "+table+" ADD COLUMN "+name+" "+definition);}
        return true;
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
        return select("SELECT * FROM study_tasks WHERE user_id=? AND active=1 ORDER BY due_date,due_time,id",
                r -> new Task(r.getLong("id"),r.getString("title"),r.getString("course"),r.getString("due_date"),r.getBoolean("done"),r.getString("source_uid"),r.getString("kind_override")==null?r.getString("kind"):r.getString("kind_override"),r.getString("due_time"),r.getString("source_url")),user);
    }
    public void addTask(long user, String title, String course, String day) {
        update("INSERT INTO study_tasks(user_id,title,course,due_date) VALUES(?,?,?,?)",user,title,course,day);
    }
    public boolean completeTask(long user,long id,boolean done) {
        return update("UPDATE study_tasks SET done=? WHERE id=? AND user_id=?",done?1:0,id,user)==1;
    }
    public boolean deleteTask(long user,long id) { return update("DELETE FROM study_tasks WHERE id=? AND user_id=?",id,user)==1; }
    public boolean classifyTask(long user,long id,String kind) {
        if(!Set.of("TASK","EVENT").contains(kind))throw new IllegalArgumentException("Elige tarea o evento.");
        return update("UPDATE study_tasks SET kind_override=? WHERE user_id=? AND id=?",kind,user,id)==1;
    }
    public int importTasks(long user,String course,List<CalendarTask> events) {
        try(var c=connect()) {
            c.setAutoCommit(false);
            try {upsertCalendar(c,user,course,events,false);c.commit();return events.size();}
            catch(SQLException|RuntimeException e){c.rollback();throw e;}
        } catch(SQLException e){throw new IllegalStateException("No se pudo importar el calendario.",e);}
    }
    private static void upsertCalendar(Connection c,long user,String course,List<CalendarTask> events,boolean managed) throws SQLException {
        try(var s=c.prepareStatement("""
            INSERT INTO study_tasks(user_id,title,course,due_date,source_uid,kind,due_time,source_url,feed_managed,active)
            VALUES(?,?,?,?,?,?,?,?,?,1)
            ON CONFLICT(user_id,source_uid) DO UPDATE SET title=excluded.title,course=excluded.course,
            due_date=excluded.due_date,kind=excluded.kind,due_time=excluded.due_time,source_url=excluded.source_url,
            feed_managed=MAX(study_tasks.feed_managed,excluded.feed_managed),active=1
            """)) {
            for(var e:events){bind(s,user,e.title(),course,e.day(),e.uid(),e.kind(),e.time(),e.link(),managed?1:0);s.executeUpdate();}
        }
    }
    public Optional<CalendarFeed> calendarFeed(long user) {
        return select("SELECT * FROM study_calendar_feeds WHERE user_id=?",r->new CalendarFeed(r.getString("url"),r.getString("course"),r.getString("synced_at"),r.getString("attempted_at"),r.getString("error"),r.getInt("skipped")),user).stream().findFirst();
    }
    /** Reconciliación atómica: nunca deja un feed a medias; conserva marcas manuales por UID. */
    public void syncCalendar(long user,String feedUrl,String course,List<CalendarTask> events,int skipped,String now,boolean replace) {
        try(var c=connect()) {
            c.setAutoCommit(false);
            try {
                try(var s=c.prepareStatement(replace
                    ?"UPDATE study_tasks SET active=0 WHERE user_id=? AND source_uid IS NOT NULL"
                    :"UPDATE study_tasks SET active=0 WHERE user_id=? AND feed_managed=1")){bind(s,user);s.executeUpdate();}
                upsertCalendar(c,user,course,events,true);
                try(var s=c.prepareStatement("""
                    INSERT INTO study_calendar_feeds(user_id,url,course,synced_at,attempted_at,error,skipped) VALUES(?,?,?,?,?,'',?)
                    ON CONFLICT(user_id) DO UPDATE SET url=excluded.url,course=excluded.course,synced_at=excluded.synced_at,
                    attempted_at=excluded.attempted_at,error='',skipped=excluded.skipped
                    """)){bind(s,user,feedUrl,course,now,now,skipped);s.executeUpdate();}
                c.commit();
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}
        }catch(SQLException e){throw new IllegalStateException("No se pudo guardar la actualización del calendario.",e);}
    }
    public void calendarFailure(long user,String now,String message) {
        update("UPDATE study_calendar_feeds SET attempted_at=?,error=? WHERE user_id=?",now,message,user);
    }
    public void disconnectCalendar(long user) {
        // Se conserva una copia local de las fechas; la UI informa de que ya no se actualiza.
        update("DELETE FROM study_calendar_feeds WHERE user_id=?",user);
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
