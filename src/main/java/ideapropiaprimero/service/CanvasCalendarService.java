package ideapropiaprimero.service;

import ideapropiaprimero.repository.StudyRepository;
import java.net.*;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Suscripción privada iCalendar, refrescada al usar la app como máximo cada cinco minutos. */
@Service
public class CanvasCalendarService {
    private final StudyRepository store;
    private final Set<String> extraHosts;
    private final Object[] locks=new Object[32];
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    public CanvasCalendarService(StudyRepository store,@Value("${app.calendar.allowed-hosts:}") String allowedHosts) {
        this.store=store;this.extraHosts=new HashSet<>();
        for(String host:allowedHosts.split(","))if(!host.isBlank())extraHosts.add(host.strip().toLowerCase(Locale.ROOT));
        Arrays.setAll(locks,i->new Object());
    }
    private Object lock(long user){return locks[Math.floorMod(user,locks.length)];}
    public Map<String,Object> status(long user) {
        var f=store.calendarFeed(user);
        if(f.isEmpty())return Map.of("connected",false,"message","Sin conexión por URL. Las fechas importadas son una copia local.");
        var feed=f.get();return Map.of("connected",true,"host",URI.create(feed.url()).getHost(),"syncedAt",feed.syncedAt(),
                "error",feed.error(),"skipped",feed.skipped(),"message","Se actualiza al usar el calendario o el chat, cada 5 minutos como máximo.");
    }
    public void connect(long user,String input,String course) {
        synchronized(lock(user)) {
            URI uri=validate(input,extraHosts);
            var parsed=CalendarImportService.parse(download(uri));
            store.syncCalendar(user,uri.toASCIIString(),StudyService.text(course,80,true),parsed.events(),parsed.skipped(),Instant.now().toString(),true);
        }
    }
    public void refresh(long user,boolean force) {
        synchronized(lock(user)) {
            var feed=store.calendarFeed(user);if(feed.isEmpty())return;
            var f=feed.get();
            if(!force&&!f.attemptedAt().isBlank()&&Instant.parse(f.attemptedAt()).plusSeconds(300).isAfter(Instant.now()))return;
            try {
                var parsed=CalendarImportService.parse(download(validate(f.url(),extraHosts)));
                store.syncCalendar(user,f.url(),f.course(),parsed.events(),parsed.skipped(),Instant.now().toString(),false);
            }catch(IllegalArgumentException e){
                store.calendarFailure(user,Instant.now().toString(),e.getMessage());
                if(force)throw e;
            }
        }
    }
    public void disconnect(long user){synchronized(lock(user)){store.disconnectCalendar(user);}}
    /** Lista de hosts de Canvas o dominios institucionales elegidos por el administrador, nunca por el navegador. */
    static URI validate(String input,Set<String> extraHosts) {
        try {
            if(input==null||input.length()>2000)throw new IllegalArgumentException();
            String value=input.strip().replaceFirst("(?i)^webcal://","https://");URI u=URI.create(value);
            String host=u.getHost()==null?"":u.getHost().toLowerCase(Locale.ROOT);
            if(!"https".equalsIgnoreCase(u.getScheme())||u.getUserInfo()!=null||u.getFragment()!=null||u.getRawQuery()!=null||
                    (u.getPort()!=-1&&u.getPort()!=443)||host.isBlank()||
                    !(host.endsWith(".instructure.com")||host.equals("instructure.com")||extraHosts.contains(host))||
                    !u.getRawPath().matches("/feeds/calendars/[A-Za-z0-9_-]+\\.ics"))throw new IllegalArgumentException();
            return u;
        }catch(RuntimeException e){throw new IllegalArgumentException("Usa la URL de Canvas → Calendario → Calendar Feed (.ics). Si tu institución usa otro dominio, habilítalo en app.calendar.allowed-hosts.");}
    }
    static boolean publicAddress(InetAddress address) {
        if(address.isAnyLocalAddress()||address.isLoopbackAddress()||address.isLinkLocalAddress()||address.isSiteLocalAddress()||address.isMulticastAddress())return false;
        byte[] b=address.getAddress();
        if(b.length==4){int a=b[0]&255,c=b[1]&255;return a!=0&&a<224&&!(a==100&&c>=64&&c<=127)&&!(a==198&&(c==18||c==19));}
        return (b[0]&0xfe)!=0xfc; // IPv6 unique-local
    }
    String download(URI uri) {
        try {
            // El allowlist evita destinos arbitrarios/DNS rebinding controlado por el solicitante.
            for(var address:InetAddress.getAllByName(uri.getHost()))if(!publicAddress(address))throw new IllegalArgumentException("El calendario debe estar en un servidor público de Canvas.");
            var request=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(12)).header("Accept","text/calendar").GET().build();
            var future=client.sendAsync(request,info->new LimitedBody());
            HttpResponse<byte[]> response;
            try {response=future.get(15,TimeUnit.SECONDS);}catch(Exception e){future.cancel(true);throw e;}
            if(response.statusCode()!=200)throw new IllegalArgumentException("Canvas no permitió leer el calendario (HTTP "+response.statusCode()+"). Copia de nuevo la URL de Calendar Feed.");
            return new String(response.body(),StandardCharsets.UTF_8);
        }catch(IllegalArgumentException e){throw e;}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalArgumentException("La actualización fue interrumpida. Intenta de nuevo.");}
        catch(Exception e){throw new IllegalArgumentException("No se pudo descargar el calendario (conexión, tiempo o tamaño máximo de 2 MB). Se conserva la última copia.");}
    }
    /** Límite durante la recepción, no después de acumular una respuesta ilimitada. */
    static final class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final CompletableFuture<byte[]> body=new CompletableFuture<>();
        private final ByteArrayOutputStream bytes=new ByteArrayOutputStream();private Flow.Subscription subscription;
        public CompletionStage<byte[]> getBody(){return body;}
        public void onSubscribe(Flow.Subscription s){subscription=s;s.request(1);}
        public void onNext(List<ByteBuffer> chunks) {
            for(var chunk:chunks){if(chunk.remaining()>CalendarImportService.MAX_BYTES-bytes.size()){subscription.cancel();body.completeExceptionally(new IllegalArgumentException("Calendario demasiado grande"));return;}
                byte[] part=new byte[chunk.remaining()];chunk.get(part);bytes.writeBytes(part);}
            subscription.request(1);
        }
        public void onError(Throwable t){body.completeExceptionally(t);}
        public void onComplete(){body.complete(bytes.toByteArray());}
    }
}
