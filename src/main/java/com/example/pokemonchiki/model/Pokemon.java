package com.example.pokemonchiki.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pokemonchiki")
public class Pokemon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private String type;
    private String mood;
    private String ability;
    private String image;

    public Pokemon() {}

    public Pokemon(String name, String type, String mood, String ability, String image) {
        this.name = name;
        this.type = type;
        this.mood = mood;
        this.ability = ability;
        this.image = image;

    }
    public Integer getId() { return id;}
    public void setId(int id) { this.id = id;}

    public String getName () {return name;}
    public void setName (String name) { this.name = name;}

    public String getType () {return type;}
    public void setType (String type) { this.type = type;}

    public String getMood() {return mood;}
    public void setMood (String mood) {this.mood = mood;}

    public String getAbility() {return ability;}
    public void setAbility (String ability) {this.ability = ability;}

    public String getImage() {return image;}
    public void setImage (String image) {this.image = image;}
}
