package com.trisha.Loc.loc.mapper;

import com.trisha.Loc.loc.entity.TrackingSession;
import com.trisha.Loc.loc.model.dto.request.SessionRequest;
import com.trisha.Loc.loc.model.dto.response.SessionProgressResponse;
import com.trisha.Loc.loc.model.dto.response.SessionResponse;
import com.trisha.Loc.loc.model.enums.SessionVisibility;

import java.time.LocalDateTime;
import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class SessionMapper {

    private SessionMapper() {}

    private static final boolean DEFAULT_AUTO_FINISH = false;
    private static final double DEFAULT_FINISH_DISTANCE_METERS = 5.0;
    private static final SessionVisibility DEFAULT_VISIBILITY = SessionVisibility.PRIVADO;

    public static TrackingSession toEntity(SessionRequest request, String userId) {
        return TrackingSession.builder()
                .id(UUID.randomUUID().toString())
                .pathId(request.pathId())
                .userId(userId)
                .autoFinish(nonNull(request.autoFinish()) ? request.autoFinish() : DEFAULT_AUTO_FINISH)
                .finishDistanceMeters(resolveFinishDistance(request.finishDistanceMeters()))
                .visibility(nonNull(request.visibility()) ? request.visibility() : DEFAULT_VISIBILITY)
                .startedAt(LocalDateTime.now())
                .build();
    }

    public static SessionResponse toResponse(TrackingSession entity) {
        return SessionResponse.builder()
                .id(entity.getId())
                .pathId(entity.getPathId())
                .userId(entity.getUserId())
                .status(entity.getStatus())
                .visibility(entity.getVisibility())
                .autoFinish(entity.getAutoFinish())
                .finishDistanceMeters(entity.getFinishDistanceMeters())
                .totalDistanceKm(entity.getTotalDistanceKm())
                .startedAt(entity.getStartedAt())
                .finishedAt(entity.getFinishedAt())
                .build();
    }

    public static SessionProgressResponse toProgress(TrackingSession session,
                                                     double traveledDistanceKm,
                                                     long elapsedTimeSeconds,
                                                     int totalPoints) {
        return SessionProgressResponse.builder()
                .sessionId(session.getId())
                .status(session.getStatus())
                .traveledDistanceKm(traveledDistanceKm)
                .elapsedTimeSeconds(elapsedTimeSeconds)
                .totalPoints(totalPoints)
                .build();
    }

    /**
     * Aplica o default de 5m quando nao informado. Um raio <= 0 nao faz sentido
     * (desligaria a deteccao mesmo com o recurso ligado), entao cai no default.
     */
    private static double resolveFinishDistance(Double informed) {
        if (isNull(informed) || informed <= 0) {
            return DEFAULT_FINISH_DISTANCE_METERS;
        }
        return informed;
    }
}
