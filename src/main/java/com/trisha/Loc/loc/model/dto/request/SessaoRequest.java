package com.trisha.Loc.loc.model.dto.request;

import com.trisha.Loc.loc.model.enums.VisibilidadeSessao;
import jakarta.validation.constraints.NotBlank;

/**
 * Inicio de sessao de rastreamento. caminhoId e usuarioId sao obrigatorios.
 * terminoAutomatico, distanciaTerminoMetros e visibilidade sao opcionais:
 * quando nulos, o mapper aplica os defaults (desligado, 5m, PRIVADO).
 */
public record SessaoRequest(
        @NotBlank String caminhoId,
        @NotBlank String usuarioId,
        Boolean terminoAutomatico,
        Double distanciaTerminoMetros,
        VisibilidadeSessao visibilidade
) {}
