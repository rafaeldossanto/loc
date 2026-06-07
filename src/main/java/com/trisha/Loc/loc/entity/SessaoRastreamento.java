package com.trisha.Loc.loc.entity;

import com.trisha.Loc.loc.model.enums.StatusSessao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessao_rastreamento", indexes = {
        @Index(name = "idx_sessao_caminho", columnList = "caminhoId"),
        @Index(name = "idx_sessao_usuario", columnList = "usuarioId")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessaoRastreamento {

    @Id
    private String id;

    @Column(nullable = false)
    private String caminhoId;

    @Column(nullable = false)
    private String usuarioId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatusSes