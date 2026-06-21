package com.trisha.Loc.loc.mapper;

import com.trisha.Loc.loc.entity.GpsPoint;
import com.trisha.Loc.loc.entity.TrackingSession;
import com.trisha.Loc.loc.model.dto.request.GpsPointRequest;
import com.trisha.Loc.loc.model.dto.response.GpsPointResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public class GpsPointMapper {

    private GpsPointMapper() {}

    public static GpsPoint toEntity(GpsPointRequest request, TrackingSession session, int order) {
        return GpsPoint.builder()
                .id(UUID.randomUUID().toString())
                .session(session)
                .latitude(request.latitude())
                .longitude(request.longitude())
                .altitude(request.altitude())
                .accuracy(request.accuracy())
                .speed(request.speed())
                .order(order)
                .recordedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Resposta sem informacao de proximidade — usada em listagens de pontos,
     * onde o aviso de termino nao se aplica.
     */
    public static GpsPointResponse toResponse(GpsPoint entity) {
        return toResponse(entity, null, null);
    }

    /**
     * Resposta enriquecida com o estado de proximidade ao ponto inicial,
     * usada na resposta de registerPoint quando o termino automatico esta ligado.
     */
    public static GpsPointResponse toResponse(GpsPoint entity, Boolean nearStart, Double distanceFromStartMeters) {
        return GpsPointResponse.builder()
                .id(entity.getId())
                .sessionId(entity.getSession().getId())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .altitude(entity.getAltitude())
                .accuracy(entity.getAccuracy())
                .speed(entity.getSpeed())
                .order(entity.getOrder())
                .recordedAt(entity.getRecordedAt())
                .nearStart(nearStart)
                .distanceFromStartMeters(distanceFromStartMeters)
                .build();
    }
}
