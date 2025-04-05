package com.example.pokemonchiki.service;

import com.example.pokemonchiki.model.Pokemon;
import com.example.pokemonchiki.repository.PokemonRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PokemonService {

    private static final Logger logger = LoggerFactory.getLogger(PokemonService.class);
    private static final String GIGACHAT_FILES_URL = "https://gigachat.devices.sberbank.ru/api/v1/files/%s/content";

    private final PokemonRepository pokemonRepository;
    private final NoteService noteService;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public PokemonService(PokemonRepository pokemonRepository, NoteService noteService) {
        this.pokemonRepository = pokemonRepository;
        this.noteService = noteService;
    }

    public Pokemon savePokemon(Pokemon pokemon) {
        return pokemonRepository.save(pokemon);
    }

    public List<Pokemon> getAllPokemons() {
        return pokemonRepository.findAll();
    }

    public Optional<Pokemon> getPokemonById(int id) {
        return pokemonRepository.findById(id);
    }

    public void deletePokemon(Integer id) {
        pokemonRepository.deleteById(id);
    }

    public Pokemon generatePokemon(String mood) throws IOException {
        Pokemon moodPokemon = new Pokemon();
        moodPokemon.setMood(mood);

        // Формируем промпт для генерации изображения
        String prompt = String.format(
                "Создай уникального покемона в стиле Pokémon, который выглядит %s. Это призрачный тип с полупрозрачным телом, темно-фиолетовыми оттенками и светящимися красными глазами. Фон обязательно должен быть полностью белым. Стиль: аниме.",
                mood);

        // Отправляем запрос на генерацию изображения через NoteService
        String imageId = generateImage(prompt);
        if (imageId == null) {
            throw new IOException("Не удалось получить идентификатор изображения от GigaChat");
        }

        // Скачиваем изображение по идентификатору
        String imageBase64 = downloadImage(imageId);
        if (imageBase64 == null) {
            throw new IOException("Не удалось скачать изображение от GigaChat");
        }

        // Устанавливаем поля покемона
        moodPokemon.setName("Ghostly " + mood); // Имя покемона с учётом настроения
        moodPokemon.setType("Ghost"); // Тип соответствует промпту
        moodPokemon.setAbility("Phantom Glow"); // Способность соответствует описанию
        moodPokemon.setImage(imageBase64); // Сохраняем изображение в формате Base64

        // Сохраняем покемона в базе данных
        return pokemonRepository.save(moodPokemon);
    }

    private String generateImage(String prompt) throws IOException {
        // Формируем тело запроса для генерации изображения
        ObjectNode requestJson = mapper.createObjectNode();
        requestJson.put("model", "GigaChat");

        ArrayNode messagesArray = mapper.createArrayNode();
        ObjectNode systemMessage = mapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", "Ты — художник, создающий изображения в стиле аниме с полностью белым фоном.");
        messagesArray.add(systemMessage);

        ObjectNode userMessage = mapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messagesArray.add(userMessage);

        requestJson.set("messages", messagesArray);
        requestJson.put("function_call", "auto");
        requestJson.put("stream", false);
        requestJson.put("update_interval", 0);

        String requestBody = mapper.writeValueAsString(requestJson);
        logger.info("Запрос к GigaChat для генерации изображения: {}", requestBody);

        // Используем метод sendGigaChatRequest из NoteService
        String responseBody = noteService.sendGigaChatRequest(requestBody, noteService.getToken(), 0);
        return extractImageIdFromResponse(responseBody);
    }

    private String extractImageIdFromResponse(String responseBody) throws IOException {
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

        // Извлекаем imageId из тега <img src="..."/>
        String contentText = content.asText();
        Pattern pattern = Pattern.compile("<img src=\"([0-9a-fA-F-]{36})\"");
        Matcher matcher = pattern.matcher(contentText);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new IOException("Не удалось извлечь imageId из ответа GigaChat: " + contentText);
        }
    }

    private String downloadImage(String imageId) throws IOException {
        String url = String.format(GIGACHAT_FILES_URL, imageId);
        String token = noteService.getToken();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Accept", "image/jpeg")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        try (Response response = noteService.getClient().newCall(request).execute()) {
            if (response.isSuccessful()) {
                byte[] imageBytes = response.body().bytes();
                // Кодируем изображение в Base64
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                logger.info("Изображение успешно скачано и закодировано в Base64, размер: {} байт", imageBytes.length);
                return base64Image;
            } else if (response.code() == 401) {
                logger.warn("Токен истёк при скачивании изображения (код 401), обновляем токен...");
                noteService.resetToken();
                String newToken = noteService.getToken();
                Request retryRequest = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("Accept", "image/jpeg")
                        .addHeader("Authorization", "Bearer " + newToken)
                        .build();
                try (Response retryResponse = noteService.getClient().newCall(retryRequest).execute()) {
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
}