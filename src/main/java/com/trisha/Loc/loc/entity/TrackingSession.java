package com.trisha.Loc.loc.entity;

import com.trisha.Loc.loc.model.enums.SessionStatus;
import com.trisha.Loc.loc.model.enums.SessionVisibility;
import com.trisha.Loc.loc.trace.TraceContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessao_rastreamento", indexes = {
        @Index(name = "idx_sessao_caminho", columnList = "caminho_id"),
        @Index(name = "idx_sessao_usuario", columnList = "usuario_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingSession {

    @Id
    private String id;

    @Column(name = "caminho_id", nullable = false)
    private String pathId;

    @Column(name = "usuario_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SessionStatus status = SessionStatus.EM_ANDAMENTO;

    /**
     * Quando ligado, o servico avisa (sem finalizar sozinho) ao detectar que o
     * usuario voltou para perto do ponto inicial do caminho. Default: desligado.
     */
    @Column(name = "termino_automatico", nullable = false)
    @Builder.Default
    private Boolean autoFinish = false;

    /**
     * Raio, em metros, que dispara o aviso de termino automatico. Default: 5m.
     * Configuravel pelo usuario ao iniciar a sessao.
     */
    @Column(name = "distancia_termino_metros", nullable = false)
    @Builder.Default
    private Double finishDistanceMeters = 5.0;

    /**
     * Quem pode acompanhar esta sessao ao vivo. Default seguro: PRIVADO — a
     * localizacao so e exposta se o usuario optar por PUBLICO ou AMIGOS.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "visibilidade", nullable = false)
    @Builder.Default
    private SessionVisibility visibility = SessionVisibility.PRIVADO;

    @Column(name = "distancia_total_km")
    private Double totalDistanceKm;

    @Column(name = "iniciada_em", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finalizada_em")
    private LocalDateTime finishedAt;

    @Column(name = "trace_id")
    private String traceId;

    @PrePersist
    void onCreate() {
        if (traceId == null) {
            traceId = TraceContext.current();
        }
    }
}
