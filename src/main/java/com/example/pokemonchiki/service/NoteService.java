package com.example.pokemonchiki.service;

import com.example.pokemonchiki.model.Note;
import com.example.pokemonchiki.model.Pokemon;
import com.example.pokemonchiki.repository.NoteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class NoteService {

    private static final Logger logger = LoggerFactory.getLogger(NoteService.class);

    private final OkHttpClient client;
    private static final String OAUTH_URL = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth";
    private static final String GIGACHAT_API_URL = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions";

    private static final String CLIENT_ID = "3c4beafe-8dc5-407b-bab1-c415c5a74c4e";
    private static final String CLIENT_SECRET = "6d97866f-deab-4844-a707-5ec9879d0a2b";
    private static final String SCOPE = "GIGACHAT_API_PERS";

    private final NoteRepository noteRepository;
    private final PokemonService pokemonService;
    private final ObjectMapper mapper = new ObjectMapper();

    private String accessToken;
    private long tokenTime;

    @Autowired
    public NoteService(NoteRepository noteRepository, PokemonService pokemonService) {
        this.noteRepository = noteRepository;
        this.pokemonService = pokemonService;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        try {
            accessToken = getToken();
            logger.info("Access Token при запуске: {}", accessToken);
        } catch (IOException e) {
            logger.error("Ошибка при получении токена при запуске: {}", e.getMessage());
            accessToken = null; // Устанавливаем null, чтобы избежать проблем
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
        if (token == null){
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

    private String analyzeText(String text) throws IOException {
        String token = getToken();

        ObjectNode requestJson = mapper.createObjectNode();
        requestJson.put("model", "GigaChat");

        ArrayNode messagesArray = mapper.createArrayNode();
        ObjectNode systemMessage = mapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", "Ты аналитик настроений. Определи настроение текста на русском языке и верни одно слово, описывающее эмоциональный окрас.");
        messagesArray.add(systemMessage);

        ObjectNode userMessage = mapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", text);
        messagesArray.add(userMessage);

        requestJson.set("messages", messagesArray);
        requestJson.put("stream", false);
        requestJson.put("update_interval", 0);

        String requestBody = mapper.writeValueAsString(requestJson);
        logger.info("Запрос к GigaChat для анализа настроения: {}", requestBody);

        String responseBody = sendGigaChatRequest(requestBody, token, 0);
        return extractMoodFromResponse(responseBody);
    }

    private String extractMoodFromResponse(String responseBody) throws IOException {
        JsonNode root = mapper.readTree(responseBody);
        JsonNode choices = root.path("choices");
        if (choices.isEmpty()) {
            throw new IOException("Ответ от GigaChat не содержит choices: " + responseBody);
        }
        JsonNode message = choices.get(0).path("message");
        JsonNode content = message.path("content");
        if (content.isMissingNode()) {
            throw new IOException("Ответ от GigaChat не содержит content: " + responseBody);
        }
        String mood = content.asText();
        if (mood.split("\\s+").length != 1) {
            throw new IOException("GigaChat вернул не одно слово: " + mood);
        }
        return mood;
    }

    public Note saveNote(String content) throws IOException {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Текст заметки не может быть пустым");
        }
        if (content.length() > 1000) {
            throw new IllegalArgumentException("Текст заметки слишком длинный, максимум 1000 символов");
        }

        String token = getToken();
        String mood = analyzeText(content);

        Pokemon pokemon = pokemonService.generatePokemon(mood);
        Note note = new Note(content, mood, pokemon);
        Note savedNote = noteRepository.save(note);
        logger.info("Сохранена заметка с id {} и настроение {}", savedNote.getId(), mood);
        return savedNote;
    }

    public String analyzeNoteMood(String content) throws IOException {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Текст заметки не может быть пустым");
        }
        return analyzeText(content);
    }

    public List<Note> getAllNotes() {
        List<Note> notes = noteRepository.findAll();
        logger.info("Получено {} заметок", notes.size());
        return notes;
    }

    public Optional<Note> getNote(Integer id) {
        Optional<Note> note = noteRepository.findById(id);
        if (note.isPresent()) {
            logger.info("Найдена заметка с ID {}", id);
        } else {
            logger.warn("Заметка с ID {} не найдена", id);
        }
        return note;
    }

    public void deleteNote(Integer id) {
        if (!noteRepository.existsById(id)) {
            logger.warn("Попытка удалить несуществующую заметку с ID {}", id);
            throw new IllegalArgumentException("Заметка с ID " + id + " не найдена");
        }
        noteRepository.deleteById(id);
        logger.info("Удалена заметка с ID {}", id);
    }

    public OkHttpClient getClient() {
        return client;
    }

    public void resetToken() {
        this.accessToken = null;
        this.tokenTime = 0;
    }
}