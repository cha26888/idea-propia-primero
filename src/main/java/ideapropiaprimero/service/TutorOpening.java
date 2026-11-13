package ideapropiaprimero.service;

import ideapropiaprimero.model.TutorMode;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** Apertura conservadora para una tarea declarada explícitamente sin intento.
 * No clasifica dominio ni pretende reconocer cualquier intención en lenguaje natural. */
final class TutorOpening {
    private static final Pattern NOT_STARTED = Pattern.compile(
            "\\b(?:no he (?:intentado|empezado|comenzado|probado|hecho nada)|"
            + "no lo he intentado|no (?:intente|empece|comence|probe) nada|"
            + "sin (?:ningun )?intento|no se (?:como|por donde) empezar)\\b");
    private static final Pattern TASK = Pattern.compile(
            "\\b(?:resuelve|resolver|calcula|calcular|soluciona|solucionar|"
            + "implementar|implementa|programar|programa|redactar|redacta|"
            + "ejercicio|problema|tarea|ensayo|demostrar|demuestra)\\b");
    private static final Pattern CONCEPT = Pattern.compile(
            "\\b(?:que (?:es|son|significa|significan)|define|defineme|definicion|"
            + "como funciona|explicame el concepto|dame un ejemplo)\\b");
    private static final Pattern ATTEMPT = Pattern.compile(
            "\\b(?:mi intento|mi respuesta|mi solucion|me dio|obtuve|"
            + "reste|sume|dividi|multiplique|escribi|probe con|creo que|"
            + "he (?:restado|sumado|dividido|multiplicado|escrito))\\b");

    private TutorOpening() { }

    static Optional<String> responseFor(TutorMode mode, String message) {
        if (mode == TutorMode.UNDERSTAND) return Optional.empty();
        String normalized = Normalizer.normalize(message, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ").strip();
        // Ante una consulta conceptual o un intento visible, conserva la revisión
        // del modelo. No usa números, longitud o mensajes del tutor como evidencia.
        if (!NOT_STARTED.matcher(normalized).find() || !TASK.matcher(normalized).find()
                || CONCEPT.matcher(normalized).find() || ATTEMPT.matcher(normalized).find()) {
            return Optional.empty();
        }
        if (mode == TutorMode.CHALLENGE) {
            return Optional.of("¿Cuál sería tu primera idea para abordar este ejercicio? "
                    + "Puede ser una hipótesis; no hace falta que sea correcta.");
        }
        return Optional.of("Empieza por distinguir qué te piden y qué información tienes. "
                + "Con eso, ¿qué intentarías primero?");
    }
}
