package ideapropiaprimero.service;
import static org.junit.jupiter.api.Assertions.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.Test;

class CanvasCalendarPolicyTest {
    @Test void permiteSoloFeedHttpsDeCanvasODominioInstitucionalConfigurado() {
        assertEquals("https",CanvasCalendarService.validate("webcal://uvg.instructure.com/feeds/calendars/user_secret.ics",Set.of()).getScheme());
        assertEquals("canvas.universidad.edu",CanvasCalendarService.validate("https://canvas.universidad.edu/feeds/calendars/token.ics",Set.of("canvas.universidad.edu")).getHost());
        for(String bad:List.of("http://uvg.instructure.com/feeds/calendars/token.ics","https://uvg.instructure.com.evil.test/feeds/calendars/token.ics","https://127.0.0.1/feeds/calendars/token.ics","https://uvg.instructure.com:9000/feeds/calendars/token.ics","https://user:secret@uvg.instructure.com/feeds/calendars/token.ics","https://uvg.instructure.com/feeds/calendars/../secret.ics","https://uvg.instructure.com/login","https://uvg.instructure.com/feeds/calendars/t.ics?redirect=foo"))
            assertThrows(IllegalArgumentException.class,()->CanvasCalendarService.validate(bad,Set.of()));
    }
    @Test void bloqueaDireccionesInternasIpv4EIpv6() throws Exception {
        for(String ip:List.of("127.0.0.1","10.0.0.2","172.16.1.1","192.168.1.1","169.254.169.254","100.64.0.1","0.0.0.0","::1","fc00::1","fe80::1"))assertFalse(CanvasCalendarService.publicAddress(InetAddress.getByName(ip)));
        assertTrue(CanvasCalendarService.publicAddress(InetAddress.getByName("8.8.8.8")));
    }
    @Test void cortaLaDescargaAntesDeAcumularMasDelLimite() {
        var body=new CanvasCalendarService.LimitedBody();boolean[] canceled={false};body.onSubscribe(new Flow.Subscription(){public void request(long n){}public void cancel(){canceled[0]=true;}});
        body.onNext(List.of(ByteBuffer.wrap(new byte[CalendarImportService.MAX_BYTES+1])));
        assertTrue(canceled[0]);assertTrue(body.getBody().toCompletableFuture().isCompletedExceptionally());
    }
}
