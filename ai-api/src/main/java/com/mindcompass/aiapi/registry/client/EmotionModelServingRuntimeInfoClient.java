// FastAPI emotion model serving runtime 정보를 조회하는 내부 클라이언트입니다.
package com.mindcompass.aiapi.registry.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class EmotionModelServingRuntimeInfoClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpClient httpClient;
    private final String fastApiBaseUrl;

    public EmotionModelServingRuntimeInfoClient(
            @Value("${ai.fastapi.base-url:http://localhost:8002}") String fastApiBaseUrl
    ) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.fastApiBaseUrl = fastApiBaseUrl;
    }

    public EmotionModelServingRuntimeInfoResponse getRuntimeInfo() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fastApiBaseUrl + "/internal/model/runtime-info"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "FastAPI runtime info call failed with status " + response.statusCode()
                );
            }
            return OBJECT_MAPPER.readValue(response.body(), EmotionModelServingRuntimeInfoResponse.class);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to load FastAPI runtime info: " + exception.getMessage(),
                    exception
            );
        }
    }

    public String getFastApiBaseUrl() {
        return fastApiBaseUrl;
    }
}
