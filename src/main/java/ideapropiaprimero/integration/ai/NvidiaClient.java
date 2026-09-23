package ideapropiaprimero.integration.ai;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class NvidiaClient implements AiClient {

    private final ObjectMapper json;
    private final HttpClient http;
    private final URI endpoint;
    private final String apiKey;
    private final String model;
    private final String reasoningEffort;

    public NvidiaClient(
            ObjectMapper json,
            @Value("${ai.endpoint}") String endpoint,
            @Value("${ai.api-key}") String apiKey,
            @Value("${ai.model}") String model,
            @Value("${ai.reasoning-effort}") String reasoningEffort) {

        this.json = json;
        this.endpoint = URI.create(endpoint);
        this.apiKey = apiKey;
        this.model = model;
        this.reasoningEffort = reasoningEffort;

        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Override
    public String responder(List<ChatMessage> messages) {

        if (apiKey.isBlank()) {
            throw new AiException(
                    503,
                    "Falta NVIDIA_API_KEY. Configúrala y reinicia la aplicación."
            );
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("max_tokens", 2048);
        body.put("stream", false);

        if (!reasoningEffort.isBlank()) {
            body.put("reasoning_effort", reasoningEffort);
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(90))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            json.writeValueAsString(body)
                    ))
                    .build();

            HttpResponse<String> response = http.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            int code = response.statusCode();

            if (code == 401 || code == 403) {
                throw new AiException(
                        502,
                        "NVIDIA rechazó el acceso. Revisa tu clave y sus permisos."
                );
            }

            if (code == 429) {
                throw new AiException(
                        503,
                        "NVIDIA alcanzó un límite de uso. Espera y revisa tu cuota."
                );
            }

            if (code == 202) {
                throw new AiException(
                        503,
                        "NVIDIA dejó la solicitud pendiente. Intenta de nuevo más tarde."
                );
            }

            if (code != 200) {
                throw new AiException(
                        502,
                        "La IA devolvió HTTP " + code
                                + ". Revisa el modelo o intenta más tarde."
                );
            }

            JsonNode result = json.readTree(response.body());
            JsonNode choice = result == null
                    ? null
                    : result.path("choices").path(0);

            JsonNode content = choice == null
                    ? null
                    : choice.path("message").path("content");

            if (content == null
                    || !content.isTextual()
                    || content.asText().isBlank()) {

                throw new AiException(
                        502,
                        "La IA no devolvió una respuesta de texto utilizable."
                );
            }

            return content.asText();

        } catch (HttpTimeoutException e) {
            throw new AiException(
                    504,
                    "La IA tardó demasiado. Intenta nuevamente."
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new AiException(
                    503,
                    "La consulta fue interrumpida."
            );

        } catch (IOException e) {
            throw new AiException(
                    502,
                    "No se pudo comunicar con la IA o interpretar su respuesta."
            );
        }
    }
}
