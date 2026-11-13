package ideapropiaprimero.service;

/** Títulos locales: no se solicita una generación adicional al proveedor. */
public final class ChatTitles {
    private ChatTitles() { }
    public static String from(String message) {
        String title=message.replaceAll("[\\p{Cntrl}\\s]+"," ").strip()
                .replaceFirst("(?iu)^(?:hola[,! ]+)?(?:por favor[, ]+)?(?:ay[uú]dame (?:a |con )|necesito ayuda (?:para |con )|puedes explicarme |expl[ií]came |quiero aprender (?:sobre )?|recuerda que )","")
                .replaceFirst("(?iu)[.!?] (?:todav[ií]a|a[uú]n|no he|por favor).*$","").strip();
        if(title.isBlank())return "Nueva conversación";
        title=title.substring(0,1).toUpperCase(java.util.Locale.ROOT)+title.substring(1);
        if(title.length()<=64)return title;
        int end=title.lastIndexOf(' ',61);if(end<25)end=61;
        return title.substring(0,end).stripTrailing()+"…";
    }
}
