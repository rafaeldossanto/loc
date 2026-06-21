package com.trisha.Loc.loc.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trisha.Loc.loc.model.enums.SessionStatus;
import com.trisha.Loc.loc.model.enums.SessionVisibility;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record SessionResponse(
        String id,
        @JsonProperty("caminhoId") String pathId,
        @JsonProperty("usuarioId") String userId,
        SessionStatus status,
        @JsonProperty("visibilidade") SessionVisibility visibility,
        @JsonProperty("terminoAutomatico") Boolean autoFinish,
        @JsonProperty("distanciaTerminoMetros") Double finishDistanceMeters,
        @JsonProperty("distanciaTotalKm") Double totalDistanceKm,
        @JsonProperty("iniciadaEm") LocalDateTime startedAt,
        @JsonProperty("finalizadaEm") LocalDateTime finishedAt
) {}
