package com.trisha.Loc.loc.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Trilha de um caminho dentro de uma bounding box: os pontos ja vem ordenados
 * e decimados (limite por caminho) para o mapa desenhar a polyline sem carregar
 * o trajeto inteiro. Quem decide se o caminho pode ser visto e o APP — aqui e
 * so geometria.
 */
public record TrailPointsResponse(
        @JsonProperty("caminhoId") String pathId,
        @JsonProperty("pontos") List<TrailPointResponse> points
) {}
