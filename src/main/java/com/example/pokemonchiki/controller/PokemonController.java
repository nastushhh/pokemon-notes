package com.example.pokemonchiki.controller;

import com.example.pokemonchiki.dto.PokemonDto;
import com.example.pokemonchiki.dto.PokemonRequestDto;
import com.example.pokemonchiki.service.PokemonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/pokemons")
public class PokemonController {

    private static final Logger logger = LoggerFactory.getLogger(PokemonController.class);

    private final PokemonService pokemonService;

    public PokemonController(PokemonService pokemonService) {
        this.pokemonService = pokemonService;
    }

    @PostMapping("/generate")
    public ResponseEntity<PokemonDto> generatePokemon(@RequestBody PokemonRequestDto request) throws IOException {
        logger.info("Запрос на генерацию покемона: name={}, color={}, noteId={}",
                request.getName(), request.getColor(), request.getNoteId());

        PokemonDto pokemonDto = pokemonService.generatePokemon(
                request.getName(),
                request.getColor(),
                request.getNoteId()
        );

        return ResponseEntity.status(201).body(pokemonDto);
    }

    @GetMapping
    public ResponseEntity<List<PokemonDto>> getAllPokemons() {
        List<PokemonDto> pokemons = pokemonService.getAllPokemons();
        return ResponseEntity.ok(pokemons);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PokemonDto> getPokemonById(@PathVariable Integer id) {
        return pokemonService.getPokemonById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePokemonById(@PathVariable Integer id) {
        pokemonService.deletePokemon(id);
        return ResponseEntity.noContent().build();
    }
}
