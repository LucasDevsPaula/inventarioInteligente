package com.assistente.inventario.model;

import com.assistente.inventario.model.enums.TipoEquipamento;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ativos")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Ativo {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "colaborador_id")
  private Colaborador colaborador;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_equipamento", nullable = false, length = 50)
  private TipoEquipamento tipoEquipamento;

  @Column(length = 100)
  private String fabricante;

  @Column(length = 150)
  private String modelo;

  @Column(length = 50)
  private String patrimonio;

  @Column(name = "service_tag_serial", length = 100)
  private String serviceTagSerial;

  @Column(name = "numero_serie", length = 100)
  private String numeroSerie;

  @Column(name = "imei_1", length = 20)
  private String imei1;

  @Column(name = "mac_address", length = 50)
  private String macAddress;

  @Column(name = "linha_corporativa", length = 30)
  private String linhaCorporativa;

  @Column(length = 100)
  private String processador;

  @Column(length = 30)
  private String status;

  @Column(name = "data_atualizacao")
  private LocalDateTime dataAtualizacao;

  @OneToMany(mappedBy = "ativo", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RegistroInventario> fotos;

  @PrePersist
  @PreUpdate
  public void preUpdate() {
    this.dataAtualizacao = LocalDateTime.now();
    if (this.status == null) {
      this.status = "EM_USO";
    }
  }
}
