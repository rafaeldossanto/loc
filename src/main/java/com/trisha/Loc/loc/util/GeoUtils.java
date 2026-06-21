package com.trisha.Loc.loc.util;

import lombok.experimental.UtilityClass;

/**
 * Utilitarios de geolocalizacao. Concentra o calculo de distancia entre
 * coordenadas para que o LocationService nao misture trigonometria com
 * regra de negocio (distancia total da trilha, proximidade do ponto inicial).
 *
 * Nota de arquitetura: o mesmo calculo existe no servico APP. A duplicacao
 * ENTRE servicos e aceita de proposito — sao processos independentes
 * (microservicos) e a formula de Haversine e uma constante geometrica estavel.
 * Um modulo compartilhado acoplaria os servicos sem ganho real.
 */
@UtilityClass
public class GeoUtils {

    private static final int EARTH_RADIUS_METERS = 6371000;
    private static final double METERS_PER_DEGREE = EARTH_RADIUS_METERS * Math.PI / 180.0;

    /**
     * Distancia em metros entre dois pontos geograficos (formula de Haversine).
     */
    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * Distancia (em metros) de um ponto P ao segmento de reta A-B, via projecao
     * equiretangular local com origem em A — precisa o suficiente na escala de
     * uma trilha. E a medida que o Douglas-Peucker usa para decidir se um ponto
     * intermediario desvia o bastante da reta para valer a pena manter.
     */
    public static double pointToSegmentDistanceMeters(double latP, double lonP,
                                                      double latA, double lonA,
                                                      double latB, double lonB) {
        double mPerDegreeLon = METERS_PER_DEGREE * Math.cos(Math.toRadians(latA));

        double bx = (lonB - lonA) * mPerDegreeLon;
        double by = (latB - latA) * METERS_PER_DEGREE;
        double px = (lonP - lonA) * mPerDegreeLon;
        double py = (latP - latA) * METERS_PER_DEGREE;

        double squaredLength = bx * bx + by * by;
        if (squaredLength == 0.0) {
            return Math.hypot(px, py); // A e B coincidem: vira distancia ponto-ponto.
        }

        double t = (px * bx + py * by) / squaredLength;
        t = Math.max(0.0, Math.min(1.0, t)); // limita a projecao ao segmento [A,B]

        return Math.hypot(px - t * bx, py - t * by);
    }
}
