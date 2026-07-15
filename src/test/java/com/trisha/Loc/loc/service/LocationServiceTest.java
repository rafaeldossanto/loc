package com.trisha.Loc.loc.service;

import com.trisha.Loc.loc.client.AppFriendshipClient;
import com.trisha.Loc.loc.entity.GpsPoint;
import com.trisha.Loc.loc.entity.TrackingSession;
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
import com.trisha.Loc.loc.stub.SessionStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocationService")
class LocationServiceTest {

    @Mock
    private TrackingSessionRepository sessionRepository;
    @Mock
    private GpsPointRepository gpsPointRepository;
    @Mock
    private AppFriendshipClient appFriendshipClient;

    @InjectMocks
    private LocationService service;

    @Test
    @DisplayName("startSession deve criar sessao quando usuario e caminho estao livres")
    void shouldStartSession() {
        SessionRequest request = SessionStub.aRequest();
        when(sessionRepository.findByUserIdAndStatus(SessionStub.USER_ID, SessionStatus.EM_ANDAMENTO))
                .thenReturn(Optional.empty());
        when(sessionRepository.findByPathId(request.pathId())).thenReturn(Optional.empty());
        when(sessionRepository.save(any(TrackingSession.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionResponse response = service.startSession(SessionStub.USER_ID, request);

        assertThat(response.pathId()).isEqualTo(request.pathId());
        assertThat(response.status()).isEqualTo(SessionStatus.EM_ANDAMENTO);
        verify(sessionRepository).save(any(TrackingSession.class));
    }

    @Test
    @DisplayName("startSession deve falhar quando usuario ja tem sessao em andamento")
    void shouldFailSessionInProgress() {
        SessionRequest request = SessionStub.aRequest();
        when(sessionRepository.findByUserIdAndStatus(SessionStub.USER_ID, SessionStatus.EM_ANDAMENTO))
                .thenReturn(Optional.of(SessionStub.aSession().build()));

        assertThatThrownBy(() -> service.startSession(SessionStub.USER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ja possui uma sessao em andamento");

        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("startSession deve falhar quando ja existe sessao para o caminho")
    void shouldFailDuplicatePathSession() {
        SessionRequest request = SessionStub.aRequest();
        when(sessionRepository.findByUserIdAndStatus(SessionStub.USER_ID, SessionStatus.EM_ANDAMENTO))
                .thenReturn(Optional.empty());
        when(sessionRepository.findByPathId(request.pathId()))
                .thenReturn(Optional.of(SessionStub.aSession().build()));

        assertThatThrownBy(() -> service.startSession(SessionStub.USER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ja existe uma sessao para esse caminho");

        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerPoint deve calcular ordem sequencial e persistir")
    void shouldRegisterPoint() {
        GpsPointRequest request = SessionStub.aPointRequest();
        TrackingSession session = SessionStub.aSession().build();
        when(sessionRepository.findById(SessionStub.SESSION_ID)).thenReturn(Optional.of(session));
        when(gpsPointRepository.countBySessionId(SessionStub.SESSION_ID)).thenReturn(2);
        when(gpsPointRepository.save(any(GpsPoint.class))).thenAnswer(inv -> inv.getArgument(0));

        GpsPointResponse response = service.registerPoint(SessionStub.USER_ID, request);

        assertThat(response.order()).isEqualTo(3);
        assertThat(response.sessionId()).isEqualTo(SessionStub.SESSION_ID);
    }

    @Test
    @DisplayName("registerPoint com termino desligado nao deve calcular proximidade")
    void shouldNotComputeProximityWhenFinishOff() {
        GpsPointRequest request = SessionStub.aPointRequest();
        TrackingSession session = SessionStub.aSession().autoFinish(false).build();
        when(sessionRepository.findById(SessionStub.SESSION_ID)).thenReturn(Optional.of(session));
        when(gpsPointRepository.countBySessionId(SessionStub.SESSION_ID)).thenReturn(5);
        when(gpsPointRepository.save(any(GpsPoint.class))).thenAnswer(inv -> inv.getArgument(0));

        GpsPointResponse response = service.registerPoint(SessionStub.USER_ID, request);

        assertThat(response.nearStart()).isNull();
        assertThat(response.distanceFromStartMeters()).isNull();
        verify(gpsPointRepository, never()).findFirstBySessionIdOrderByOrderAsc(any());
    }

    @Test
    @DisplayName("registerPoint deve avisar quando termino ligado e ponto dentro do raio")
    void shouldWarnNearStart() {
        // Ponto registrado coincide com o ponto inicial (distancia ~0m), dentro do raio de 5m.
        GpsPointRequest request = SessionStub.aPointRequest();
        TrackingSession session = SessionStub.aSession()
                .autoFinish(true).finishDistanceMeters(5.0).build();
        GpsPoint initial = SessionStub.aPoint(1, SessionStub.LATITUDE, SessionStub.LONGITUDE).build();

        when(sessionRepository.findById(SessionStub.SESSION_ID)).thenReturn(Optional.of(session));
        when(gpsPointRepository.countBySessionId(SessionStub.SESSION_ID)).thenReturn(9);
        when(gpsPointRepository.save(any(GpsPoint.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gpsPointRepository.findFirstBySessionIdOrderByOrderAsc(SessionStub.SESSION_ID))
                .thenReturn(Optional.of(initial));

        GpsPointResponse response = service.registerPoint(SessionStub.USER_ID, request);

        assertThat(response.nearStart()).isTrue();
        assertThat(response.distanceFromStartMeters()).isLessThan(5.0);
    }

    @Test
    @DisplayName("registerPoint nao deve avisar quando termino ligado mas ponto fora do raio")
    void shouldNotWarnWhenFar() {
        // Ponto inicial distante (~111km) do ponto atual — fora do raio de 5m.
        GpsPointRequest request = SessionStub.aPointRequest();
        TrackingSession session = SessionStub.aSession()
                .autoFinish(true).finishDistanceMeters(5.0).build();
        GpsPoint initial = SessionStub.aPoint(1, SessionStub.LATITUDE + 1.0, SessionStub.LONGITUDE).build();

        when(sessionRepository.findById(SessionStub.SESSION_ID)).thenReturn(Optional.of(session));
        when(gpsPointRepository.countBySessionId(SessionStub.SESSION_ID)).thenReturn(3);
        when(gpsPointRepository.save(any(GpsPoint.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gpsPointRepository.findFirstBySessionIdOrderByOrderAsc(SessionStub.SESSION_ID))
                .thenReturn(Optional.of(initial));

        GpsPointResponse response = service.registerPoint(SessionStub.USER_ID, request);

        assertThat(response.nearStart()).isFalse();
        assertThat(response.distanceFromStartMeters()).isGreaterThan(5.0);
    }

    @Test
    @DisplayName("registerPoint nao deve avisar no proprio ponto inicial (ordem 1)")
    void shouldNotWarnAtInitialPoint() {
        GpsPointRequest request = SessionStub.aPointRequest();
        TrackingSession session = SessionStub.aSession()
                .autoFinish(true).finishDistanceMeters(5.0).build();
        when(sessionRepository.findById(SessionStub.SESSION_ID)).thenReturn(Optional.of(session));
        when(gpsPointRepository.countBySessionId(SessionStub.SESSION_ID)).thenReturn(0); // ordem 1
        when(gpsPointRepository.save(any(GpsPoint.class))).thenAnswer(inv -> inv.getArgument(0));

        GpsPointResponse response = service.registerPoint(SessionStub.USER_ID, request);

        assertThat(response.nearStart()).isNull();
        verify(gpsPointRepository, never()).findFirstBySessionIdOrderByOrderAsc(any());
    }

    @Test
    @DisplayName("startSession deve aplicar defaults de termino automatico (desligado, 5m)")
    void shouldApplyFinishDefaults() {
        SessionRequest request = SessionStub.aRequest();
        when(sessionRepository.findByUserIdAndStatus(SessionStub.USER_ID, SessionStatus.EM_ANDAMENTO))
                .thenReturn(Optional.empty());
        when(sessionRepository.findByPathId(request.pathId())).thenReturn(Optional.empty());
        when(sessionRepository.save(any(TrackingSession.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionResponse response = service.startSession(SessionStub.USER_ID, request);

        assertThat(response.autoFinish()).isFalse();
        assertThat(response.finishDistanceMeters()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("startSession deve respeitar termino ligado com raio customizado")
    void shouldRespectCustomFinish() {
        SessionRequest request = SessionStub.aRequestWithFinish(10.0);
        when(sessionRepository.findByUserIdAndStatus(SessionStub.USER_ID, SessionStatus.EM_ANDAMENTO))
                .thenReturn(Optional.empty());
        when(sessionRepository.findByPathId(request.pathId())).thenReturn(Optional.empty());
        when(sessionRepository.save(any(TrackingSession.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionResponse response = service.startSession(SessionStub.USER_ID, request);

        assertThat(response.autoFinish()).isTrue();
        assertThat(response.finishDistanceMeters()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("startSession deve aplicar visibilidade PRIVADO por padrao")
    void shouldApplyDefaultVisibility() {
        SessionRequest request = SessionStub.aRequest();
        when(sessionRepository.findByUserIdAndStatus(SessionStub.USER_ID, SessionStatus.EM_ANDAMENTO))
                .thenReturn(Optional.empty());
        when(sessionRepository.findByPathId(request.pathId())).thenReturn(Optional.empty());
        when(sessionRepository.save(any(TrackingSession.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionResponse response = service.startSession(SessionStub.USER_ID, request);

        assertThat(response.visibility()).isEqualTo(SessionVisibility.PRIVADO);
    }

    @Test
    @DisplayName("startSession deve respeitar a visibilidade escolhida pelo usuario")
    void shouldRespectChosenVisibility() {
        SessionRequest request = SessionStub.aRequestWithVisibility(SessionVisibility.PUBLICO);
        when(sessionRepository.findByUserIdAndStatus(SessionStub.USER_ID, SessionStatus.EM_ANDAMENTO))
                .thenReturn(Optional.empty());
        when(sessionRepository.findByPathId(request.pathId())).thenReturn(Optional.empty());
        when(sessionRepository.save(any(TrackingSession.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionResponse response = service.startSession(SessionStub.USER_ID, request);

        assertThat(response.visibility()).isEqualTo(SessionVisibility.PUBLICO);
    }

    @Test
    @DisplayName("registerPoint deve falhar quando sessao nao esta em andamento")
    void shouldFailRegisterOnFinishedSession() {
        GpsPointRequest request = SessionStub.aPointRequest();
        TrackingSession session = SessionStub.aSession().status(SessionStatus.FINALIZADA).build();
        when(sessionRepository.findById(SessionStub.SESSION_ID)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.registerPoint(SessionStub.USER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao esta em andamento");

        verify(gpsPointRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerPoint deve recusar quando quem envia nao e o dono da sessao")
    void shouldRejectRegisterPointFromNonOwner() {
        GpsPointRequest request = SessionStub.aPointRequest();
        TrackingSession session = SessionStub.aSession().build();
        when(sessionRepository.findById(SessionStub.SESSION_ID)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.registerPoint("intruso", request))
                .isInstanceOf(com.trisha.Loc.loc.exception.ForbiddenException.class)
                .hasMessageContaining("Apenas o dono");

        verify(gpsPointRepository, never()).save(any());
    }

    @Test
    @DisplayName("finishSession deve recusar quando quem pede nao e o dono")
    void shouldRejectFinishFromNonOwner() {
        TrackingSession session = SessionStub.aSession().build();
        when(sessionRepository.findById(SessionStub.SESSION_ID)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.finishSession("intruso", SessionStub.SESSION_ID))
                .isInstanceOf(com.trisha.Loc.loc.exception.ForbiddenException.class)
                .hasMessageContaining("Apenas o dono");

        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelSession deve recusar quando quem pede nao e o dono")
    void shouldRejectCancelFromNonOwner() {
        TrackingSession session = SessionStub.aSession().build();
        when(sessionRepository.findById(SessionStub.SESSION_ID)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.cancelSession("intruso", SessionStub.SESSION_ID))
                .isInstanceOf(com.trisha.Loc.loc.exception.ForbiddenException.class)
                .hasMessageContaining("Apenas o dono");

        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("finishSession deve calcular distancia via Haversine e marcar FINALIZADA")
    void shouldFinishWithDistance() {
        TrackingSession session = SessionStub.aSession().build();
        // Dois pontos separados por ~1 grau de longitude no equador (~104km nessa latitude).
        List<GpsPoint> points = List.of(
                SessionStub.aPoint(1, -20.4350, -41.7920).build(),
                SessionStub.aPoint(2, -20.4350, -41.7820).build()
        );
        when(sessionRepository.findById(SessionStub.SESSION_ID)).thenReturn(Optional.of(session));
        when(gpsPointRepository.findBySessionIdOrderByOrderAsc(SessionStub.SESSION_ID)).thenReturn(points);
        when(sessionRepository.save(any(TrackingSession.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionResponse response = service.finishSession(SessionStub.USER_ID, SessionStub.SESSION_ID);

        assertThat(response.status()).isEqualTo(SessionStatus.FINALIZADA);
        assertThat(response.finishedAt()).isNotNull();
        // ~0.01 grau de longitude nessa latitude ~ 1.04 km. Faixa generosa para evitar fragilidade.
        assertThat(response.totalDistanceKm()).isBetween(0.9, 1.2);
    }

    @Test
    @DisplayName("finishSession deve resultar em distancia zero com menos de 2 pontos")
    void shouldFinishWithoutPoints() {
        TrackingSession session = SessionStub.aSession().build();
        when(sessionRepository.findById(SessionStub.SESSION_ID)).thenReturn(Optional.of(session));
        when(gpsPointRepository.findBySessionIdOrderByOrderAsc(SessionStub.SESSION_ID))
                .thenReturn(List.of(SessionStub.aPoint(1, -20.43, -41.79).build()));
        when(sessionRepository.save(any(TrackingSession.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionResponse response = service.finishSession(SessionStub.USER_ID, SessionStub.SESSION_ID);

        assertThat(response.totalDistanceKm()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("finishSession deve simplificar o trajeto removendo pontos redundantes")
    void shouldSimplifyPathOnFinish() {
        TrackingSession session = SessionStub.aSession().build();
        // 4 pontos colineares: os de ordem 2 e 3 sao redundantes e devem sair.
        List<GpsPoint> points = List.of(
                SessionStub.aPoint(1, -20.4300, -41.7900).build(),
                SessionStub.aPoint(2, -20.4310, -41.7900).build(),
                SessionStub.aPoint(3, -20.4320, -41.7900).build(),
                SessionStub.aPoint(4, -20.4330, -41.7900).build()
        );
        when(sessionRepository.findById(SessionStub.SESSION_ID)).thenReturn(Optional.of(session));
        when(gpsPointRepository.findBySessionIdOrderByOrderAsc(SessionStub.SESSION_ID)).thenReturn(points);
        when(sessionRepository.save(any(TrackingSession.class))).thenAnswer(inv -> inv.getArgument(0));

        service.finishSession(SessionStub.USER_ID, SessionStub.SESSION_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GpsPoint>> captor = ArgumentCaptor.forClass(List.class);
        verify(gpsPointRepository).deleteAll(captor.capture());
        assertThat(captor.getValue()).extracting(GpsPoint::getOrder).containsExactly(2, 3);
    }

    @Test
    @DisplayName("cancelSession deve marcar CANCELADA")
    void shouldCancelSession() {
        TrackingSession session = SessionStub.aSession().build();
        when(sessionRepository.findById(SessionStub.SESSION_ID)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(TrackingSession.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionResponse response = service.cancelSession(SessionStub.USER_ID, SessionStub.SESSION_ID);

        assertThat(response.status()).isEqualTo(SessionStatus.CANCELADA);
        assertThat(response.finishedAt()).isNotNull();
    }

    @Test
    @DisplayName("findActiveSession deve falhar quando sessao nao existe")
    void shouldFailMissingSession() {
        when(sessionRepository.findById("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelSession(SessionStub.USER_ID, "inexistente"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sessao nao encontrada");
    }

    @Test
    @DisplayName("getSessionByPath deve retornar sessao do caminho")
    void shouldFindByPath() {
        when(sessionRepository.findByPathId(SessionStub.PATH_ID))
                .thenReturn(Optional.of(SessionStub.aSession().build()));

        SessionResponse response = service.getSessionByPath(SessionStub.PATH_ID);

        assertThat(response.pathId()).isEqualTo(SessionStub.PATH_ID);
    }

    @Test
    @DisplayName("getSessionByPath deve falhar quando nao ha sessao")
    void shouldFailFindByMissingPath() {
        when(sessionRepository.findByPathId("sem-sessao")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSessionByPath("sem-sessao"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sessao nao encontrada para esse caminho");
    }

    @Test
    @DisplayName("getPointsBySession deve mapear lista ordenada para o dono")
    void shouldListPointsBySession() {
        TrackingSession session = SessionStub.aSession().build();
        when(sessionRepository.findById(SessionStub.SESSION_ID)).thenReturn(Optional.of(session));
        when(gpsPointRepository.findBySessionIdOrderByOrderAsc(SessionStub.SESSION_ID))
                .thenReturn(List.of(
                        SessionStub.aPoint(1, -20.43, -41.79).build(),
                        SessionStub.aPoint(2, -20.43, -41.78).build()
                ));

        List<GpsPointResponse> response =
                service.getPointsBySession(SessionStub.USER_ID, SessionStub.SESSION_ID, "tok");

        assertThat(response).hasSize(2);
        assertThat(response.get(0).order()).isEqualTo(1);
    }

    @Test
    @DisplayName("getPointsBySession deve recusar sessao privada para quem nao e o dono")
    void shouldRejectPrivateSessionPoints() {
        TrackingSession session = SessionStub.aSession().visibility(SessionVisibility.PRIVADO).build();
        when(sessionRepository.findById(SessionStub.SESSION_ID)).thenReturn(Optional.of(session));
        when(appFriendshipClient.canViewPath(SessionStub.PATH_ID, "tok")).thenReturn(false);

        assertThatThrownBy(() -> service.getPointsBySession("intruso", SessionStub.SESSION_ID, "tok"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sem acesso");

        verify(gpsPointRepository, never()).findBySessionIdOrderByOrderAsc(SessionStub.SESSION_ID);
    }

    @Test
    @DisplayName("getPointsByPath deve resolver a sessao e retornar seus pontos quando o caminho e visivel")
    void shouldListPointsByPath() {
        when(appFriendshipClient.canViewPath(SessionStub.PATH_ID, "tok")).thenReturn(true);
        when(sessionRepository.findByPathId(SessionStub.PATH_ID))
                .thenReturn(Optional.of(SessionStub.aSession().build()));
        when(gpsPointRepository.findBySessionIdOrderByOrderAsc(SessionStub.SESSION_ID))
                .thenReturn(List.of(SessionStub.aPoint(1, -20.43, -41.79).build()));

        List<GpsPointResponse> response =
                service.getPointsByPath(SessionStub.USER_ID, SessionStub.PATH_ID, "tok");

        assertThat(response).hasSize(1);
    }

    @Test
    @DisplayName("getPointsByPath deve recusar quando o caminho nao e visivel ao observador")
    void shouldRejectPointsByPathWhenNotVisible() {
        when(appFriendshipClient.canViewPath(SessionStub.PATH_ID, "tok")).thenReturn(false);

        assertThatThrownBy(() -> service.getPointsByPath("intruso", SessionStub.PATH_ID, "tok"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sem acesso");

        verify(sessionRepository, never()).findByPathId(SessionStub.PATH_ID);
    }

    @Test
    @DisplayName("getProgress em sessao em andamento recalcula a distancia pelos pontos")
    void shouldComputeProgressInProgress() {
        // Sessao em andamento (sem totalDistanceKm gravada): recalcula pelos pontos.
        TrackingSession session = SessionStub.aSession()
                .totalDistanceKm(null)
                .startedAt(java.time.LocalDateTime.now().minusMinutes(30))
                .build();
        List<GpsPoint> points = List.of(
                SessionStub.aPoint(1, -20.4350, -41.7920).build(),
                SessionStub.aPoint(2, -20.4350, -41.7820).build()
        );
        when(sessionRepository.findById(SessionStub.SESSION_ID)).thenReturn(Optional.of(session));
        when(gpsPointRepository.findBySessionIdOrderByOrderAsc(SessionStub.SESSION_ID)).thenReturn(points);

        SessionProgressResponse progress = service.getProgress(SessionStub.SESSION_ID);

        assertThat(progress.totalPoints()).isEqualTo(2);
        assertThat(progress.traveledDistanceKm()).isBetween(0.9, 1.2);
        assertThat(progress.elapsedTimeSeconds()).isGreaterThanOrEqualTo(1800L);
    }

    @Test
    @DisplayName("getProgress em sessao finalizada usa a distancia gravada")
    void shouldUseStoredDistanceWhenFinished() {
        TrackingSession session = SessionStub.aSession()
                .status(SessionStatus.FINALIZADA)
                .totalDistanceKm(12.5)
                .finishedAt(java.time.LocalDateTime.now())
                .build();
        when(sessionRepository.findById(SessionStub.SESSION_ID)).thenReturn(Optional.of(session));
        when(gpsPointRepository.findBySessionIdOrderByOrderAsc(SessionStub.SESSION_ID)).thenReturn(List.of());

        SessionProgressResponse progress = service.getProgress(SessionStub.SESSION_ID);

        assertThat(progress.traveledDistanceKm()).isEqualTo(12.5);
        assertThat(progress.status()).isEqualTo(SessionStatus.FINALIZADA);
    }

    @Test
    @DisplayName("getProgress deve falhar quando sessao nao existe")
    void shouldFailProgressMissing() {
        when(sessionRepository.findById("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProgress("inexistente"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sessao nao encontrada");
    }

    @Test
    @DisplayName("updateVisibility deve alterar a visibilidade quando o dono pede")
    void shouldUpdateVisibility() {
        TrackingSession session = SessionStub.aSession().build();
        when(sessionRepository.findById(SessionStub.SESSION_ID)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(TrackingSession.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionResponse response = service.updateVisibility(
                SessionStub.USER_ID, SessionStub.SESSION_ID, SessionVisibility.SEGUIDORES);

        assertThat(response.visibility()).isEqualTo(SessionVisibility.SEGUIDORES);
    }

    @Test
    @DisplayName("updateVisibility deve falhar quando quem pede nao e o dono")
    void shouldFailUpdateVisibilityNotOwner() {
        TrackingSession session = SessionStub.aSession().build();
        when(sessionRepository.findById(SessionStub.SESSION_ID)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.updateVisibility(
                "outro-usuario", SessionStub.SESSION_ID, SessionVisibility.PUBLICO))
                .isInstanceOf(com.trisha.Loc.loc.exception.ForbiddenException.class)
                .hasMessageContaining("Apenas o dono");

        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("getLiveSessions aplica as regras de visibilidade e exclui as proprias sessoes")
    void shouldListLiveSessionsRespectingVisibility() {
        TrackingSession mine = SessionStub.aSession().visibility(SessionVisibility.PUBLICO).build();
        TrackingSession publicSession = SessionStub.aSession()
                .id("sessao-publica").userId("outro-1").visibility(SessionVisibility.PUBLICO).build();
        TrackingSession followersSession = SessionStub.aSession()
                .id("sessao-seguidores").userId("outro-2").visibility(SessionVisibility.SEGUIDORES).build();
        TrackingSession privateSession = SessionStub.aSession()
                .id("sessao-privada").userId("outro-3").visibility(SessionVisibility.PRIVADO).build();

        when(sessionRepository.findByStatus(SessionStatus.EM_ANDAMENTO))
                .thenReturn(List.of(mine, publicSession, followersSession, privateSession));
        when(appFriendshipClient.followingIds("tok")).thenReturn(List.of("outro-2"));
        when(gpsPointRepository.findFirstBySessionIdOrderByOrderDesc("sessao-publica"))
                .thenReturn(Optional.of(SessionStub.aPoint(3, -20.1, -41.1).build()));
        when(gpsPointRepository.findFirstBySessionIdOrderByOrderDesc("sessao-seguidores"))
                .thenReturn(Optional.of(SessionStub.aPoint(5, -20.2, -41.2).build()));

        List<LiveSessionResponse> response = service.getLiveSessions(SessionStub.USER_ID, "tok");

        assertThat(response).hasSize(2);
        assertThat(response.get(0).sessionId()).isEqualTo("sessao-publica");
        assertThat(response.get(0).latitude()).isEqualTo(-20.1);
        assertThat(response.get(1).sessionId()).isEqualTo("sessao-seguidores");
    }

    @Test
    @DisplayName("getLiveSessions omite sessao ao vivo que ainda nao tem ponto")
    void shouldSkipLiveSessionWithoutPoints() {
        TrackingSession session = SessionStub.aSession()
                .id("sessao-vazia").userId("outro-1").visibility(SessionVisibility.PUBLICO).build();
        when(sessionRepository.findByStatus(SessionStatus.EM_ANDAMENTO)).thenReturn(List.of(session));
        when(gpsPointRepository.findFirstBySessionIdOrderByOrderDesc("sessao-vazia"))
                .thenReturn(Optional.empty());

        assertThat(service.getLiveSessions(SessionStub.USER_ID, "tok")).isEmpty();
    }

    @Test
    @DisplayName("getPointsInBoundingBox deve agrupar pontos por caminho preservando a ordem")
    void shouldGroupBboxPointsByPath() {
        when(gpsPointRepository.findInBoundingBox(-21.0, -42.0, -20.0, -41.0))
                .thenReturn(List.of(
                        view(SessionStub.PATH_ID, -20.10, -41.10),
                        view(SessionStub.PATH_ID, -20.20, -41.20),
                        view("caminho-2", -20.30, -41.30)));

        List<TrailPointsResponse> response = service.getPointsInBoundingBox(-21.0, -42.0, -20.0, -41.0, 200);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).pathId()).isEqualTo(SessionStub.PATH_ID);
        assertThat(response.get(0).points()).hasSize(2);
        assertThat(response.get(0).points().get(0).latitude()).isEqualTo(-20.10);
        assertThat(response.get(1).pathId()).isEqualTo("caminho-2");
        assertThat(response.get(1).points()).hasSize(1);
    }

    @Test
    @DisplayName("getPointsInBoundingBox deve decimar mantendo o primeiro e o ultimo ponto")
    void shouldDecimateBboxPoints() {
        List<TrailPointView> points = new java.util.ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            points.add(view(SessionStub.PATH_ID, -20.0 - i * 0.01, -41.0));
        }
        when(gpsPointRepository.findInBoundingBox(-21.0, -42.0, -20.0, -41.0)).thenReturn(points);

        List<TrailPointsResponse> response = service.getPointsInBoundingBox(-21.0, -42.0, -20.0, -41.0, 4);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).points()).hasSize(4);
        assertThat(response.get(0).points().get(0).latitude()).isEqualTo(-20.01);
        assertThat(response.get(0).points().get(3).latitude()).isEqualTo(-20.10);
    }

    @Test
    @DisplayName("getPointsInBoundingBox deve respeitar o teto de caminhos por resposta")
    void shouldCapPathsPerBbox() {
        List<TrailPointView> points = new java.util.ArrayList<>();
        for (int i = 1; i <= 60; i++) {
            points.add(view("caminho-" + i, -20.0, -41.0));
        }
        when(gpsPointRepository.findInBoundingBox(-21.0, -42.0, -20.0, -41.0)).thenReturn(points);

        List<TrailPointsResponse> response = service.getPointsInBoundingBox(-21.0, -42.0, -20.0, -41.0, 200);

        assertThat(response).hasSize(50);
    }

    private TrailPointView view(String pathId, double latitude, double longitude) {
        return new TrailPointView() {
            @Override
            public String getPathId() {
                return pathId;
            }

            @Override
            public Double getLatitude() {
                return latitude;
            }

            @Override
            public Double getLongitude() {
                return longitude;
            }

            @Override
            public Double getAltitude() {
                return 800.0;
            }
        };
    }

    @Test
    @DisplayName("getPointsInBoundingBox deve falhar com bbox invertida")
    void shouldFailInvalidBbox() {
        assertThatThrownBy(() -> service.getPointsInBoundingBox(-20.0, -41.0, -21.0, -42.0, 200))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bounding box invalida");
    }
}
