package com.assistente.inventario.repository;

import com.assistente.inventario.model.Ativo;
import com.assistente.inventario.model.enums.TipoEquipamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AtivoRepository extends JpaRepository<Ativo, Long> {
  Optional<Ativo> findByPatrimonio(String patrimonio);

  Optional<Ativo> findByServiceTagSerialIgnoreCase(String serviceTagSerial);

  Optional<Ativo> findByImei1(String imei1);

  List<Ativo> findByColaboradorMatricula(String matricula);

  List<Ativo> findByTipoEquipamento(TipoEquipamento tipoEquipamento);

  @Query(
"""
    SELECT a FROM Ativo a
    WHERE (:patrimonio IS NOT NULL AND a.patrimonio = :patrimonio)
    OR (:serviceTagSerial IS NOT NULL AND LOWER(a.serviceTagSerial) = LOWER(:serviceTagSerial))
    OR (:imei1 IS NOT NULL AND a.imei1 = :imei1)
""")
  Optional<Ativo> buscarExistente(
      @Param("patrimonio") String patrimonio,
      @Param("serviceTagSerial") String serviceTagSerial,
      @Param("imei1") String imei1);
}
