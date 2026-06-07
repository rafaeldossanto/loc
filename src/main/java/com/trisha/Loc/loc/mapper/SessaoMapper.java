package com.trisha.Loc.loc.mapper;

import com.trisha.Loc.loc.entity.SessaoRastreamento;
import com.trisha.Loc.loc.model.dto.request.SessaoRequest;
import com.trisha.Loc.loc.model.dto.response.SessaoResponse;

import java.time.LocalDateTime;
import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class SessaoMapper {

    private SessaoMapper() {}

    private static final boolean TERMINO_AUTOMATICO_PADRAO = false;
    private static final double DISTANCIA_TERMINO_PADRAO_METROS = 5.0;

    public static SessaoRastreamento toEntity(SessaoRequest request) {
        return SessaoRastreamento.builder()
                .id(UUID.randomUUID().toString())
                .caminhoId(request.caminhoId())
                .usuarioId(request.usuarioId())
                .terminoAutomatico(nonNull(request.terminoAutomatico()) ? request.terminoAutomatico() : TERMINO_AUTOMATICO_PADRAO)
                .distanciaTerminoMetros(definirDistanciaTermino(request.distanciaTerminoMetros()))
                .iniciadaEm(LocalDateTime.now())
                .build();
    }

    public static SessaoResponse toResponse(SessaoRastreamento entity) {
        return SessaoResponse.builder()
                .id(entity.getId())
                .caminhoId(entity.getCaminhoId())
                .usuarioId(entity.getUsuarioId())
                .status(entity.getStatus())
                .terminoAutomatico(entity.getTerminoAutomatico())
                .distanciaTerminoMetros(entity.getDistanciaTerminoMetros())
                .distanciaTotalKm(entity.getDistanciaTotalKm())
                .iniciadaEm(entity.getIniciadaEm())
                .finalizadaEm(entity.getFinalizadaEm())
                .build();
    }

    /**
     * Aplica o default de 5m quando nao informado. Um raio <= 0 nao faz sentido
     * (desligaria a deteccao mesmo com o recurso ligado), entao cai no default.
     */
    private static double definirDistanciaTermino(Double informada) {
        if (isNull(informada) || informada <= 0) {
            return DISTANCIA_TERMINO_PADRAO_METROS;
        }
        return informada;
    }
}
