package com.trisha.Loc.loc.repository;

import com.trisha.Loc.loc.entity.GpsPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GpsPointRepository extends JpaRepository<GpsPoint, String> {

    List<GpsPoint> findBySessionIdOrderByOrderAsc(String sessionId);

    int countBySessionId(String sessionId);

    /** Ponto inicial da sessao (ordem 1): referencia para o termino automatico. */
    Optional<GpsPoint> findFirstBySessionIdOrderByOrderAsc(String sessionId);

    /** Ultimo ponto da sessao: posicao atual de uma trilha ao vivo. */
    Optional<GpsPoint> findFirstBySessionIdOrderByOrderDesc(String sessionId);

    /**
     * Pontos dentro da bounding box do mapa, agrupaveis por caminho (ordenados
     * por sessao e ordem). JOIN FETCH evita o N+1 ao ler session.pathId.
     */
    @Query("""
            SELECT p FROM GpsPoint p JOIN FETCH p.session s
            WHERE p.latitude BETWEEN :minLat AND :maxLat
              AND p.longitude BETWEEN :minLng AND :maxLng
            ORDER BY s.pathId, p.order
            """)
    List<GpsPoint> findInBoundingBox(double minLat, double minLng, double maxLat, double maxLng);
}
