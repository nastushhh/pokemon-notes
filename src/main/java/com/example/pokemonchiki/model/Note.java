package com.example.pokemonchiki.model;

import jakarta.persistence.*;

@Entity
@Table(name = "notes")
public class Note {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "TEXT")
    private String note;
    private String mood;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "pokemon_id", referencedColumnName = "id")
    private Pokemon pokemon;

    public Note() {}

    public Note (String note, String mood, Pokemon pokemon) {
        this.note = note;
        this.mood = mood;
        this.pokemon = pokemon;
    }

    public Integer getId(){
        return id;
    }
    public void setId(Integer id){
        this.id = id;
    }
    public String getNote(){
        return note;
    }
    public void setNote(String note){
        this.note = note;
    }
    public String getMood(){
        return mood;
    }
    public void setMood(String mood) {
        this.mood = mood;
    }

    public Pokemon getPokemon(){
        return pokemon;
    }
    public void setPokemon(Pokemon pokemon){
        this.pokemon = pokemon;
    }
}
