package com.trisha.Loc.loc.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trisha.Loc.loc.model.dto.request.GpsPointRequest;
import com.trisha.Loc.loc.model.dto.response.GpsPointResponse;
import com.trisha.Loc.loc.repository.TrackingSessionRepository;
import com.trisha.Loc.loc.service.LocationService;
import com.trisha.Loc.loc.websocket.LocationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Trata cada ponto GPS recebido do broker MQTT. Como o MQTT nao carrega o token,
 * o ponto traz o usuarioId no payload e so e aceito se for o DONO da sessao —
 * caso contrario, e descartado. Em seguida persiste pela mesma regra de dominio
 * e difunde ao vivo (Redis Pub/Sub -> WebSocket).
 *
 * (A garantia forte exige ACL no broker Mosquitto — credencial por device e
 * restricao de topico; isso e camada de infra, complementar a esta validacao.)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LocationMqttHandler {

    private final LocationService locationService;
    private final LocationEventPublisher eventPublisher;
    private final TrackingSessionRepository sessionRepository;
    private final ObjectMapper objectMapper;

    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public void process(Message<String> message) {
        try {
            GpsPointRequest request = objectMapper.readValue(message.getPayload(), GpsPointRequest.class);

            if (!isSessionOwner(request)) {
                log.warn("Ponto MQTT descartado: usuario {} nao e dono da sessao {}", request.userId(), request.sessionId());
                return;
            }

            GpsPointResponse point = locationService.registerPoint(request);
            eventPublisher.publish(point);
        } catch (Exception e) {
            log.error("Falha ao processar ponto recebido via MQTT: {}", e.getMessage());
        }
    }

    private boolean isSessionOwner(GpsPointRequest request) {
        return sessionRepository.findById(request.sessionId())
                .map(session -> session.getUserId().equals(request.userId()))
                .orElse(false);
    }
}
