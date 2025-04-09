package com.example.pokemonchiki.model;

import jakarta.persistence.*;

@Entity
@Table(name = "pokemonchiki")
public class Pokemon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    private String mood;
    private String color;
    @Column(columnDefinition = "TEXT")
    private String image;

    public Pokemon() {}

    public Pokemon(String name, String mood, String color, String image) {
        this.name = name;
        this.mood = mood;
        this.color = color;
        this.image = image;
    }

    public Integer getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
}