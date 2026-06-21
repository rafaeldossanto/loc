package com.trisha.Loc.loc.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trisha.Loc.loc.model.enums.SessionStatus;
import lombok.Builder;

/**
 * Progresso em tempo real de uma sessao de rastreamento — para o app exibir
 * "distancia percorrida" e "tempo decorrido" enquanto a trilha esta em andamento.
 */
@Builder
public record SessionProgressResponse(
        @JsonProperty("sessaoId") String sessionId,
        SessionStatus status,
        @JsonProperty("distanciaPercorridaKm") Double traveledDistanceKm,
        @JsonProperty("tempoDecorridoSegundos") Long elapsedTimeSeconds,
        @JsonProperty("totalPontos") Integer totalPoints
) {}
