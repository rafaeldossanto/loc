package com.trisha.Loc.loc.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trisha.Loc.loc.config.RedisConfig;
import com.trisha.Loc.loc.model.dto.response.GpsPointResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica cada ponto registrado no canal Redis, para que TODAS as instancias do
 * servico (e seus assinantes WebSocket) recebam — nao so a instancia que tratou
 * a requisicao.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LocationEventPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(GpsPointResponse point) {
        try {
            redisTemplate.convertAndSend(RedisConfig.POINTS_CHANNEL, objectMapper.writeValueAsString(point));
        } catch (JsonProcessingException e) {
            // A falha na difusao do tempo real nao pode derrubar o registro do ponto,
            // que ja foi persistido. Apenas registra o erro.
            log.error("Falha ao publicar ponto da sessao {} no Redis: {}", point.sessionId(), e.getMessage());
        }
    }
}
