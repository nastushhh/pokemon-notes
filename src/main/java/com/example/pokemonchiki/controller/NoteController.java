package com.example.pokemonchiki.controller;

import com.example.pokemonchiki.dto.NoteDto;
import com.example.pokemonchiki.dto.NoteRequestDto;
import com.example.pokemonchiki.service.NoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;

    @Autowired
    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping
    public ResponseEntity<NoteDto> createNote(@RequestBody NoteRequestDto request) throws IOException {
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        NoteDto noteDto = noteService.saveNote(request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED).body(noteDto);
    }

    @PostMapping("/analyze")
    public ResponseEntity<String> analyzeMood(@RequestBody NoteRequestDto request) throws IOException {
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Текст не может быть пустым.");
        }
        String mood = noteService.analyzeNoteMood(request.getContent());
        return ResponseEntity.ok(mood);
    }

    @GetMapping
    public ResponseEntity<List<NoteDto>> getAllNotes() {
        List<NoteDto> noteDtos = noteService.getAllNotes();
        return ResponseEntity.ok(noteDtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoteDto> getNoteById(@PathVariable Integer id) {
        NoteDto noteDto = noteService.getNoteById(id);
        if (noteDto == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        }
        return ResponseEntity.ok(noteDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNoteById(@PathVariable Integer id) {
        noteService.deleteNote(id);
        return ResponseEntity.noContent().build();
    }
}
