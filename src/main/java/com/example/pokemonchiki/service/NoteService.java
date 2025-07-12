package com.example.pokemonchiki.service;

import com.example.pokemonchiki.dto.NoteDto;
import com.example.pokemonchiki.model.Note;
import com.example.pokemonchiki.model.Pokemon;
import com.example.pokemonchiki.repository.NoteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NoteService {

    private static final Logger logger = LoggerFactory.getLogger(NoteService.class);
    private final NoteRepository noteRepository;
    private final GigaChatService gigaChatService;
    private final ObjectMapper mapper = new ObjectMapper();

    public NoteService(NoteRepository noteRepository, GigaChatService gigaChatService) {
        this.noteRepository = noteRepository;
        this.gigaChatService = gigaChatService;
    }
    
    private String analyzeText(String text) throws IOException {
        String token = gigaChatService.getToken();

        ObjectNode requestJson = mapper.createObjectNode();
        requestJson.put("model", "GigaChat");

        ArrayNode messages = mapper.createArrayNode();

        ObjectNode systemMessage = mapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", "Ты аналитик настроений. Определи настроение текста на русском языке и верни одно слово (грустный, веселый, печальный, смешной, радостный, спокойный, умиротворенный, гордый, трогательный, напуганный, встревоженный), описывающее эмоциональный окрас.");
        messages.add(systemMessage);

        ObjectNode userMessage = mapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", text);
        messages.add(userMessage);

        requestJson.set("messages", messages);
        requestJson.put("stream", false);
        requestJson.put("update_interval", 0);

        String requestBody = mapper.writeValueAsString(requestJson);
        logger.debug("Запрос к GigaChat для анализа настроения: {}", requestBody);

        String responseBody = gigaChatService.sendGigaChatRequest(requestBody, token, 0);
        logger.debug("Ответ от GigaChat: {}", responseBody);

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

        String mood = content.asText().trim();
        if (mood.split("\\s+").length != 1) {
            throw new IOException("GigaChat вернул не одно слово: " + mood);
        }
        return mood;
    }
    
    @Transactional
    public NoteDto saveNote(String content) throws IOException {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Текст заметки не может быть пустым");
        }
        if (content.length() > 1000) {
            throw new IllegalArgumentException("Текст заметки слишком длинный, максимум 1000 символов");
        }

        String mood = analyzeText(content);
        Note note = new Note(content.trim(), mood, null);
        Note savedNote = noteRepository.save(note);

        logger.info("Сохранена заметка id={}, mood={}", savedNote.getId(), mood);

        return NoteDto.fromEntity(savedNote);
    }

    @Transactional
    public NoteDto updateNote(Note note) {
        Note updatedNote = noteRepository.save(note);
        logger.info("Обновлена заметка id={}", updatedNote.getId());
        return NoteDto.fromEntity(updatedNote);
    }

    public String analyzeNoteMood(String content) throws IOException {
        return analyzeText(content);
    }

    public List<NoteDto> getAllNotes() {
        return noteRepository.findAll().stream()
                .map(NoteDto::fromEntity)
                .collect(Collectors.toList());
    }

    public NoteDto getNoteById(Integer id) {
        Optional<Note> noteOpt = noteRepository.findById(id);
        return noteOpt.map(this::mapToDto).orElse(null);
    }

    @Transactional
    public void deleteNote(Integer id) {
        noteRepository.deleteById(id);
        logger.info("Удалена заметка id={}", id);
    }

    private NoteDto mapToDto(Note note) {
        NoteDto dto = new NoteDto();
        dto.setId(note.getId());
        dto.setContent(note.getContent());
        dto.setMood(note.getMood());

        Pokemon pokemon = note.getPokemon();
        dto.setPokemonId(pokemon != null ? pokemon.getId() : null);

        return dto;
    }
}
