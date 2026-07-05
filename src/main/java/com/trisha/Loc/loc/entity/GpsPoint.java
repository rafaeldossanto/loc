package com.trisha.Loc.loc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ponto_gps", indexes = {
        @Index(name = "idx_ponto_gps_sessao", columnList = "sessao_id"),
        // Consulta por bounding box do mapa (lat/lng BETWEEN) a cada pan/zoom.
        @Index(name = "idx_ponto_gps_lat_lng", columnList = "latitude, longitude")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GpsPoint {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sessao_id", nullable = false)
    private TrackingSession session;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private Double altitude;

    @Column(name = "precisao")
    private Double accuracy;

    @Column(name = "velocidade")
    private Double speed;

    @Column(name = "ordem", nullable = false)
    private Integer order;

    @Column(name = "registrado_em", nullable = false)
    private LocalDateTime recordedAt;
}
