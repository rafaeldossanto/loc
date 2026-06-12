package com.trisha.Loc.loc.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Registro de um ponto GPS na sessao. sessaoId e as coordenadas sao
 * obrigatorios; altitude, precisao e velocidade sao opcionais (nem todo
 * dispositivo fornece).
 */
public record PontoGpsRequest(
        @NotBlank String sessaoId,
        @NotNull Double latitude,
        @NotNull Double longitude,
        Double altitude,
        Double precisao,
        Double velocidade,
        String usuarioId
) {}
