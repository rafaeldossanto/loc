package com.trisha.Loc.loc.model.dto.request;

public record PontoGpsRequest(
        String sessaoId,
        Double latitude,
        Double longitude,
        Double altitude,
        Double precisao,
        Double velocidade
) {}
