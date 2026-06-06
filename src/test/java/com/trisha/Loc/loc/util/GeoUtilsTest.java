package com.trisha.Loc.loc.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GeoUtils")
class GeoUtilsTest {

    @Test
    @DisplayName("distancia entre o mesmo ponto deve ser zero")
    void distanciaZero() {
        double d = GeoUtils.distanciaMetros(-20.4350, -41.7920, -20.4350, -41.7920);
        assertThat(d).isCloseTo(0.0, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    @DisplayName("0,01 grau de longitude nessa latitude deve dar ~1km")
    void distanciaUmGrauLongitude() {
        double d = GeoUtils.distanciaMetros(-20.4350, -41.7920, -20.4350, -41.7820);
        assertThat(d).isBetween(900.0, 1200.0);
    }

    @Test
    @DisplayName("1 grau de latitude deve dar ~111km")
    void distanciaUmGrauLatitude() {
        double d = GeoUtils.distanciaMetros(-20.0, -41.0, -21.0, -41.0);
        assertThat(d).isBetween(110_000.0, 112_000.0);
    }
}
