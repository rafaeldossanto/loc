package com.trisha.Loc.loc.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record GpsPointResponse(
        String id,
        @JsonProperty("sessaoId") String sessionId,
        Double latitude,
        Double longitude,
        Double altitude,
        @JsonProperty("precisao") Double accuracy,
        @JsonProperty("velocidade") Double speed,
        @JsonProperty("ordem") Integer order,
        @JsonProperty("registradoEm") LocalDateTime recordedAt,
        /**
         * true quando o termino automatico esta ligado e este ponto esta dentro
         * do raio configurado em relacao ao ponto inicial. O app usa isso para
         * perguntar "finalizar ou continuar?" — o servico NUNCA finaliza sozinho.
         */
        @JsonProperty("proximoDoInicio") Boolean nearStart,
        /** Distancia (em metros) deste ponto ao ponto inicial; null se nao calculada. */
        @JsonProperty("distanciaDoInicioMetros") Double distanceFromStartMeters
) {}
