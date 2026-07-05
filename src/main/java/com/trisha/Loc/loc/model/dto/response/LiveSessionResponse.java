package com.trisha.Loc.loc.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trisha.Loc.loc.model.enums.SessionVisibility;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Sessao em andamento que o usuario autenticado pode acompanhar ao vivo, com a
 * ultima posicao conhecida (para o marcador no mapa antes de assinar o topico
 * /topic/sessao/{sessaoId}).
 */
@Builder
public record LiveSessionResponse(
        @JsonProperty("sessaoId") String sessionId,
        @JsonProperty("caminhoId") String pathId,
        @JsonProperty("usuarioId") String userId,
        @JsonProperty("visibilidade") SessionVisibility visibility,
        @JsonProperty("iniciadaEm") LocalDateTime startedAt,
        Double latitude,
        Double longitude
) {}
