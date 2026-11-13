package ideapropiaprimero.service;

import ideapropiaprimero.repository.StudyRepository.Task;
import java.text.Normalizer;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.regex.*;

/** Las consultas de fechas son datos de agenda; no requieren generación ni tokens. */
public final class StudyAgenda {
    private StudyAgenda() { }
    private static final DateTimeFormatter DISPLAY=DateTimeFormatter.ofPattern("dd/MM/uuuu");
    public static Optional<String> reply(List<Task> tasks,String input,LocalDate today) {
        String q=Normalizer.normalize(input.toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}","");
        boolean subject=q.matches("(?s).*\\b(pendientes?|entregas?|tareas?|agenda|calendario|eventos?|clases?)\\b.*");
        boolean request=q.matches("(?s).*\\b(que|cuales|dime|muestra|muestrame|lista|listar|ver|revisa|recuerdame|tengo|tenemos)\\b.*");
        if(!(request&&(subject||q.matches("(?s).*que tengo.*(hoy|manana|semana).*"))))return Optional.empty();
        // Una petición de resolver/explicar una tarea sigue siendo trabajo del tutor.
        if(q.matches("(?s).*\\b(resuelve|resolver|explica|explicame|ayudame a|como hago)\\b.*"))return Optional.empty();
        boolean past=q.matches("(?s).*\\b(vencidos?|vencidas?|atrasados?|atrasadas?)\\b.*");
        LocalDate start=today,end=today;
        var iso=Pattern.compile("\\b(\\d{4}-\\d{2}-\\d{2})\\b").matcher(q);
        var local=Pattern.compile("\\b(\\d{1,2})/(\\d{1,2})/(\\d{2}|\\d{4})\\b").matcher(q);
        try {
            if(iso.find())start=LocalDate.parse(iso.group(1));
            else if(local.find()){int year=Integer.parseInt(local.group(3));start=LocalDate.of(year<100?year+2000:year,Integer.parseInt(local.group(2)),Integer.parseInt(local.group(1)));}
            else if(q.contains("pasado manana"))start=today.plusDays(2);
            else if(q.contains("manana"))start=today.plusDays(1);
            else if(q.contains("ayer"))start=today.minusDays(1);
            end=start;
            if(q.contains("esta semana")){start=today;end=today.plusDays(7-today.getDayOfWeek().getValue());}
            else if(q.contains("semana")||q.contains("proxim")){start=today;end=today.plusDays(6);}
        }catch(DateTimeException e){return Optional.of("Esa fecha no es válida. Escríbela como día/mes/año.");}
        LocalDate from=start,to=end;
        var selected=tasks.stream().filter(t->past?LocalDate.parse(t.dueDate()).isBefore(today):
                !LocalDate.parse(t.dueDate()).isBefore(from)&&!LocalDate.parse(t.dueDate()).isAfter(to))
                .sorted(Comparator.comparing(Task::dueDate).thenComparing(Task::dueTime)).toList();
        var deliver=selected.stream().filter(t->t.kind().equals("TASK")&&!t.done()).toList();
        var events=selected.stream().filter(t->t.kind().equals("EVENT")).toList();
        var review=selected.stream().filter(t->t.kind().equals("REVIEW")).toList();
        String range=from.equals(to)?from.format(DISPLAY):from.format(DISPLAY)+" al "+to.format(DISPLAY);
        var out=new StringBuilder(past?"**Entregas con fecha pasada, sin marcar como completadas aquí**":"**Agenda del "+range+" · Guatemala**");
        if(!past)out.append("\n\n**Entregas sin completar en esta app**");
        append(out,deliver,past?"No hay entregas vencidas registradas.":"No hay entregas sin completar registradas para esa fecha.");
        if(!past&&!events.isEmpty()){out.append("\n\n**Eventos y clases**");append(out,events,"");}
        if(!review.isEmpty())out.append("\n\nHay ").append(review.size()).append(" elementos antiguos sin clasificar. Revísalos en Calendario → Por clasificar o conecta de nuevo Canvas.");
        if(selected.stream().anyMatch(t->t.sourceUid()!=null))out.append("\n\nEl calendario no confirma si entregaste una tarea en Canvas. Marca lo terminado en Mi estudio.");
        return Optional.of(out.toString());
    }
    private static void append(StringBuilder out,List<Task> tasks,String empty) {
        if(tasks.isEmpty()){out.append("\n").append(empty);return;}
        for(var task:tasks.stream().limit(20).toList())out.append("\n- ").append(safe(task.title())).append(" — ")
                .append(LocalDate.parse(task.dueDate()).format(DISPLAY)).append(task.dueTime().isBlank()?"":" · "+task.dueTime())
                .append(task.course().isBlank()?"":" · "+safe(task.course()));
        if(tasks.size()>20)out.append("\nHay ").append(tasks.size()-20).append(" más; consulta el calendario.");
    }
    private static String safe(String s){return s.replaceAll("[\\r\\n]"," ").replaceAll("([\\\\`*_{}\\[\\]()<>#!|])","\\\\$1");}
}
