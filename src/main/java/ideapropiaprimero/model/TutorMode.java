package ideapropiaprimero.model;

/** Contrato estable de API y persistencia; etiquetas en español en la interfaz. */
public enum TutorMode {
    UNDERSTAND, PRACTICE, CHALLENGE;

    public static TutorMode parse(String value) {
        if (value == null) throw new IllegalArgumentException("Selecciona un modo de acompañamiento.");
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Modo inválido. Usa UNDERSTAND, PRACTICE o CHALLENGE.");
        }
    }
}
