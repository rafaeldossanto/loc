package com.trisha.Loc.loc.model.dto.response;

/**
 * Ponto enxuto de uma trilha para desenho no mapa: so as coordenadas (e a
 * altitude, para perfis de elevacao futuros). Sem id/sessao/precisao — a
 * consulta por bounding box devolve muitos pontos e o payload importa.
 */
public record TrailPointResponse(
        Double latitude,
        Double longitude,
        Double altitude
) {}
