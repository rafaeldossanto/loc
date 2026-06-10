package com.trisha.Loc.loc.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trisha.Loc.loc.model.dto.request.PontoGpsRequest;
import com.trisha.Loc.loc.model.dto.response.PontoGpsResponse;
import com.trisha.Loc.loc.service.LocalizacaoService;
import com.trisha.Loc.loc.websocket.LocalizacaoEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Trata cada ponto GPS recebido do broker MQTT: desserializa, persiste pela
 * MESMA regra de dominio ({@link LocalizacaoService#registrarPonto}) e difunde
 * ao vivo (Redis Pub/Sub -> WebSocket) para os amigos que acompanham.
 *
 * A falha no processamento de um ponto isolado e apenas logada e NAO derruba o
 * consumidor — um ponto problematico nao pode travar o stream da trilha.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LocalizacaoMqttHandler {

    private final LocalizacaoService localizacaoService;
    private final LocalizacaoEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public void processar(Message<String> mensagem) {
        try {
            PontoGpsRequest request = objectMapper.readValue(mensagem.getPayload(), PontoGpsRequest.class);
            PontoGpsResponse ponto = localizacaoService.registrarPonto(request);
            eventPublisher.publicar(ponto);
        } catch (Exception e) {
            log.error("Falha ao processar ponto recebido via MQTT: {}", e.getMessage());
        }
    }
}
