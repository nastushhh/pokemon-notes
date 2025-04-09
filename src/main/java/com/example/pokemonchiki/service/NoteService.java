package com.example.pokemonchiki.service;

import com.example.pokemonchiki.model.Note;
import com.example.pokemonchiki.repository.NoteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class NoteService {

    private static final Logger logger = LoggerFactory.getLogger(NoteService.class);
    private final NoteRepository noteRepository;
    private final GigaChatService gigaChatService;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public NoteService(NoteRepository noteRepository, GigaChatService gigaChatService) {
        this.noteRepository = noteRepository;
        this.gigaChatService = gigaChatService;
    }

    private String analyzeText(String text) throws IOException {
        String token = gigaChatService.getToken();
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

        String responseBody = gigaChatService.sendGigaChatRequest(requestBody, token, 0);
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
        String mood = analyzeText(content);
        Note note = new Note(content, mood, null); // Без покемона при создании
        Note savedNote = noteRepository.save(note);
        logger.info("Сохранена заметка с id {} и настроением {}", savedNote.getId(), mood);
        return savedNote;
    }

    public Note updateNote(Note note) {
        Note updatedNote = noteRepository.save(note);
        logger.info("Обновлена заметка с id {} с покемоном id {}", updatedNote.getId(),
                updatedNote.getPokemon() != null ? updatedNote.getPokemon().getId() : null);
        return updatedNote;
    }

    public String analyzeNoteMood(String content) throws IOException {
        return analyzeText(content);
    }

    public List<Note> getAllNotes() {
        return noteRepository.findAll();
    }

    public Optional<Note> getNote(Integer id) {
        return noteRepository.findById(id);
    }

    public void deleteNote(Integer id) {
        noteRepository.deleteById(id);
    }
}