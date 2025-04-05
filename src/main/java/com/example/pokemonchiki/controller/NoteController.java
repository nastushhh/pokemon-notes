package com.example.pokemonchiki.controller;

import com.example.pokemonchiki.model.Note;
import com.example.pokemonchiki.service.NoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;

    @Autowired
    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping
    public ResponseEntity<?> createNote(@RequestBody String content) {
        try {
            String mood = noteService.analyzeNoteMood(content);
            Note note = noteService.saveNote(content);
            return new ResponseEntity<>(mood, HttpStatus.CREATED);
        } catch (IOException e) {
            return new ResponseEntity<>("Ошибка при анализе настроения: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeMood(@RequestBody String content) {
        try {
            String mood = noteService.analyzeNoteMood(content);
            return new ResponseEntity<>(mood, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>("Ошибка при анализе настроения: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    public ResponseEntity<List<Note>> getAllNotes() {
        List<Note> notes = noteService.getAllNotes();
        return new ResponseEntity<>(notes, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Note> getNoteById(@PathVariable Integer id) {
        Optional<Note> note = noteService.getNote(id);
        return note.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNoteById(@PathVariable Integer id) {
        try {
            noteService.deleteNote(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/token")
    public ResponseEntity<?> getAccessToken() {
        try {
            String token = noteService.getToken();
            return new ResponseEntity<>(token, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>("Ошибка при получении токена: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}