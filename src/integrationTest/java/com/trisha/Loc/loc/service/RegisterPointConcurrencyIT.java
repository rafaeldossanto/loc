package com.trisha.Loc.loc.service;

import com.trisha.Loc.loc.LocIntegrationTest;
import com.trisha.Loc.loc.entity.GpsPoint;
import com.trisha.Loc.loc.entity.TrackingSession;
import com.trisha.Loc.loc.model.dto.request.GpsPointRequest;
import com.trisha.Loc.loc.model.enums.SessionStatus;
import com.trisha.Loc.loc.model.enums.SessionVisibility;
import com.trisha.Loc.loc.repository.GpsPointRepository;
import com.trisha.Loc.loc.repository.TrackingSessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regressao da corrida na ordem do ponto GPS. A ordem vem de {@code count()+1};
 * sem serializacao, gravacoes concorrentes da mesma sessao (REST + MQTT) geram
 * ordens duplicadas. Este teste sobe um Postgres real e dispara N gravacoes
 * simultaneas na MESMA sessao: com o lock pessimista na sessao, as ordens saem
 * exatamente 1..N, sem duplicata nem buraco.
 */
@DisplayName("registerPoint (concorrencia)")
class RegisterPointConcurrencyIT extends LocIntegrationTest {

    private static final String USER_ID = "usuario-concorrente";
    private static final int CONCURRENT_POINTS = 50;

    @Autowired
    private LocationService locationService;
    @Autowired
    private TrackingSessionRepository sessionRepository;
    @Autowired
    private GpsPointRepository gpsPointRepository;

    @AfterEach
    void cleanUp() {
        gpsPointRepository.deleteAll();
        sessionRepository.deleteAll();
    }

    @Test
    @DisplayName("gravacoes concorrentes geram ordens unicas e sequenciais")
    void concurrentRegisterPointYieldsUniqueSequentialOrders() throws InterruptedException {
        TrackingSession session = sessionRepository.save(TrackingSession.builder()
                .id(UUID.randomUUID().toString())
                .pathId("caminho-concorrente")
                .userId(USER_ID)
                .status(SessionStatus.EM_ANDAMENTO)
                .autoFinish(false)
                .finishDistanceMeters(5.0)
                .visibility(SessionVisibility.PRIVADO)
                .startedAt(LocalDateTime.now())
                .build());

        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_POINTS);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(CONCURRENT_POINTS);
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < CONCURRENT_POINTS; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    locationService.registerPoint(USER_ID, new GpsPointRequest(
                            session.getId(), -20.4350, -41.7920, 800.0, 5.0, 1.2, USER_ID));
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    finished.countDown();
                }
            });
        }

        startGate.countDown(); // libera todas as threads de uma vez para maximizar a contencao
        boolean completed = finished.await(30, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertThat(completed).as("todas as gravacoes concluiram no tempo").isTrue();
        assertThat(failures.get()).as("nenhuma gravacao falhou").isZero();

        List<Integer> orders = gpsPointRepository.findBySessionIdOrderByOrderAsc(session.getId())
                .stream().map(GpsPoint::getOrder).toList();

        List<Integer> expected = IntStream.rangeClosed(1, CONCURRENT_POINTS).boxed().toList();
        assertThat(orders)
                .as("ordens 1..N sem duplicata nem buraco")
                .containsExactlyElementsOf(expected);
    }
}
