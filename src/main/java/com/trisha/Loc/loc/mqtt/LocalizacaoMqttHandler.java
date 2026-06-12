package com.trisha.Loc.loc.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trisha.Loc.loc.model.dto.request.PontoGpsRequest;
import com.trisha.Loc.loc.model.dto.response.PontoGpsResponse;
import com.trisha.Loc.loc.repository.SessaoRastreamentoRepository;
import com.trisha.Loc.loc.service.LocalizacaoService;
import com.trisha.Loc.loc.websocket.LocalizacaoEventPublisher;
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
public class LocalizacaoMqttHandler {

    private final LocalizacaoService localizacaoService;
    private final LocalizacaoEventPublisher eventPublisher;
    private final SessaoRastreamentoRepository sessaoRepository;
    private final ObjectMapper objectMapper;

    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public void processar(Message<String> mensagem) {
        try {
            PontoGpsRequest request = objectMapper.readValue(mensagem.getPayload(), PontoGpsRequest.class);

            if (!ehDonoDaSessao(request)) {
                log.warn("Ponto MQTT descartado: usuario {} nao e dono da sessao {}", request.usuarioId(), request.sessaoId());
                return;
            }

            PontoGpsResponse ponto = localizacaoService.registrarPonto(request);
            eventPublisher.publicar(ponto);
        } catch (Exception e) {
            log.error("Falha ao processar ponto recebido via MQTT: {}", e.getMessage());
        }
    }

    private boolean ehDonoDaSessao(PontoGpsRequest request) {
        return sessaoRepository.findById(request.sessaoId())
                .map(sessao -> sessao.getUsuarioId().equals(request.usuarioId()))
                .orElse(false);
    }
}
