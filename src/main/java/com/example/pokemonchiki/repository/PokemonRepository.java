package com.example.pokemonchiki.repository;

import com.example.pokemonchiki.model.Pokemon;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface PokemonRepository extends JpaRepository<Pokemon, Integer> {

}