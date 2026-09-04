package com.assistente.inventario.repository;

import com.assistente.inventario.model.Ativo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AtivoRepository extends JpaRepository<Ativo, Long> {
  Optional<Ativo> findByPatrimonio(String patrimonio);

  Optional<Ativo> findByServiceTagSerialIgonereCase(String serviceTagSerial);

  Optional<Ativo> findByImei1(String imei1);

  List<Ativo> findByColaboradorMatricula(String matricula);

  List<Ativo> findByTipoEquimanento(String tipoEquimanento);

  @Query(
"""
    SELECT a FROM Ativo a
    WHERE (:patrimonio IS NOT NULL AND a.patrimonio = :patrimonio)
    OR (:serviceTagSerial IS NOT NULL AND LOWER(a.serviceTagSerial) = LOWER(:serviceTagSerial))
    OR (:imei1 IS NOT NULL AND a.imei_1 = :imei1)
""")
  Optional<Ativo> buscarExistente(
      @Param("patrimonio") String patrimonio,
      @Param("serviceTag") String serviceTag,
      @Param("imei1") String imei1);
}
