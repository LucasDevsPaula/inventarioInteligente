package com.assistente.inventario.model;

import com.assistente.inventario.model.enums.TipoEquipamento;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "modelo_tac")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModeloTac {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 8)
  private String tac;

  @Column(nullable = false, length = 100)
  private String fabricante;

  @Column(nullable = false, length = 150)
  private String modelo;

  @Column(length = 100)
  private String processador;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_equipamento", nullable = false, length = 50)
  private TipoEquipamento tipoEquipamento;
}
