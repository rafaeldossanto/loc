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
    private static final double METROS_POR_GRAU = RAIO_TERRA_METROS * Math.PI / 180.0;

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

    /**
     * Distancia (em metros) de um ponto P ao segmento de reta A-B, via projecao
     * equiretangular local com origem em A — precisa o suficiente na escala de
     * uma trilha. E a medida que o Douglas-Peucker usa para decidir se um ponto
     * intermediario desvia o bastante da reta para valer a pena manter.
     */
    public static double distanciaPontoSegmentoMetros(double latP, double lonP,
                                                      double latA, double lonA,
                                                      double latB, double lonB) {
        double mPorGrauLon = METROS_POR_GRAU * Math.cos(Math.toRadians(latA));

        double bx = (lonB - lonA) * mPorGrauLon;
        double by = (latB - latA) * METROS_POR_GRAU;
        double px = (lonP - lonA) * mPorGrauLon;
        double py = (latP - latA) * METROS_POR_GRAU;

        double comprimentoQuadrado = bx * bx + by * by;
        if (comprimentoQuadrado == 0.0) {
            return Math.hypot(px, py); // A e B coincidem: vira distancia ponto-ponto.
        }

        double t = (px * bx + py * by) / comprimentoQuadrado;
        t = Math.max(0.0, Math.min(1.0, t)); // limita a projecao ao segmento [A,B]

        return Math.hypot(px - t * bx, py - t * by);
    }
}
