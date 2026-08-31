package com.assistente.inventario.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "colaboradores")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Colaborador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nome;

    @Column(nullable = false, length = 150)
    private String cargo;

    @Column(length = 100)
    private String setor;

    @Column(name = "data_cadastro")
    private LocalDateTime dataCadastro;

    @OneToMany(mappedBy = "colaborador", cascade = CascadeType.ALL)
    private List<Ativo> ativos;

    @PrePersist
    public void prePersist() {
        this.dataCadastro = LocalDateTime.now();
    }
}
