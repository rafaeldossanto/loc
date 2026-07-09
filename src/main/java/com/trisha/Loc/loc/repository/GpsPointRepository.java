package com.trisha.Loc.loc.repository;

import com.trisha.Loc.loc.entity.GpsPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * por caminho e ordem). Query nativa PostGIS: a expressao geometrica do
     * WHERE e IDENTICA a do indice GiST idx_ponto_gps_geo (migration V4) —
     * assim o planner usa o indice espacial em vez de varrer a tabela.
     */
    @Query(value = """
            SELECT s.caminho_id AS "pathId",
                   p.latitude   AS "latitude",
                   p.longitude  AS "longitude",
                   p.altitude   AS "altitude"
            FROM ponto_gps p
            JOIN sessao_rastreamento s ON s.id = p.sessao_id
            WHERE ST_SetSRID(ST_MakePoint(p.longitude, p.latitude), 4326)
                  && ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326)
            ORDER BY s.caminho_id, p.ordem
            """, nativeQuery = true)
    List<TrailPointView> findInBoundingBox(@Param("minLat") double minLat,
                                           @Param("minLng") double minLng,
                                           @Param("maxLat") double maxLat,
                                           @Param("maxLng") double maxLng);
}
