package com.trisha.Loc.loc.model.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Inicio de sessao de rastreamento. caminhoId e usuarioId sao obrigatorios.
 * terminoAutomatico e distanciaTerminoMetros sao opcionais: quando nulos, o
 * mapper aplica os defaults (desligado, 5m).
 */
public record SessaoRequest(
        @NotBlank String caminhoId,
        @NotBlank String usuarioId,
        Boolean terminoAutomatico,
        Double distanciaTerminoMetros
) {}
