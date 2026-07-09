package com.trisha.Loc.loc.service;

import com.trisha.Loc.loc.client.AppFriendshipClient;
import com.trisha.Loc.loc.entity.GpsPoint;
import com.trisha.Loc.loc.entity.TrackingSession;
import com.trisha.Loc.loc.mapper.GpsPointMapper;
import com.trisha.Loc.loc.mapper.SessionMapper;
import com.trisha.Loc.loc.model.dto.request.GpsPointRequest;
import com.trisha.Loc.loc.model.dto.request.SessionRequest;
import com.trisha.Loc.loc.model.dto.response.GpsPointResponse;
import com.trisha.Loc.loc.model.dto.response.LiveSessionResponse;
import com.trisha.Loc.loc.model.dto.response.SessionProgressResponse;
import com.trisha.Loc.loc.model.dto.response.SessionResponse;
import com.trisha.Loc.loc.model.dto.response.TrailPointsResponse;
import com.trisha.Loc.loc.model.enums.SessionStatus;
import com.trisha.Loc.loc.model.enums.SessionVisibility;
import com.trisha.Loc.loc.repository.GpsPointRepository;
import com.trisha.Loc.loc.repository.TrackingSessionRepository;
import com.trisha.Loc.loc.repository.TrailPointView;
import com.trisha.Loc.loc.util.GeoUtils;
import com.trisha.Loc.loc.util.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    private final TrackingSessionRepository sessionRepository;
    private final GpsPointRepository gpsPointRepository;
    private final AppFriendshipClient appFriendshipClient;

    /** Desvio maximo (m) para um ponto ser considerado redundante num trecho reto. */
    private static final double SIMPLIFICATION_TOLERANCE_METERS = 8.0;

    /** Teto de caminhos por consulta de bbox — protege o payload em areas densas. */
    private static final int MAX_PATHS_PER_BBOX = 50;

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

    /**
     * Troca quem pode acompanhar a sessao ao vivo, com ela em andamento — o
     * usuario pode abrir (ex.: PRIVADO -> SEGUIDORES) ou fechar a trilha no
     * meio do caminho. So o dono altera; quem ja assinava um topico que ficou
     * mais restrito continua ate o proximo SUBSCRIBE (aceito no MVP).
     */
    public SessionResponse updateVisibility(String userId, String sessionId, SessionVisibility visibility) {
        log.info("Alterando visibilidade da sessao {} para {}", sessionId, visibility);
        TrackingSession session = findActiveSession(sessionId);

        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Apenas o dono pode alterar a visibilidade da sessao");
        }

        session.setVisibility(visibility);
        return SessionMapper.toResponse(sessionRepository.save(session));
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

    public SessionResponse getSession(String sessionId) {
        return SessionMapper.toResponse(
                sessionRepository.findById(sessionId)
                        .orElseThrow(() -> new IllegalArgumentException("Sessao nao encontrada"))
        );
    }

    /**
     * O usuario pode acompanhar esta sessao ao vivo? Mesma regra do SUBSCRIBE
     * no WebSocket — o BFF usa para liberar o catch-up (pontos ja gravados) de
     * uma sessao em andamento cuja aventura ainda nao e visivel.
     */
    public boolean canWatchSession(String userId, String sessionId, String bearerToken) {
        TrackingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sessao nao encontrada"));

        if (session.getUserId().equals(userId)) {
            return true;
        }
        // Checagem unitaria (uma sessao): as consultas por par bastam aqui.
        return switch (session.getVisibility()) {
            case PUBLICO -> true;
            case SEGUIDORES -> appFriendshipClient.isFollower(userId, session.getUserId(), bearerToken);
            case AMIGOS -> appFriendshipClient.areFriends(userId, session.getUserId(), bearerToken);
            case PRIVADO -> false;
        };
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
     * Sessoes em andamento que o usuario pode acompanhar ao vivo, com a ultima
     * posicao conhecida. Mesmas regras do SUBSCRIBE no WebSocket: PUBLICO para
     * todos, SEGUIDORES para quem segue o dono, AMIGOS para amigos, PRIVADO
     * nunca; as proprias sessoes ficam de fora (o dono ja se ve rastreando).
     * Sessao sem ponto registrado ainda nao aparece — nao ha o que mostrar.
     * As checagens sociais sao EM LOTE: no maximo duas chamadas ao APP
     * (amigos-ids e seguindo-ids), independente de quantas sessoes existam.
     */
    public List<LiveSessionResponse> getLiveSessions(String userId, String bearerToken) {
        List<TrackingSession> sessions = sessionRepository.findByStatus(SessionStatus.EM_ANDAMENTO).stream()
                .filter(session -> !session.getUserId().equals(userId))
                .toList();

        // So consulta o APP se alguma sessao exigir aquela relacao.
        Set<String> friends = sessions.stream().anyMatch(s -> SessionVisibility.AMIGOS.equals(s.getVisibility()))
                ? Set.copyOf(appFriendshipClient.friendIds(bearerToken))
                : Set.of();
        Set<String> following = sessions.stream().anyMatch(s -> SessionVisibility.SEGUIDORES.equals(s.getVisibility()))
                ? Set.copyOf(appFriendshipClient.followingIds(bearerToken))
                : Set.of();

        return sessions.stream()
                .filter(session -> canWatch(session, friends, following))
                .map(session -> gpsPointRepository
                        .findFirstBySessionIdOrderByOrderDesc(session.getId())
                        .map(last -> SessionMapper.toLive(session, last)))
                .flatMap(Optional::stream)
                .toList();
    }

    private boolean canWatch(TrackingSession session, Set<String> friends, Set<String> following) {
        return switch (session.getVisibility()) {
            case PUBLICO -> true;
            case SEGUIDORES -> following.contains(session.getUserId());
            case AMIGOS -> friends.contains(session.getUserId());
            case PRIVADO -> false;
        };
    }

    /**
     * Trilhas dentro da area visivel do mapa, agrupadas por caminho. Cada
     * caminho volta com os pontos em ordem e decimados (no maximo
     * maxPointsPerPath) — o app carrega so o viewport atual, nunca o mundo
     * inteiro. Ha tambem um teto de caminhos por resposta: numa area muito
     * trilhada, devolve os primeiros e ignora o resto (aumentar o zoom traz
     * os demais). Aqui e so geometria: quem filtra o que o usuario pode ver
     * (visibilidade da aventura) e o APP, orquestrado pelo BFF.
     */
    public List<TrailPointsResponse> getPointsInBoundingBox(
            double minLat, double minLng, double maxLat, double maxLng, int maxPointsPerPath) {
        if (minLat > maxLat || minLng > maxLng) {
            throw new IllegalArgumentException("Bounding box invalida: min maior que max");
        }

        List<TrailPointView> points = gpsPointRepository.findInBoundingBox(minLat, minLng, maxLat, maxLng);

        Map<String, List<TrailPointView>> byPath = new LinkedHashMap<>();
        for (TrailPointView point : points) {
            List<TrailPointView> group = byPath.get(point.getPathId());
            if (group == null) {
                if (byPath.size() >= MAX_PATHS_PER_BBOX) {
                    continue;
                }
                group = new ArrayList<>();
                byPath.put(point.getPathId(), group);
            }
            group.add(point);
        }

        return byPath.entrySet().stream()
                .map(entry -> new TrailPointsResponse(
                        entry.getKey(),
                        decimate(entry.getValue(), maxPointsPerPath).stream()
                                .map(GpsPointMapper::toTrailPoint)
                                .toList()))
                .toList();
    }

    /**
     * Reduz a trilha a no maximo max pontos com passo uniforme, preservando o
     * primeiro e o ultimo — suficiente para a polyline no zoom do viewport.
     */
    private List<TrailPointView> decimate(List<TrailPointView> points, int max) {
        if (max < 2 || points.size() <= max) {
            return points;
        }
        List<TrailPointView> kept = new ArrayList<>(max);
        double step = (double) (points.size() - 1) / (max - 1);
        for (int i = 0; i < max; i++) {
            kept.add(points.get((int) Math.round(i * step)));
        }
        return kept;
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
