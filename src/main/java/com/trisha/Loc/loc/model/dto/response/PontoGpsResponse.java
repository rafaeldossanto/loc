package com.trisha.Loc.loc.model.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PontoGpsResponse(
        String id,
        String sessaoId,
        Double latitude,
        Double longitude,
        Double altitude,
        Double precisao,
        Double velocidade,
        Integer ordem,
        LocalDateTime registradoEm,
        /**
         * true quando o termino automatico esta ligado e este ponto esta dentro
         * do raio configurado em relacao ao ponto inicial. O app usa isso para
         * perguntar "finalizar ou continuar?" — o servico NUNCA finaliza sozinho.
         */
        Boolean proximoDoInicio,
        /** Distancia (em metros) deste ponto ao ponto inicial; null se nao calculada. */
        Double distanciaDoInicioMetros
) {}
