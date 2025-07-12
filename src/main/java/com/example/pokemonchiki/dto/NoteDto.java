package com.example.pokemonchiki.dto;

import com.example.pokemonchiki.model.Note;

public class NoteDto{
    private Integer id;
    private String content;
    private String mood;
    private Integer pokemonId;

    public NoteDto(){}

    public NoteDto(Integer id, String content, String mood, Integer pokemonId){
        this.id = id;
        this.content = content;
        this.mood = mood;
        this.pokemonId = pokemonId;
    }

    public static NoteDto fromEntity(Note note){
        if (note == null) return null;

        return new NoteDto(
                note.getId(),
                note.getContent(),
                note.getMood(),
                note.getPokemon() != null ? note.getPokemon().getId() : null
        );
    }

    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public String getMood() {
        return mood;
    }
    public void setMood(String mood) {
        this.mood = mood;
    }

    public Integer getPokemonId() {
        return pokemonId;
    }
    public void setPokemonId(Integer pokemonId) {
        this.pokemonId = pokemonId;
    }
}