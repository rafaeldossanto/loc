package com.trisha.Loc.loc.repository;

import com.trisha.Loc.loc.entity.GpsPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GpsPointRepository extends JpaRepository<GpsPoint, String> {

    List<GpsPoint> findBySessionIdOrderByOrderAsc(String sessionId);

    int countBySessionId(String sessionId);

    /** Ponto inicial da sessao (ordem 1): referencia para o termino automatico. */
    Optional<GpsPoint> findFirstBySessionIdOrderByOrderAsc(String sessionId);
}
