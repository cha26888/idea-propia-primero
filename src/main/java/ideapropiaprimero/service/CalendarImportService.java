package ideapropiaprimero.service;

import ideapropiaprimero.repository.StudyRepository.CalendarTask;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Subconjunto explícito de RFC 5545: eventos y tareas individuales; sin expandir recurrencias. */
public final class CalendarImportService {
    public record Result(List<CalendarTask> events,int skipped) { }
    private record Property(String parameters,String value) { }
    public static final ZoneId STUDY_ZONE=ZoneId.of("America/Guatemala");
    private CalendarImportService() { }
    public static Result parse(String input) {
        if(input==null||input.length()>400_000||!input.contains("BEGIN:VCALENDAR")||!input.contains("END:VCALENDAR"))
            throw new IllegalArgumentException("Selecciona un archivo .ics válido de hasta 400 KB.");
        String unfolded=input.replace("\r\n","\n").replace("\r","\n").replaceAll("\n[ \t]","");
        Map<String,Property> event=null;String component=null;int nested=0,skipped=0,count=0;
        var results=new LinkedHashMap<String,CalendarTask>();
        for(String line:unfolded.split("\n")) {
            if(event==null&&(line.equals("BEGIN:VEVENT")||line.equals("BEGIN:VTODO"))) {
                event=new HashMap<>();component=line.substring(6);nested=0;continue;
            }
            if(event==null)continue;
            if(line.startsWith("BEGIN:")){nested++;continue;}
            if(line.startsWith("END:")&&nested>0){nested--;continue;}
            if(line.equals("END:"+component)) {
                if(++count>500)throw new IllegalArgumentException("Importa como máximo 500 eventos por archivo.");
                try {
                    if(event.containsKey("RRULE")||event.containsKey("RDATE")||event.containsKey("EXDATE")||"CANCELLED".equals(value(event,"STATUS")))throw new IllegalArgumentException();
                    String uid=value(event,"UID"),title=unescape(value(event,"SUMMARY"));
                    Property date=event.get(component.equals("VTODO")?"DUE":"DTSTART");
                    if(uid.isBlank()||uid.length()>400||title.isBlank()||date==null)throw new IllegalArgumentException();
                    String recurrence=value(event,"RECURRENCE-ID");
                    String key=uid+(recurrence.isBlank()?"":"#"+recurrence);
                    results.put(key,new CalendarTask(key,title.substring(0,Math.min(160,title.length())),day(date).toString()));
                } catch(RuntimeException e){skipped++;}
                event=null;continue;
            }
            if(nested>0)continue;
            int colon=line.indexOf(':');if(colon<1)continue;
            String header=line.substring(0,colon);int semi=header.indexOf(';');
            event.put(semi<0?header:header.substring(0,semi),new Property(semi<0?"":header.substring(semi+1),line.substring(colon+1)));
        }
        if(event!=null)throw new IllegalArgumentException("El calendario está incompleto.");
        return new Result(List.copyOf(results.values()),skipped);
    }
    private static String value(Map<String,Property> values,String key){return values.containsKey(key)?values.get(key).value():"";}
    private static String unescape(String s){return s.replace("\\n"," ").replace("\\N"," ").replace("\\,",",").replace("\\;",";").replace("\\\\","\\").strip();}
    private static LocalDate day(Property p) {
        String value=p.value();
        if(value.matches("\\d{8}"))return LocalDate.parse(value,DateTimeFormatter.BASIC_ISO_DATE);
        boolean utc=value.endsWith("Z");
        LocalDateTime date=LocalDateTime.parse(utc?value.substring(0,value.length()-1):value,DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss").withResolverStyle(java.time.format.ResolverStyle.STRICT));
        ZoneId zone=STUDY_ZONE;
        for(String parameter:p.parameters().split(";"))if(parameter.startsWith("TZID="))zone=ZoneId.of(parameter.substring(5).replace("\"",""));
        return date.atZone(utc?ZoneOffset.UTC:zone).withZoneSameInstant(STUDY_ZONE).toLocalDate();
    }
}
