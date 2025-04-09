package com.example.pokemonchiki.controller;

import com.example.pokemonchiki.model.Pokemon;
import com.example.pokemonchiki.service.PokemonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/pokemons")
public class PokemonController {

    private final PokemonService pokemonService;

    @Autowired
    public PokemonController(PokemonService pokemonService) {
        this.pokemonService = pokemonService;
    }

    @PostMapping
    public ResponseEntity<Pokemon> createPokemon(@RequestBody Pokemon pokemon) {
        if (pokemon.getName() == null || pokemon.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Имя покемона не может быть пустым");
        }
        Pokemon savedPokemon = pokemonService.savePokemon(pokemon);
        return new ResponseEntity<>(savedPokemon, HttpStatus.CREATED);
    }

    @PostMapping("/generate")
    public ResponseEntity<Pokemon> generatePokemon(@RequestBody String json) throws Exception {
        String mood = extractField(json, "mood");
        String name = extractField(json, "name");
        String color = extractField(json, "color");

        if (mood == null || mood.trim().isEmpty()) {
            throw new IllegalArgumentException("Настроение не может быть пустым");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Имя не может быть пустым");
        }
        if (color == null || color.trim().isEmpty()) {
            throw new IllegalArgumentException("Цвет не может быть пустым");
        }

        Pokemon pokemon = pokemonService.generatePokemon(mood, name, color);
        return new ResponseEntity<>(pokemon, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Pokemon>> getAllPokemon() {
        List<Pokemon> pokemons = pokemonService.getAllPokemons();
        return new ResponseEntity<>(pokemons, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pokemon> getPokemonById(@PathVariable Integer id) {
        Optional<Pokemon> pokemon = pokemonService.getPokemonById(id);
        return pokemon.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseThrow(() -> new IllegalArgumentException("Покемон с ID " + id + " не найден"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePokemonById(@PathVariable Integer id) {
        pokemonService.deletePokemon(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private String extractField(String json, String field) {
        String pattern = "\"" + field + "\":\\s*\"([^\"]+)\"";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }
}