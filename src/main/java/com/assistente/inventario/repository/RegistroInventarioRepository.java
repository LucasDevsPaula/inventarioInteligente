package com.assistente.inventario.repository;

import com.assistente.inventario.model.RegistroInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistroInventarioRepository extends JpaRepository<RegistroInventario, Long> {
    List<RegistroInventario> findByAtivoIdOrderByCriadoEmDesc(Long ativoId);
    List<RegistroInventario> findTop50ByOrderByCriadoEmDesc();
}
