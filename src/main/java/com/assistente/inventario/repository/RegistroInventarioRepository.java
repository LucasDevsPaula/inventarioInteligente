package com.assistente.inventario.repository;

import com.assistente.inventario.model.RegistroInventario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistroInventarioRepository extends JpaRepository<RegistroInventario, Long> {
    List<RegistroInventario> findByAtivoInOrderByCriadoEmDesc(Long ativoId);
    List<RegistroInventario> findTop50ByOrderByCriadoEmDesc();
}
