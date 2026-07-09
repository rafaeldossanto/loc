package com.trisha.Loc.loc.repository;

/**
 * Projecao da consulta nativa por bounding box: so o que o mapa precisa para
 * desenhar (caminho + coordenadas + altitude), sem materializar entidades —
 * a consulta pode devolver milhares de linhas por viewport.
 */
public interface TrailPointView {

    String getPathId();

    Double getLatitude();

    Double getLongitude();

    Double getAltitude();
}
