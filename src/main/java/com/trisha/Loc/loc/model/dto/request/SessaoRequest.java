package com.trisha.Loc.loc.model.dto.request;

import com.trisha.Loc.loc.model.enums.VisibilidadeSessao;
import jakarta.validation.constraints.NotBlank;

/**
 * Inicio de sessao de rastreamento. O usuario (dono) vem do token, nao do
 * request. caminhoId e obrigatorio; terminoAutomatico, distanciaTerminoMetros e
 * visibilidade sao opcionais — defaults aplicados no mapper.
 */
public record SessaoRequest(
        @NotBlank String caminhoId,
        Boolean terminoAutomatico,
        Double distanciaTerminoMetros,
        VisibilidadeSessao visibilidade
) {}
