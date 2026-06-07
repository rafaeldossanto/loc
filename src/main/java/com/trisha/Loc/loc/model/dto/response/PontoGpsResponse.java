package com.trisha.Loc.loc.model.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PontoGpsResponse(
        String id,
        String sessaoId,
        Double latitude,
        Double longitude,
        Double altitude,
        Double precisao,
        Double velocidade,
        Integer ordem,
        LocalDateTime registradoEm,
    