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

    public static SessaoResponse toResponse(Ses