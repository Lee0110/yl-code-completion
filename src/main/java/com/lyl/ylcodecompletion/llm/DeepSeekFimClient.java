package com.lyl.ylcodecompletion.llm;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class DeepSeekFimClient {

    private static final Logger LOG = Logger.getInstance(DeepSeekFimClient.class);

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private static final Gson GSON = new Gson();

    public static DeepSeekFimClient getInstance() {
        return ApplicationManager.getApplication().getService(DeepSeekFimClient.class);
    }

    public @NotNull CompletableFuture<LlmResponse> complete(
            @NotNull LlmRequest req,
            @Nullable String apiKey,
            @NotNull String baseUrl,
            int timeoutMs
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            return CompletableFuture.failedFuture(
                    new LlmException(LlmException.Kind.AUTH, "API key is empty")
            );
        }

        String url = stripTrailingSlash(baseUrl) + "/completions";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", req.model());
        body.put("prompt", req.prompt());
        if (req.suffix() != null && !req.suffix().isEmpty()) {
            body.put("suffix", req.suffix());
        }
        body.put("max_tokens", req.maxTokens());
        body.put("temperature", req.temperature());
        body.put("top_p", req.topP());
        body.put("stream", false);
        if (req.stop() != null && !req.stop().isEmpty()) {
            body.put("stop", req.stop());
        }

        String json = GSON.toJson(body);

        HttpRequest httpReq;
        try {
            httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(Math.max(1000, timeoutMs)))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
        } catch (IllegalArgumentException e) {
            return CompletableFuture.failedFuture(
                    new LlmException(LlmException.Kind.BAD_REQUEST, "Invalid base URL: " + baseUrl, e)
            );
        }

        return HTTP.sendAsync(httpReq, HttpResponse.BodyHandlers.ofString())
                .handle((resp, err) -> {
                    if (err != null) {
                        Throwable cause = err instanceof CompletionException ? err.getCause() : err;
                        if (cause instanceof HttpTimeoutException) {
                            throw new LlmException(LlmException.Kind.TIMEOUT, "Request timed out", cause);
                        }
                        throw new LlmException(LlmException.Kind.NETWORK,
                                "Network error: " + cause.getMessage(), cause);
                    }
                    return parseResponse(resp);
                });
    }

    public @NotNull CompletableFuture<Void> testConnection(
            @Nullable String apiKey,
            @NotNull String baseUrl,
            @NotNull String model,
            int timeoutMs
    ) {
        LlmRequest probe = new LlmRequest(model, "// hi", null, 1, 0.0, 1.0, null);
        return complete(probe, apiKey, baseUrl, timeoutMs).thenApply(r -> null);
    }

    private static @NotNull LlmResponse parseResponse(@NotNull HttpResponse<String> resp) {
        int code = resp.statusCode();
        String bodyText = resp.body() == null ? "" : resp.body();
        if (code == 401 || code == 403) {
            throw new LlmException(LlmException.Kind.AUTH,
                    "Authentication failed (" + code + "): " + truncate(bodyText));
        }
        if (code == 429) {
            throw new LlmException(LlmException.Kind.RATE_LIMITED,
                    "Rate limited: " + truncate(bodyText));
        }
        if (code >= 500) {
            throw new LlmException(LlmException.Kind.SERVER,
                    "Server error (" + code + "): " + truncate(bodyText));
        }
        if (code >= 400) {
            throw new LlmException(LlmException.Kind.BAD_REQUEST,
                    "Bad request (" + code + "): " + truncate(bodyText));
        }

        try {
            JsonElement el = JsonParser.parseString(bodyText);
            JsonObject root = el.getAsJsonObject();
            JsonArray choices = root.has("choices") ? root.getAsJsonArray("choices") : null;
            if (choices == null || choices.isEmpty()) {
                throw new LlmException(LlmException.Kind.UNKNOWN,
                        "Empty choices in response: " + truncate(bodyText));
            }
            JsonObject first = choices.get(0).getAsJsonObject();
            String text = first.has("text") && !first.get("text").isJsonNull()
                    ? first.get("text").getAsString() : "";
            String finish = first.has("finish_reason") && !first.get("finish_reason").isJsonNull()
                    ? first.get("finish_reason").getAsString() : null;
            return new LlmResponse(text, finish);
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            LOG.debug("Failed to parse FIM response: " + bodyText, e);
            throw new LlmException(LlmException.Kind.UNKNOWN,
                    "Failed to parse response: " + e.getMessage(), e);
        }
    }

    private static @NotNull String stripTrailingSlash(@NotNull String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static @NotNull String truncate(@NotNull String s) {
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }
}
