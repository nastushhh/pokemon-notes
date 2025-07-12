package com.example.pokemonchiki.service;

import com.example.pokemonchiki.dto.PokemonDto;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PokemonService {
    

    private static final Logger logger = LoggerFactory.getLogger(PokemonService.class);
    private final PokemonRepository pokemonRepository;
    private final GigaChatService gigaChatService;
    private final NoteService noteService;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.image-dir:src/main/resources/static/images/}")
    private String imageDir;

    @Value("${app.base-url:http://localhost:8080/images/}")
    private String baseUrl;

    @Autowired
    public PokemonService(PokemonRepository pokemonRepository, GigaChatService gigaChatService, NoteService noteService) {
        this.pokemonRepository = pokemonRepository;
        this.gigaChatService = gigaChatService;
        this.noteService = noteService;

        File dir = new File(imageDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

    }

    public PokemonDto savePokemon(PokemonDto dto){
        Pokemon pokemon = mapDtoToEntity(dto);
        Pokemon saved = pokemonRepository.save(pokemon);
        return mapEntityToDto(saved);
    }

    public List<PokemonDto> getAllPokemons() {

        return pokemonRepository.findAll()
                .stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    public Optional<PokemonDto> getPokemonById(int id) {
        return pokemonRepository.findById(id)
                .map(this::mapEntityToDto);
    }

    public void deletePokemon(Integer id) {
        pokemonRepository.deleteById(id);
    }

    @Transactional
    public PokemonDto generatePokemon(String name, String color, Integer noteId) throws IOException {
        Note note = noteService.getNote(noteId)
                .orElseThrow(() -> new IllegalArgumentException("Note with ID " + noteId + " not found"));

        String mood = note.getMood();
        if (mood == null || mood.trim().isEmpty()) {
            throw new IllegalArgumentException("Note with ID " + noteId + " has no mood set");
        }

        String prompt = String.format(
                "Создай уникального покемона в стиле Pokémon, который выглядит %s. Это призрачный тип с полупрозрачным телом, %s оттенками и светящимися глазами, которые отражают настроение. Фон полностью белый. Стиль: аниме.",
                mood, color);

        String imageId = generateImage(prompt);
        if (imageId == null) {
            throw new IOException("GigaChat did not return an image ID");
        }

        String imageBase64 = downloadImage(imageId);
        if (imageBase64 == null) {
            throw new IOException("Failed to download image from GigaChat");
        }

        String imageUrl = saveImageToFile(imageBase64, name);

        Pokemon pokemon = new Pokemon();
        pokemon.setName(name);
        pokemon.setMood(mood);
        pokemon.setColor(color);
        pokemon.setImage(imageUrl);

        Pokemon savedPokemon = pokemonRepository.save(pokemon);
        note.setPokemon(savedPokemon);
        noteService.updateNote(note);

        return mapEntityToDto(savedPokemon);
    }

    private String saveImageToFile(String imageBase64, String pokemonName) throws IOException {
        String base64Data = imageBase64.replaceFirst("^data:image/[^;]+;base64,", "");
        byte[] imageBytes = Base64.getDecoder().decode(base64Data);

        String fileName = pokemonName + "_" + System.currentTimeMillis() + ".jpg";
        String filePath = imageDir + fileName;

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(imageBytes);
        }

        logger.info("Image saved to {}", filePath);

        return baseUrl + fileName;
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
        logger.debug("GigaChat request for image generation: {}", requestBody);

        String responseBody = gigaChatService.sendGigaChatRequest(requestBody, gigaChatService.getToken(), 0);
        return extractImageIdFromResponse(responseBody);
    }

    private String extractImageIdFromResponse(String responseBody) throws IOException {
        JsonNode root = mapper.readTree(responseBody);
        JsonNode choices = root.path("choices");

        if (choices.isEmpty()) {
            throw new IOException("GigaChat response missing 'choices': " + responseBody);
        }

        JsonNode contentNode = choices.get(0).path("message").path("content");
        if (contentNode.isMissingNode()) {
            throw new IOException("GigaChat response missing 'content': " + responseBody);
        }

        String contentText = contentNode.asText();
        Pattern pattern = Pattern.compile("<img src=\"([0-9a-fA-F-]{36})\"");
        Matcher matcher = pattern.matcher(contentText);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new IOException("Failed to extract image ID from GigaChat response: " + contentText);
        }
    }

    private String downloadImage(String imageId) throws IOException {
        return gigaChatService.downloadFile(imageId, gigaChatService.getToken());
    }

    private PokemonDto mapEntityToDto(Pokemon pokemon) {
        return new PokemonDto(
                pokemon.getId(),
                pokemon.getName(),
                pokemon.getMood(),
                pokemon.getColor(),
                pokemon.getImage()
        );
    }

    private Pokemon mapDtoToEntity(PokemonDto dto) {
        Pokemon pokemon = new Pokemon();
        pokemon.setId(dto.getId());
        pokemon.setName(dto.getName());
        pokemon.setMood(dto.getMood());
        pokemon.setColor(dto.getColor());
        pokemon.setImage(dto.getImageUrl());
        return pokemon;
    }
}