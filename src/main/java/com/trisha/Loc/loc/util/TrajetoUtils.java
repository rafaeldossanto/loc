package com.trisha.Loc.loc.util;

import com.trisha.Loc.loc.entity.PontoGps;
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
public class TrajetoUtils {

    /**
     * Retorna a sublista de pontos a MANTER. O primeiro e o ultimo sao sempre
     * preservados; um ponto intermediario so e mantido se desviar mais que
     * {@code toleranciaMetros} da reta entre os extremos do seu trecho.
     *
     * @param pontos lista ja ordenada pela ordem de captura
     */
    public static List<PontoGps> simplificar(List<PontoGps> pontos, double toleranciaMetros) {
        if (pontos.size() < 3) {
            return new ArrayList<>(pontos);
        }

        boolean[] manter = new boolean[pontos.size()];
        manter[0] = true;
        manter[pontos.size() - 1] = true;
        marcarRecursivo(pontos, 0, pontos.size() - 1, toleranciaMetros, manter);

        List<PontoGps> resultado = new ArrayList<>();
        for (int i = 0; i < pontos.size(); i++) {
            if (manter[i]) {
                resultado.add(pontos.get(i));
            }
        }
        return resultado;
    }

    private static void marcarRecursivo(List<PontoGps> pontos, int inicio, int fim,
                                        double tolerancia, boolean[] manter) {
        PontoGps a = pontos.get(inicio);
        PontoGps b = pontos.get(fim);

        double maiorDistancia = 0.0;
        int indiceMaior = -1;

        for (int i = inicio + 1; i < fim; i++) {
            PontoGps p = pontos.get(i);
            double distancia = GeoUtils.distanciaPontoSegmentoMetros(
                    p.getLatitude(), p.getLongitude(),
                    a.getLatitude(), a.getLongitude(),
                    b.getLatitude(), b.getLongitude());
            if (distancia > maiorDistancia) {
                maiorDistancia = distancia;
                indiceMaior = i;
            }
        }

        if (maiorDistancia > tolerancia && indiceMaior != -1) {
            manter[indiceMaior] = true;
            marcarRecursivo(pontos, inicio, indiceMaior, tolerancia, manter);
            marcarRecursivo(pontos, indiceMaior, fim, tolerancia, manter);
        }
    }
}
