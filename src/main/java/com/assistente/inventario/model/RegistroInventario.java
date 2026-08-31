package com.assistente.inventario.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "registros_inventario")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegistroInventario {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ativo_id", nullable = false)
  private Ativo ativo;

  @Column(name = "nome_arquivo_original")
  private String nomeArquivoOriginal;

  @Column(name = "nome_arquivo_renomeado")
  private String nomeArquivoRenomeado;

  @Column(name = "caminho_foto", length = 500)
  private String caminhoFoto;

  @Column(length = 255)
  private String Localizacao;

  @Column(name = "criado_em")
  private LocalDateTime criadoEm;

  @PrePersist
  public void prePersist() {
    this.criadoEm = LocalDateTime.now();
  }
}
