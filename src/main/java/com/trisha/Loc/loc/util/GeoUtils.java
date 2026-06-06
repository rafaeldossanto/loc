package com.trisha.Loc.loc.util;

import lombok.experimental.UtilityClass;

/**
 * Utilitarios de geolocalizacao. Concentra o calculo de distancia entre
 * coordenadas para que o LocalizacaoService nao misture trigonometria com
 * regra de negocio (distancia total da trilha, proximidade do ponto inicial).
 *
 * Nota de arquitetura: o mesmo calculo existe no servico APP. A duplicacao
 * ENTRE servicos e aceita de proposito — sao processos independentes
 * (microservicos) e a formula de Haversine e uma constante geometrica estavel.
 * Um modulo compartilhado acoplaria os servicos sem ganho real.
 */
@UtilityClass
public class GeoUtils {

    private static final int RAIO_TERRA_METROS = 6371000;

    /**
     * Distancia em metros entre dois pontos geograficos (formula de Haversine).
     */
    public static double distanciaMetros(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return RAIO_TERRA_METROS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
