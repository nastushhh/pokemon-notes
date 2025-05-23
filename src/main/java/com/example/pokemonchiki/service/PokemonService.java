package com.example.pokemonchiki.service;

import com.example.pokemonchiki.model.Pokemon;
import com.example.pokemonchiki.model.Note;
import com.example.pokemonchiki.repository.PokemonRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PokemonService {

    private static final Logger logger = LoggerFactory.getLogger(PokemonService.class);
    private final PokemonRepository pokemonRepository;
    private final GigaChatService gigaChatService;
    private final NoteService noteService;

    private final ObjectMapper mapper = new ObjectMapper();
    private static final String IMAGE_DIR = "src/main/resources/static/images/";
    private static final String BASE_URL = "http://localhost:8080/images/";

    @Autowired
    public PokemonService(PokemonRepository pokemonRepository, GigaChatService gigaChatService, NoteService noteService) {
        this.pokemonRepository = pokemonRepository;
        this.gigaChatService = gigaChatService;
        this.noteService = noteService;

        File dir = new File(IMAGE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

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

    public Pokemon generatePokemon(String name, String color, Integer noteId) throws IOException {
        Optional<Note> noteOpt = noteService.getNote(noteId);
        if (noteOpt.isEmpty()) {
            throw new IllegalArgumentException("Заметка с ID " + noteId + " не найдена");
        }
        Note note = noteOpt.get();

        String mood = note.getMood();
        if (mood == null || mood.trim().isEmpty()) {
            throw new IllegalArgumentException("У заметки с ID " + noteId + "отсутствует настроение");
        }

        Pokemon moodPokemon = new Pokemon();
        moodPokemon.setMood(mood);
        moodPokemon.setName(name);
        moodPokemon.setColor(color);

        String prompt = String.format(
                "Создай уникального покемона в стиле Pokémon, который выглядит %s. Это призрачный тип с полупрозрачным телом, %s оттенками и светящимися глазами, которые отражают настроение. Фон обязательно должен быть полностью белым. Стиль: аниме.",
                mood, color);

        String imageId = generateImage(prompt);
        if (imageId == null) {
            throw new IOException("Не удалось получить идентификатор изображения от GigaChat");
        }

        String imageBase64 = downloadImage(imageId);
        if (imageBase64 == null) {
            throw new IOException("Не удалось скачать изображение от GigaChat");
        }

        String imageUrl = saveImgToFile(imageBase64, name);
        moodPokemon.setImage(imageUrl);

        Pokemon savedPokemon = pokemonRepository.save(moodPokemon);

        note.setPokemon(savedPokemon);
        noteService.updateNote(note);

        return savedPokemon;
    }

    private String saveImgToFile(String imageBase64, String pokemonName) throws IOException {
        String base64Data = imageBase64.replaceFirst("^data:image/[^;]+;base64,", "");
        byte[] imageBytes = Base64.getDecoder().decode(base64Data);

        String fileName = pokemonName + "_" + System.currentTimeMillis() + ".jpg";
        String filePath = IMAGE_DIR + fileName;

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(imageBytes);
        }
        return BASE_URL + filePath;
    }

    private String generateImage(String prompt) throws IOException {
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

        String responseBody = gigaChatService.sendGigaChatRequest(requestBody, gigaChatService.getToken(), 0);
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
        String token = gigaChatService.getToken();
        return gigaChatService.downloadFile(imageId, token);
    }
}