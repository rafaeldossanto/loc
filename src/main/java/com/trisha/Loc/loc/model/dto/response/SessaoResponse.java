package com.trisha.Loc.loc.model.dto.response;

import com.trisha.Loc.loc.model.enums.StatusSessao;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record SessaoResponse(
        String id,
        String caminhoId,
        String usuarioId,
        StatusSessao status,
        Boolean terminoAutomatico,
        Double distanciaTerminoMetros,
        Double distanciaTotalKm,
