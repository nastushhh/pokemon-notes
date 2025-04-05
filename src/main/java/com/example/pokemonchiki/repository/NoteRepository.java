package com.example.pokemonchiki.repository;

import com.example.pokemonchiki.model.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Integer> {
    List<Note> findByPokemonId(Integer pokemonId);
}
