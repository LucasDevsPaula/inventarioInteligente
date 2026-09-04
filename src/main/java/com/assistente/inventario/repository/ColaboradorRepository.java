package com.assistente.inventario.repository;

import com.assistente.inventario.model.Colaborador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ColaboradorRepository extends JpaRepository<Colaborador, Long> {
    Optional<Colaborador> findByMatricula(String matricula);
    boolean existsByMatricula(String matricula);
    List<Colaborador> findByNomeContainingIgnoreCase(String nome);
    List<Colaborador> findBySetorIgnoreCase(String setor);
}
