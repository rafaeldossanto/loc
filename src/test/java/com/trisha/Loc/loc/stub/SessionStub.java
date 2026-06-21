package com.trisha.Loc.loc.stub;

import com.trisha.Loc.loc.entity.GpsPoint;
import com.trisha.Loc.loc.entity.TrackingSession;
import com.trisha.Loc.loc.model.dto.request.GpsPointRequest;
import com.trisha.Loc.loc.model.dto.request.SessionRequest;
import com.trisha.Loc.loc.model.enums.SessionStatus;
import com.trisha.Loc.loc.model.enums.SessionVisibility;

import java.time.LocalDateTime;

/**
 * Facilitador de testes para TrackingSession e GpsPoint.
 * Builders ja preenchidos — usar direto com {@code .build()}
 * ou sobrescrever campos pontuais quando o teste precisar.
 */
public final class SessionStub {

    public static final String SESSION_ID = "sessao-1";
    public static final String PATH_ID = "caminho-1";
    public static final String USER_ID = "usuario-1";

    public static final double LATITUDE = -20.4350;
    public static final double LONGITUDE = -41.7920;

    private SessionStub() {
    }

    public static TrackingSession.TrackingSessionBuilder aSession() {
        return TrackingSession.builder()
                .id(SESSION_ID)
                .pathId(PATH_ID)
                .userId(USER_ID)
                .status(SessionStatus.EM_ANDAMENTO)
                .startedAt(LocalDateTime.now());
    }

    /** Request padrao: termino automatico desligado (defaults aplicados no mapper). */
    public static SessionRequest aRequest() {
        return new SessionRequest(PATH_ID, null, null, null);
    }

    /** Request com termino automatico ligado e raio customizado. */
    public static SessionRequest aRequestWithFinish(double distanceMeters) {
        return new SessionRequest(PATH_ID, true, distanceMeters, null);
    }

    /** Request com visibilidade explicita (termino automatico desligado). */
    public static SessionRequest aRequestWithVisibility(SessionVisibility visibility) {
        return new SessionRequest(PATH_ID, null, null, visibility);
    }

    public static GpsPoint.GpsPointBuilder aPoint(int order, double latitude, double longitude) {
        return GpsPoint.builder()
                .id("ponto-" + order)
                .session(aSession().build())
                .latitude(latitude)
                .longitude(longitude)
                .altitude(800.0)
                .accuracy(5.0)
                .speed(1.2)
                .order(order)
                .recordedAt(LocalDateTime.now());
    }

    public static GpsPointRequest aPointRequest() {
        return new GpsPointRequest(SESSION_ID, LATITUDE, LONGITUDE, 800.0, 5.0, 1.2, USER_ID);
    }
}
