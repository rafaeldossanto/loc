package com.trisha.Loc.loc.mapper;

import com.trisha.Loc.loc.entity.SessaoRastreamento;
import com.trisha.Loc.loc.model.dto.request.SessaoRequest;
import com.trisha.Loc.loc.model.dto.response.SessaoResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public class SessaoMapper {

    private SessaoMapper() {}

    public static SessaoRastreamento toEntity(SessaoRequest request) {
        return SessaoRastreamento.builder()
                .id(UUID.randomUUID().toString())
                .caminhoId(request.caminhoId())
                .usuarioId(request.usuarioId())
                .iniciadaEm(LocalDateTime.now())
                .build();
    }

    public static SessaoResponse toResponse(SessaoRastreamento entity) {
        return SessaoResponse.builder()
                .id(entity.getId())
                .caminhoId(entity.getCaminhoId())
                .usuarioId(entity.getUsuarioId())
                .status(entity.getStatus())
                .distanciaTotalKm(entity.getDistanciaTotalKm())
                .iniciadaEm(entity.getIniciadaEm())
                .finalizadaEm(entity.getFinalizadaEm())
                .build();
    }
}
