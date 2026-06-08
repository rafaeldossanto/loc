package com.trisha.Loc.loc.util;

import com.trisha.Loc.loc.entity.PontoGps;
import com.trisha.Loc.loc.stub.SessaoStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TrajetoUtils (simplificacao Douglas-Peucker)")
class TrajetoUtilsTest {

    private static final double TOLERANCIA = 8.0;

    @Test
    @DisplayName("trecho retilineo deve manter apenas o primeiro e o ultimo ponto")
    void deveManterApenasExtremosEmLinhaReta() {
        // 4 pontos no mesmo meridiano (longitude fixa): os 2 do meio sao redundantes.
        List<PontoGps> reta = List.of(
                SessaoStub.umPonto(1, -20.4300, -41.7900).build(),
                SessaoStub.umPonto(2, -20.4310, -41.7900).build(),
                SessaoStub.umPonto(3, -20.4320, -41.7900).build(),
                SessaoStub.umPonto(4, -20.4330, -41.7900).build()
        );

        List<PontoGps> resultado = TrajetoUtils.simplificar(reta, TOLERANCIA);

        assertThat(resultado).extracting(PontoGps::getOrdem).containsExactly(1, 4);
    }

    @Test
    @DisplayName("vertice com desvio relevante (> tolerancia) deve ser preservado")
    void deveManterVerticeQuandoHaDesvioRelevante() {
        // B desvia ~111m da reta A-C (muito acima da tolerancia) -> mantem os 3.
        List<PontoGps> comCurva = List.of(
                SessaoStub.umPonto(1, -20.4300, -41.7900).build(),
                SessaoStub.umPonto(2, -20.4290, -41.7850).build(),
                SessaoStub.umPonto(3, -20.4300, -41.7800).build()
        );

        List<PontoGps> resultado = TrajetoUtils.simplificar(comCurva, TOLERANCIA);

        assertThat(resultado).extracting(PontoGps::getOrdem).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("desvio menor que a tolerancia deve ser descartado")
    void deveRemoverPontoComDesvioMenorQueTolerancia() {
        // B desvia ~5,5m da reta A-C (abaixo dos 8m) -> descartado.
        List<PontoGps> quaseReta = List.of(
                SessaoStub.umPonto(1, -20.43000, -41.7900).build(),
                SessaoStub.umPonto(2, -20.42995, -41.7850).build(),
                SessaoStub.umPonto(3, -20.43000, -41.7800).build()
        );

        List<PontoGps> resultado = TrajetoUtils.simplificar(quaseReta, TOLERANCIA);

        assertThat(resultado).extracting(PontoGps::getOrdem).containsExactly(1, 3);
    }

    @Test
    @DisplayName("com menos de 3 pontos nao ha o que simplificar")
    void deveRetornarTudoComMenosDeTresPontos() {
        List<PontoGps> doisPontos = List.of(
                SessaoStub.umPonto(1, -20.43, -41.79).build(),
                SessaoStub.umPonto(2, -20.43, -41.78).build()
        );

        assertThat(TrajetoUtils.simplificar(doisPontos, TOLERANCIA)).hasSize(2);
    }
}
