package com.trisha.Loc.loc.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Registro de um ponto GPS na sessao. sessionId e as coordenadas sao
 * obrigatorios; altitude, accuracy e speed sao opcionais (nem todo
 * dispositivo fornece).
 */
public record GpsPointRequest(
        @JsonProperty("sessaoId") @NotBlank String sessionId,
        @NotNull Double latitude,
        @NotNull Double longitude,
        Double altitude,
        @JsonProperty("precisao") Double accuracy,
        @JsonProperty("velocidade") Double speed,
        @JsonProperty("usuarioId") String userId
) {}
