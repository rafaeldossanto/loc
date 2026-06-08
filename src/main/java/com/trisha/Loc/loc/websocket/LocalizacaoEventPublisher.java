package com.trisha.Loc.loc.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trisha.Loc.loc.config.RedisConfig;
import com.trisha.Loc.loc.model.dto.response.PontoGpsResponse;
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
public class LocalizacaoEventPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void publicar(PontoGpsResponse ponto) {
        try {
            redisTemplate.convertAndSend(RedisConfig.CANAL_PONTOS, objectMapper.writeValueAsString(ponto));
        } catch (JsonProcessingException e) {
            // A falha na difusao do tempo real nao pode derrubar o registro do ponto,
            // que ja foi persistido. Apenas registra o erro.
            log.error("Falha ao publicar ponto da sessao {} no Redis: {}", ponto.sessaoId(), e.getMessage());
        }
    }
}
