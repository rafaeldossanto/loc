package com.trisha.Loc.loc.util;

import com.trisha.Loc.loc.entity.GpsPoint;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplificacao do trajeto GPS pelo algoritmo de Douglas-Peucker.
 *
 * Motivo: o app registra um ponto a cada poucos segundos; num trecho retilineo
 * (ex.: 1km em linha reta) os pontos intermediarios sao redundantes — bastam o
 * inicio e o fim do trecho para descrever a mesma forma. Ao finalizar a sessao,
 * removemos esses pontos para nao inchar o banco, preservando os vertices onde
 * a trilha realmente muda de direcao.
 *
 * A distancia total da trilha deve ser calculada ANTES da simplificacao, com
 * todos os pontos — aqui so reduzimos o que e armazenado, nao a metrica.
 */
@UtilityClass
public class PathUtils {

    /**
     * Retorna a sublista de pontos a MANTER. O primeiro e o ultimo sao sempre
     * preservados; um ponto intermediario so e mantido se desviar mais que
     * {@code toleranceMeters} da reta entre os extremos do seu trecho.
     *
     * @param points lista ja ordenada pela ordem de captura
     */
    public static List<GpsPoint> simplify(List<GpsPoint> points, double toleranceMeters) {
        if (points.size() < 3) {
            return new ArrayList<>(points);
        }

        boolean[] keep = new boolean[points.size()];
        keep[0] = true;
        keep[points.size() - 1] = true;
        markRecursive(points, 0, points.size() - 1, toleranceMeters, keep);

        List<GpsPoint> result = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            if (keep[i]) {
                result.add(points.get(i));
            }
        }
        return result;
    }

    private static void markRecursive(List<GpsPoint> points, int start, int end,
                                      double tolerance, boolean[] keep) {
        GpsPoint a = points.get(start);
        GpsPoint b = points.get(end);

        double maxDistance = 0.0;
        int maxIndex = -1;

        for (int i = start + 1; i < end; i++) {
            GpsPoint p = points.get(i);
            double distance = GeoUtils.pointToSegmentDistanceMeters(
                    p.getLatitude(), p.getLongitude(),
                    a.getLatitude(), a.getLongitude(),
                    b.getLatitude(), b.getLongitude());
            if (distance > maxDistance) {
                maxDistance = distance;
                maxIndex = i;
            }
        }

        if (maxDistance > tolerance && maxIndex != -1) {
            keep[maxIndex] = true;
            markRecursive(points, start, maxIndex, tolerance, keep);
            markRecursive(points, maxIndex, end, tolerance, keep);
        }
    }
}
