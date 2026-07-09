package com.trisha.Loc.loc.websocket;

import tools.jackson.databind.ObjectMapper;
import com.trisha.Loc.loc.model.dto.response.GpsPointResponse;
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
public class GpsPointRedisSubscriber implements MessageListener {

    private static final String SESSION_TOPIC_DESTINATION = "/topic/sessao/";

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            GpsPointResponse point = objectMapper.readValue(json, GpsPointResponse.class);
            messagingTemplate.convertAndSend(SESSION_TOPIC_DESTINATION + point.sessionId(), point);
        } catch (Exception e) {
            log.error("Falha ao reentregar ponto recebido do Redis: {}", e.getMessage());
        }
    }
}
