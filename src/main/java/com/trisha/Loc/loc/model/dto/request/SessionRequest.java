package com.trisha.Loc.loc.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trisha.Loc.loc.model.enums.SessionVisibility;
import jakarta.validation.constraints.NotBlank;

/**
 * Inicio de sessao de rastreamento. O usuario (dono) vem do token, nao do
 * request. pathId e obrigatorio; autoFinish, finishDistanceMeters e
 * visibility sao opcionais — defaults aplicados no mapper.
 */
public record SessionRequest(
        @JsonProperty("caminhoId") @NotBlank String pathId,
        @JsonProperty("terminoAutomatico") Boolean autoFinish,
        @JsonProperty("distanciaTerminoMetros") Double finishDistanceMeters,
        @JsonProperty("visibilidade") SessionVisibility visibility
) {}
