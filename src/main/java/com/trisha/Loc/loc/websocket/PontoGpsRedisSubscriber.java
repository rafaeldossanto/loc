package com.trisha.Loc.loc.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trisha.Loc.loc.model.dto.response.PontoGpsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Recebe os pontos publicados no canal Redis (por qualquer instancia) e os
 * reentrega aos assinantes WebSocket DESTA instancia, no topico da sessao.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PontoGpsRedisSubscriber implements MessageListener {

    private static final String DESTINO_TOPICO_SESSAO = "/topic/sessao/";

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            PontoGpsResponse ponto = objectMapper.readValue(json, PontoGpsResponse.class);
            messagingTemplate.convertAndSend(DESTINO_TOPICO_SESSAO + ponto.sessaoId(), ponto);
        } catch (Exception e) {
            log.error("Falha ao reentregar ponto recebido do Redis: {}", e.getMessage());
        }
    }
}
