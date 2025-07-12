package com.example.pokemonchiki.model;

import jakarta.persistence.*;

@Entity
@Table(name = "notes")
public class Note{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(columnDefinition = "TEXT")
    private String content;
    private String mood;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pokemon_id", referencedColumnName = "id")
    private Pokemon pokemon;

    public Note(){}

    public Note(String content, String mood, Pokemon pokemon){
        this.content = content;
        this.mood = mood;
        this.pokemon = pokemon;
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

    public void setContent(String content){
        this.content = content;
    }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood){
        this.mood = mood;
    }

    public Pokemon getPokemon() {
        return pokemon;
    }

    public void setPokemon(Pokemon pokemon) {
        this.pokemon = pokemon;
    }
}