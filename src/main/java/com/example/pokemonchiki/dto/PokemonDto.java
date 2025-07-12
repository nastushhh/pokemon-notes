package com.example.pokemonchiki.dto;

public class PokemonDto{
    private Integer id;
    private String name;
    private String mood;
    private String color;
    private String imageUrl;

    public PokemonDto() {}

    public PokemonDto(Integer id, String name, String mood, String color, String imageUrl) {
        this.id = id;
        this.name = name;
        this.mood = mood;
        this.color = color;
        this.imageUrl = imageUrl;
    }

    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getMood() {
        return mood;
    }
    public void setMood(String mood) {
        this.mood = mood;
    }

    public String getColor() {
        return color;
    }
    public void setColor(String color) {
        this.color = color;
    }

    public String getImageUrl() {
        return imageUrl;
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}