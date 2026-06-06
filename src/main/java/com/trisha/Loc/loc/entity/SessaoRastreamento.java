package com.trisha.Loc.loc.entity;

import com.trisha.Loc.loc.model.enums.StatusSessao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessao_rastreamento")
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
    private StatusSessao status = StatusSessao.EM_ANDAMENTO;

    /**
     * Quando ligado, o servico avisa (sem finalizar sozinho) ao detectar que o
     * usuario voltou para perto do ponto inicial do caminho. Default: desligado.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean terminoAutomatico = false;

    /**
     * Raio, em metros, que dispara o aviso de termino automatico. Default: 5m.
     * Configuravel pelo usuario ao iniciar a sessao.
     */
    @Column(nullable = false)
    @Builder.Default
    private Double distanciaTerminoMetros = 5.0;

    private Double distanciaTotalKm;

    @Column(nullable = false)
    private LocalDateTime iniciadaEm;

    private LocalDateTime finalizadaEm;
}
