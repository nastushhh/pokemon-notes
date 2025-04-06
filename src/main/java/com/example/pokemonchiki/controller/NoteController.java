package com.example.pokemonchiki.controller;

import com.example.pokemonchiki.model.Note;
import com.example.pokemonchiki.service.NoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Note> createNote(@RequestBody String content) throws Exception {
        Note note = noteService.saveNote(content);
        return new ResponseEntity<>(note, HttpStatus.CREATED);
    }

    @PostMapping("/analyze")
    public ResponseEntity<String> analyzeMood(@RequestBody String content) throws Exception {
        String mood = noteService.analyzeNoteMood(content);
        return new ResponseEntity<>(mood, HttpStatus.OK);
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
                .orElseThrow(() -> new IllegalArgumentException("Заметка с ID " + id + " не найдена"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNoteById(@PathVariable Integer id) {
        noteService.deleteNote(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}