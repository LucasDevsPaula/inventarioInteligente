package com.assistente.inventario.repository;

import com.assistente.inventario.model.ModeloTac;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModeloTacRepository extends JpaRepository<ModeloTac, Long> {
    Optional<ModeloTac> findByTac(String tac);
}
