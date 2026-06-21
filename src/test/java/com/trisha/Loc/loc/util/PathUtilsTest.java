package com.trisha.Loc.loc.util;

import com.trisha.Loc.loc.entity.GpsPoint;
import com.trisha.Loc.loc.stub.SessionStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PathUtils (simplificacao Douglas-Peucker)")
class PathUtilsTest {

    private static final double TOLERANCE = 8.0;

    @Test
    @DisplayName("trecho retilineo deve manter apenas o primeiro e o ultimo ponto")
    void shouldKeepOnlyEndpointsOnStraightLine() {
        // 4 pontos no mesmo meridiano (longitude fixa): os 2 do meio sao redundantes.
        List<GpsPoint> line = List.of(
                SessionStub.aPoint(1, -20.4300, -41.7900).build(),
                SessionStub.aPoint(2, -20.4310, -41.7900).build(),
                SessionStub.aPoint(3, -20.4320, -41.7900).build(),
                SessionStub.aPoint(4, -20.4330, -41.7900).build()
        );

        List<GpsPoint> result = PathUtils.simplify(line, TOLERANCE);

        assertThat(result).extracting(GpsPoint::getOrder).containsExactly(1, 4);
    }

    @Test
    @DisplayName("vertice com desvio relevante (> tolerancia) deve ser preservado")
    void shouldKeepVertexWhenDeviationIsRelevant() {
        // B desvia ~111m da reta A-C (muito acima da tolerancia) -> mantem os 3.
        List<GpsPoint> withCurve = List.of(
                SessionStub.aPoint(1, -20.4300, -41.7900).build(),
                SessionStub.aPoint(2, -20.4290, -41.7850).build(),
                SessionStub.aPoint(3, -20.4300, -41.7800).build()
        );

        List<GpsPoint> result = PathUtils.simplify(withCurve, TOLERANCE);

        assertThat(result).extracting(GpsPoint::getOrder).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("desvio menor que a tolerancia deve ser descartado")
    void shouldRemovePointWithDeviationBelowTolerance() {
        // B desvia ~5,5m da reta A-C (abaixo dos 8m) -> descartado.
        List<GpsPoint> nearlyStraight = List.of(
                SessionStub.aPoint(1, -20.43000, -41.7900).build(),
                SessionStub.aPoint(2, -20.42995, -41.7850).build(),
                SessionStub.aPoint(3, -20.43000, -41.7800).build()
        );

        List<GpsPoint> result = PathUtils.simplify(nearlyStraight, TOLERANCE);

        assertThat(result).extracting(GpsPoint::getOrder).containsExactly(1, 3);
    }

    @Test
    @DisplayName("com menos de 3 pontos nao ha o que simplificar")
    void shouldReturnAllWithFewerThanThreePoints() {
        List<GpsPoint> twoPoints = List.of(
                SessionStub.aPoint(1, -20.43, -41.79).build(),
                SessionStub.aPoint(2, -20.43, -41.78).build()
        );

        assertThat(PathUtils.simplify(twoPoints, TOLERANCE)).hasSize(2);
    }
}
