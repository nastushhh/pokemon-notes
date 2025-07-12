package com.example.pokemonchiki.service;

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
import java.util.concurrent.TimeUnit;

@Service
public class GigaChatService {

    private static final Logger logger = LoggerFactory.getLogger(GigaChatService.class);
    private static final String OAUTH_URL = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth";
    private static final String GIGACHAT_API_URL = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions";
    private static final String GIGACHAT_FILES_URL = "https://gigachat.devices.sberbank.ru/api/v1/files/%s/content";

    private static final String CLIENT_ID = "3c4beafe-8dc5-407b-bab1-c415c5a74c4e";
    private static final String CLIENT_SECRET = "6d97866f-deab-4844-a707-5ec9879d0a2b";
    private static final String SCOPE = "GIGACHAT_API_PERS";

    private final OkHttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private String accessToken;
    private long tokenTime;

    public GigaChatService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        try {
            accessToken = getToken();
            logger.info("Access Token при запуске: {}", accessToken);
        } catch (IOException e) {
            logger.error("Ошибка при получении токена при запуске: {}", e.getMessage());
            accessToken = null;
        }
    }

    public String getToken() throws IOException {
        long currentTime = System.currentTimeMillis();
        if (accessToken != null && currentTime < tokenTime) {
            logger.info("Используется существующий токен: {}", accessToken);
            return accessToken;
        }

        logger.info("Формирование заголовка Authorization...");
        String authString = CLIENT_ID + ":" + CLIENT_SECRET;
        logger.debug("authString: {}", authString);
        String encodedAuth = Base64.getEncoder().encodeToString(authString.getBytes(StandardCharsets.UTF_8));
        logger.debug("encodedAuth (Base64): {}", encodedAuth);
        String authorizationHeader = "Basic " + encodedAuth;
        logger.debug("Authorization header: {}", authorizationHeader);

        String uuid = UUID.randomUUID().toString();
        if (!uuid.matches("[0-9a-fA-F-]{36}")) {
            throw new IllegalStateException("Сгенерированный RqUID не соответствует формату UUIDv4: " + uuid);
        }
        logger.debug("RqUID: {}", uuid);

        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        String bodyContent = "grant_type=client_credentials&scope=" + SCOPE;
        logger.debug("Request body: {}", bodyContent);
        RequestBody body = RequestBody.create(mediaType, bodyContent);

        Request request = new Request.Builder()
                .url(OAUTH_URL)
                .method("POST", body)
                .addHeader("RqUID", uuid)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", authorizationHeader)
                .build();

        logger.info("Отправка запроса на получение токена...");
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseBody = new String(response.body().bytes(), StandardCharsets.UTF_8);
                logger.debug("Ответ от GigaChat при получении токена: {}", responseBody);
                accessToken = extractAccessToken(responseBody);
                tokenTime = extractExpirationTime(responseBody);
                logger.info("Новый токен получен: {}", accessToken);
                return accessToken;
            } else {
                String errorBody = new String(response.body().bytes(), StandardCharsets.UTF_8);
                logger.error("Не удалось получить токен. Код: {}, Ошибка: {}", response.code(), errorBody);
                throw new IOException("Не удалось получить токен. Код: " + response.code() + ", Ошибка: " + errorBody);
            }
        }
    }

    public String sendGigaChatRequest(String requestBody, String token, int retryCount) throws IOException {
        if (token == null) {
            throw new IOException("Токен не получен. Проверьте учетные данные GigaChat API");
        }

        if (retryCount > 3) {
            throw new IOException("Превышено количество попыток обновления токена");
        }

        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(mediaType, requestBody);

        Request request = new Request.Builder()
                .url(GIGACHAT_API_URL)
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseBody = new String(response.body().bytes(), StandardCharsets.UTF_8);
                logger.info("Ответ от GigaChat: {}", responseBody);
                return responseBody;
            } else if (response.code() == 401) {
                logger.warn("Токен истёк (код 401), обновляем токен...");
                accessToken = null;
                tokenTime = 0;
                String newToken = getToken();
                return sendGigaChatRequest(requestBody, newToken, retryCount + 1);
            } else {
                String errorBody = new String(response.body().bytes(), StandardCharsets.UTF_8);
                logger.error("Ошибка запроса к GigaChat: {}", errorBody);
                throw new IOException("Ошибка запроса к GigaChat: " + errorBody);
            }
        }
    }

    public String downloadFile(String fileId, String token) throws IOException {
        String url = String.format(GIGACHAT_FILES_URL, fileId);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Accept", "image/jpeg")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                byte[] imageBytes = response.body().bytes();
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                logger.info("Изображение успешно скачано и закодировано в Base64, размер: {} байт", imageBytes.length);
                return base64Image;
            } else if (response.code() == 401) {
                logger.warn("Токен истёк при скачивании изображения (код 401), обновляем токен...");
                accessToken = null;
                tokenTime = 0;
                String newToken = getToken();
                Request retryRequest = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("Accept", "image/jpeg")
                        .addHeader("Authorization", "Bearer " + newToken)
                        .build();
                try (Response retryResponse = client.newCall(retryRequest).execute()) {
                    if (retryResponse.isSuccessful()) {
                        byte[] imageBytes = retryResponse.body().bytes();
                        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                        logger.info("Изображение успешно скачано после повторного запроса, размер: {} байт", imageBytes.length);
                        return base64Image;
                    } else {
                        String errorBody = retryResponse.body() != null ? retryResponse.body().string() : "Нет тела ответа";
                        logger.error("Ошибка скачивания изображения после повторного запроса: {}", errorBody);
                        throw new IOException("Ошибка скачивания изображения после повторного запроса: " + errorBody);
                    }
                }
            } else {
                String errorBody = response.body() != null ? response.body().string() : "Нет тела ответа";
                logger.error("Ошибка скачивания изображения: {}", errorBody);
                throw new IOException("Ошибка скачивания изображения: " + errorBody);
            }
        }
    }

    private String extractAccessToken(String responseBody) throws IOException {
        JsonNode root = mapper.readTree(responseBody);
        JsonNode accessTokenNode = root.get("access_token");
        if (accessTokenNode == null || accessTokenNode.isNull()) {
            throw new IOException("Поле access_token отсутствует в ответе: " + responseBody);
        }
        return accessTokenNode.asText();
    }

    private long extractExpirationTime(String responseBody) throws IOException {
        JsonNode root = mapper.readTree(responseBody);
        JsonNode expiresAtNode = root.get("expires_at");
        if (expiresAtNode == null || expiresAtNode.isNull()) {
            throw new IOException("Поле expires_at отсутствует в ответе: " + responseBody);
        }
        return expiresAtNode.asLong();
    }
}