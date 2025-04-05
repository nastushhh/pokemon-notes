package com.example.pokemonchiki.controller;

import com.example.pokemonchiki.model.Pokemon;
import com.example.pokemonchiki.repository.PokemonRepository;
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
    public PokemonController(PokemonService pokemonService, PokemonRepository pokemonRepository) {
        this.pokemonService = pokemonService;
    }

    @PostMapping
    public ResponseEntity<Pokemon> createPokemon(@RequestBody Pokemon pokemon){
        Pokemon savedPokemon = pokemonService.savePokemon(pokemon);
        return new ResponseEntity<>(savedPokemon, HttpStatus.CREATED);
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generatePokemon(@RequestBody String mood){
        try {
            Pokemon pokemon = pokemonService.generatePokemon(mood);
            return new ResponseEntity<>(pokemon, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>("Ошибка при генерации покемона: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<List<Pokemon>> getAllPokemon(){
        List<Pokemon> pokemons = pokemonService.getAllPokemons();
        return new ResponseEntity<>(pokemons, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pokemon> getPokemonById(@PathVariable Integer id){
        Optional<Pokemon> pokemon = pokemonService.getPokemonById(id);
        return pokemon.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePokemonById(@PathVariable Integer id){
        try {
            pokemonService.deletePokemon(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e){
            return new ResponseEntity<>("Ошибка при удалении покемона: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}