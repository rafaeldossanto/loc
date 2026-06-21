package com.trisha.Loc.loc.service;

import com.trisha.Loc.loc.entity.GpsPoint;
import com.trisha.Loc.loc.entity.TrackingSession;
import com.trisha.Loc.loc.mapper.GpsPointMapper;
import com.trisha.Loc.loc.mapper.SessionMapper;
import com.trisha.Loc.loc.model.dto.request.GpsPointRequest;
import com.trisha.Loc.loc.model.dto.request.SessionRequest;
import com.trisha.Loc.loc.model.dto.response.GpsPointResponse;
import com.trisha.Loc.loc.model.dto.response.SessionProgressResponse;
import com.trisha.Loc.loc.model.dto.response.SessionResponse;
import com.trisha.Loc.loc.model.enums.SessionStatus;
import com.trisha.Loc.loc.repository.GpsPointRepository;
import com.trisha.Loc.loc.repository.TrackingSessionRepository;
import com.trisha.Loc.loc.util.GeoUtils;
import com.trisha.Loc.loc.util.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    private final TrackingSessionRepository sessionRepository;
    private final GpsPointRepository gpsPointRepository;

    /** Desvio maximo (m) para um ponto ser considerado redundante num trecho reto. */
    private static final double SIMPLIFICATION_TOLERANCE_METERS = 8.0;

    public SessionResponse startSession(String userId, SessionRequest request) {
        log.info("Iniciando sessao de rastreamento para caminho: {}", request.pathId());

        sessionRepository.findByUserIdAndStatus(userId, SessionStatus.EM_ANDAMENTO)
                .ifPresent(s -> {
                    throw new IllegalArgumentException("Usuario ja possui uma sessao em andamento");
                });

        sessionRepository.findByPathId(request.pathId())
                .ifPresent(s -> {
                    throw new IllegalArgumentException("Ja existe uma sessao para esse caminho");
                });

        var session = sessionRepository.save(SessionMapper.toEntity(request, userId));
        log.info("Sessao {} iniciada", session.getId());
        return SessionMapper.toResponse(session);
    }

    public GpsPointResponse registerPoint(GpsPointRequest request) {
        TrackingSession session = findActiveSession(request.sessionId());

        int order = gpsPointRepository.countBySessionId(session.getId()) + 1;
        GpsPoint point = gpsPointRepository.save(GpsPointMapper.toEntity(request, session, order));

        log.debug("Ponto GPS #{} registrado na sessao {}", order, session.getId());

        return buildResponseWithProximity(session, point, order);
    }

    private GpsPointResponse buildResponseWithProximity(TrackingSession session, GpsPoint point, int order) {
        boolean featureOff = !Boolean.TRUE.equals(session.getAutoFinish());
        boolean isInitialPoint = order <= 1;

        if (featureOff || isInitialPoint) {
            return GpsPointMapper.toResponse(point);
        }

        return gpsPointRepository.findFirstBySessionIdOrderByOrderAsc(session.getId())
                .map(initial -> {
                    double distance = GeoUtils.distanceMeters(
                            initial.getLatitude(), initial.getLongitude(),
                            point.getLatitude(), point.getLongitude());
                    boolean near = distance <= session.getFinishDistanceMeters();

                    if (near) {
                        log.info("Sessao {} a {}m do inicio (limite {}m) — sugerindo termino",
                                session.getId(), (int) distance, session.getFinishDistanceMeters());
                    }
                    return GpsPointMapper.toResponse(point, near, distance);
                })
                .orElseGet(() -> GpsPointMapper.toResponse(point));
    }

    public SessionResponse finishSession(String sessionId) {
        log.info("Finalizando sessao: {}", sessionId);
        TrackingSession session = findActiveSession(sessionId);

        List<GpsPoint> points = gpsPointRepository.findBySessionIdOrderByOrderAsc(sessionId);

        double totalDistance = calculateTotalDistance(points);
        simplifyPath(points);

        session.setStatus(SessionStatus.FINALIZADA);
        session.setTotalDistanceKm(totalDistance / 1000.0);
        session.setFinishedAt(LocalDateTime.now());

        log.info("Sessao {} finalizada — distancia: {}km", sessionId, session.getTotalDistanceKm());
        return SessionMapper.toResponse(sessionRepository.save(session));
    }

    /**
     * Remove do banco os pontos redundantes de trechos retilineos (Douglas-Peucker),
     * preservando a forma da trilha. Roda na finalizacao, com o trajeto ja completo.
     */
    private void simplifyPath(List<GpsPoint> points) {
        if (points.size() < 3) {
            return;
        }

        List<GpsPoint> kept = PathUtils.simplify(points, SIMPLIFICATION_TOLERANCE_METERS);
        Set<String> keptIds = kept.stream().map(GpsPoint::getId).collect(Collectors.toSet());

        List<GpsPoint> removed = points.stream()
                .filter(point -> !keptIds.contains(point.getId()))
                .toList();

        if (!removed.isEmpty()) {
            gpsPointRepository.deleteAll(removed);
            log.info("Trajeto da sessao simplificado: {} de {} pontos removidos ({} mantidos)",
                    removed.size(), points.size(), kept.size());
        }
    }

    public SessionResponse cancelSession(String sessionId) {
        log.info("Cancelando sessao: {}", sessionId);
        TrackingSession session = findActiveSession(sessionId);

        session.setStatus(SessionStatus.CANCELADA);
        session.setFinishedAt(LocalDateTime.now());

        return SessionMapper.toResponse(sessionRepository.save(session));
    }

    public SessionResponse getSessionByPath(String pathId) {
        return SessionMapper.toResponse(
                sessionRepository.findByPathId(pathId)
                        .orElseThrow(() -> new IllegalArgumentException("Sessao nao encontrada para esse caminho"))
        );
    }

    public List<GpsPointResponse> getPointsBySession(String sessionId) {
        return gpsPointRepository.findBySessionIdOrderByOrderAsc(sessionId)
                .stream().map(GpsPointMapper::toResponse).toList();
    }

    public List<GpsPointResponse> getPointsByPath(String pathId) {
        TrackingSession session = sessionRepository.findByPathId(pathId)
                .orElseThrow(() -> new IllegalArgumentException("Sessao nao encontrada para esse caminho"));

        return getPointsBySession(session.getId());
    }

    /**
     * Progresso em tempo real da sessao: distancia ja percorrida (recalculada a
     * partir dos pontos GPS) e tempo decorrido desde o inicio. Para o app exibir
     * durante a trilha em andamento. Em sessao finalizada, usa a distancia ja
     * gravada; em andamento, recalcula com os pontos atuais.
     */
    public SessionProgressResponse getProgress(String sessionId) {
        TrackingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sessao nao encontrada"));

        List<GpsPoint> points = gpsPointRepository.findBySessionIdOrderByOrderAsc(sessionId);

        double distanceKm = nonNull(session.getTotalDistanceKm())
                ? session.getTotalDistanceKm()
                : calculateTotalDistance(points) / 1000.0;

        LocalDateTime end = nonNull(session.getFinishedAt()) ? session.getFinishedAt() : LocalDateTime.now();
        long seconds = Duration.between(session.getStartedAt(), end).getSeconds();

        return SessionMapper.toProgress(session, distanceKm, seconds, points.size());
    }

    private double calculateTotalDistance(List<GpsPoint> points) {
        if (points.size() < 2) return 0.0;

        double total = 0.0;
        for (int i = 1; i < points.size(); i++) {
            total += GeoUtils.distanceMeters(
                    points.get(i - 1).getLatitude(), points.get(i - 1).getLongitude(),
                    points.get(i).getLatitude(), points.get(i).getLongitude()
            );
        }
        return total;
    }

    private TrackingSession findActiveSession(String sessionId) {
        TrackingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sessao nao encontrada"));

        if (!SessionStatus.EM_ANDAMENTO.equals(session.getStatus())) {
            throw new IllegalArgumentException("Sessao nao esta em andamento");
        }
        return session;
    }
}
