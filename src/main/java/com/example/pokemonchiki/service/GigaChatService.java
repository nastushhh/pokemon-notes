package com.example.pokemonchiki.service;

import com.example.pokemonchiki.config.GigaChatProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Service
public class GigaChatService {

    private static final Logger logger = LoggerFactory.getLogger(GigaChatService.class);

    private final OkHttpClient client;
    private final ObjectMapper mapper;
    private final GigaChatProperties props;

    private String accessToken;
    private long tokenTime;

    public GigaChatService(OkHttpClient client, ObjectMapper mapper, GigaChatProperties props) {
        this.client = client;
        this.mapper = mapper;
        this.props = props;
    }

    public String getToken() throws IOException {
        long currentTime = System.currentTimeMillis();
        if (accessToken != null && currentTime < tokenTime) {
            logger.debug("Используется существующий токен.");
            return accessToken;
        }

        String authString = props.getClientId() + ":" + props.getClientSecret();
        String encodedAuth = Base64.getEncoder().encodeToString(authString.getBytes(StandardCharsets.UTF_8));
        String authorizationHeader = "Basic " + encodedAuth;

        String uuid = UUID.randomUUID().toString();

        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        String bodyContent = "grant_type=client_credentials&scope=" + props.getScope();
        RequestBody body = RequestBody.create(mediaType, bodyContent);

        Request request = new Request.Builder()
                .url(props.getOauthUrl())
                .post(body)
                .addHeader("RqUID", uuid)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", authorizationHeader)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseBody = new String(response.body().bytes(), StandardCharsets.UTF_8);
                accessToken = extractAccessToken(responseBody);
                tokenTime = extractExpirationTime(responseBody);
                logger.info("Новый токен успешно получен.");
                return accessToken;
            } else {
                String errorBody = response.body() != null ? response.body().string() : "Нет тела ответа";
                logger.error("Ошибка получения токена: {}", errorBody);
                throw new IOException("Ошибка получения токена: " + errorBody);
            }
        }
    }

    public String sendGigaChatRequest(String requestBody, String token, int retryCount) throws IOException {
        if (token == null) throw new IOException("Токен отсутствует");

        if (retryCount > 3) throw new IOException("Превышено количество попыток обновления токена");

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                requestBody
        );

        Request request = new Request.Builder()
                .url(props.getApiUrl())
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseBody = new String(response.body().bytes(), StandardCharsets.UTF_8);
                logger.debug("Ответ от GigaChat: {}", responseBody);
                return responseBody;
            } else if (response.code() == 401) {
                logger.warn("Токен истёк, обновляем...");
                accessToken = null;
                tokenTime = 0;
                String newToken = getToken();
                return sendGigaChatRequest(requestBody, newToken, retryCount + 1);
            } else {
                String errorBody = response.body() != null ? response.body().string() : "Нет тела ответа";
                logger.error("Ошибка запроса к GigaChat: {}", errorBody);
                throw new IOException("Ошибка запроса к GigaChat: " + errorBody);
            }
        }
    }

    public String downloadFile(String fileId, String token) throws IOException {
        String url = String.format(props.getFilesUrl(), fileId);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Accept", "image/jpeg")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                byte[] imageBytes = response.body().bytes();
                return Base64.getEncoder().encodeToString(imageBytes);
            } else if (response.code() == 401) {
                logger.warn("Токен истёк при скачивании, обновляем...");
                String newToken = getToken();
                return downloadFile(fileId, newToken);
            } else {
                String errorBody = response.body() != null ? response.body().string() : "Нет тела ответа";
                throw new IOException("Ошибка скачивания файла: " + errorBody);
            }
        }
    }

    private String extractAccessToken(String responseBody) throws IOException {
        JsonNode root = mapper.readTree(responseBody);
        JsonNode accessTokenNode = root.get("access_token");
        if (accessTokenNode == null || accessTokenNode.isNull()) {
            throw new IOException("access_token отсутствует в ответе: " + responseBody);
        }
        return accessTokenNode.asText();
    }

    private long extractExpirationTime(String responseBody) throws IOException {
        JsonNode root = mapper.readTree(responseBody);
        JsonNode expiresAtNode = root.get("expires_at");
        if (expiresAtNode == null || expiresAtNode.isNull()) {
            throw new IOException("expires_at отсутствует в ответе: " + responseBody);
        }
        return expiresAtNode.asLong();
    }
}
