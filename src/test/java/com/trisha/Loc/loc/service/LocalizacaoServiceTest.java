package com.trisha.Loc.loc.service;

import com.trisha.Loc.loc.entity.PontoGps;
import com.trisha.Loc.loc.entity.SessaoRastreamento;
import com.trisha.Loc.loc.model.dto.request.PontoGpsRequest;
import com.trisha.Loc.loc.model.dto.request.SessaoRequest;
import com.trisha.Loc.loc.model.dto.response.PontoGpsResponse;
import com.trisha.Loc.loc.model.dto.response.ProgressoSessaoResponse;
import com.trisha.Loc.loc.model.dto.response.SessaoResponse;
import com.trisha.Loc.loc.model.enums.StatusSessao;
import com.trisha.Loc.loc.model.enums.VisibilidadeSessao;
import com.trisha.Loc.loc.repository.PontoGpsRepository;
import com.trisha.Loc.loc.repository.SessaoRastreamentoRepository;
import com.trisha.Loc.loc.stub.SessaoStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocalizacaoService")
class LocalizacaoServiceTest {

    @Mock
    private SessaoRastreamentoRepository sessaoRepository;
    @Mock
    private PontoGpsRepository pontoGpsRepository;

    @InjectMocks
    private LocalizacaoService service;

    @Test
    @DisplayName("iniciarSessao deve criar sessao quando usuario e caminho estao livres")
    void deveIniciarSessao() {
        SessaoRequest request = SessaoStub.umRequest();
        when(sessaoRepository.findByUsuarioIdAndStatus(request.usuarioId(), StatusSessao.EM_ANDAMENTO))
                .thenReturn(Optional.empty());
        when(sessaoRepository.findByCaminhoId(request.caminhoId())).thenReturn(Optional.empty());
        when(sessaoRepository.save(any(SessaoRastreamento.class))).thenAnswer(inv -> inv.getArgument(0));

        SessaoResponse response = service.iniciarSessao(request);

        assertThat(response.caminhoId()).isEqualTo(request.caminhoId());
        assertThat(response.status()).isEqualTo(StatusSessao.EM_ANDAMENTO);
        verify(sessaoRepository).save(any(SessaoRastreamento.class));
    }

    @Test
    @DisplayName("iniciarSessao deve falhar quando usuario ja tem sessao em andamento")
    void deveFalharSessaoEmAndamento() {
        SessaoRequest request = SessaoStub.umRequest();
        when(sessaoRepository.findByUsuarioIdAndStatus(request.usuarioId(), StatusSessao.EM_ANDAMENTO))
                .thenReturn(Optional.of(SessaoStub.umaSessao().build()));

        assertThatThrownBy(() -> service.iniciarSessao(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ja possui uma sessao em andamento");

        verify(sessaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("iniciarSessao deve falhar quando ja existe sessao para o caminho")
    void deveFalharSessaoCaminhoDuplicado() {
        SessaoRequest request = SessaoStub.umRequest();
        when(sessaoRepository.findByUsuarioIdAndStatus(request.usuarioId(), StatusSessao.EM_ANDAMENTO))
                .thenReturn(Optional.empty());
        when(sessaoRepository.findByCaminhoId(request.caminhoId()))
                .thenReturn(Optional.of(SessaoStub.umaSessao().build()));

        assertThatThrownBy(() -> service.iniciarSessao(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ja existe uma sessao para esse caminho");

        verify(sessaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("registrarPonto deve calcular ordem sequencial e persistir")
    void deveRegistrarPonto() {
        PontoGpsRequest request = SessaoStub.umRequestPonto();
        SessaoRastreamento sessao = SessaoStub.umaSessao().build();
        when(sessaoRepository.findById(SessaoStub.SESSAO_ID)).thenReturn(Optional.of(sessao));
        when(pontoGpsRepository.countBySessaoId(SessaoStub.SESSAO_ID)).thenReturn(2);
        when(pontoGpsRepository.save(any(PontoGps.class))).thenAnswer(inv -> inv.getArgument(0));

        PontoGpsResponse response = service.registrarPonto(request);

        assertThat(response.ordem()).isEqualTo(3);
        assertThat(response.sessaoId()).isEqualTo(SessaoStub.SESSAO_ID);
    }

    @Test
    @DisplayName("registrarPonto com termino desligado nao deve calcular proximidade")
    void naoDeveCalcularProximidadeComTerminoDesligado() {
        PontoGpsRequest request = SessaoStub.umRequestPonto();
        SessaoRastreamento sessao = SessaoStub.umaSessao().terminoAutomatico(false).build();
        when(sessaoRepository.findById(SessaoStub.SESSAO_ID)).thenReturn(Optional.of(sessao));
        when(pontoGpsRepository.countBySessaoId(SessaoStub.SESSAO_ID)).thenReturn(5);
        when(pontoGpsRepository.save(any(PontoGps.class))).thenAnswer(inv -> inv.getArgument(0));

        PontoGpsResponse response = service.registrarPonto(request);

        assertThat(response.proximoDoInicio()).isNull();
        assertThat(response.distanciaDoInicioMetros()).isNull();
        verify(pontoGpsRepository, never()).findFirstBySessaoIdOrderByOrdemAsc(any());
    }

    @Test
    @DisplayName("registrarPonto deve avisar quando termino ligado e ponto dentro do raio")
    void deveAvisarProximoDoInicio() {
        // Ponto registrado coincide com o ponto inicial (distancia ~0m), dentro do raio de 5m.
        PontoGpsRequest request = SessaoStub.umRequestPonto();
        SessaoRastreamento sessao = SessaoStub.umaSessao()
                .terminoAutomatico(true).distanciaTerminoMetros(5.0).build();
        PontoGps inicial = SessaoStub.umPonto(1, SessaoStub.LATITUDE, SessaoStub.LONGITUDE).build();

        when(sessaoRepository.findById(SessaoStub.SESSAO_ID)).thenReturn(Optional.of(sessao));
        when(pontoGpsRepository.countBySessaoId(SessaoStub.SESSAO_ID)).thenReturn(9);
        when(pontoGpsRepository.save(any(PontoGps.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pontoGpsRepository.findFirstBySessaoIdOrderByOrdemAsc(SessaoStub.SESSAO_ID))
                .thenReturn(Optional.of(inicial));

        PontoGpsResponse response = service.registrarPonto(request);

        assertThat(response.proximoDoInicio()).isTrue();
        assertThat(response.distanciaDoInicioMetros()).isLessThan(5.0);
    }

    @Test
    @DisplayName("registrarPonto nao deve avisar quando termino ligado mas ponto fora do raio")
    void naoDeveAvisarQuandoLonge() {
        // Ponto inicial distante (~111km) do ponto atual — fora do raio de 5m.
        PontoGpsRequest request = SessaoStub.umRequestPonto();
        SessaoRastreamento sessao = SessaoStub.umaSessao()
                .terminoAutomatico(true).distanciaTerminoMetros(5.0).build();
        PontoGps inicial = SessaoStub.umPonto(1, SessaoStub.LATITUDE + 1.0, SessaoStub.LONGITUDE).build();

        when(sessaoRepository.findById(SessaoStub.SESSAO_ID)).thenReturn(Optional.of(sessao));
        when(pontoGpsRepository.countBySessaoId(SessaoStub.SESSAO_ID)).thenReturn(3);
        when(pontoGpsRepository.save(any(PontoGps.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pontoGpsRepository.findFirstBySessaoIdOrderByOrdemAsc(SessaoStub.SESSAO_ID))
                .thenReturn(Optional.of(inicial));

        PontoGpsResponse response = service.registrarPonto(request);

        assertThat(response.proximoDoInicio()).isFalse();
        assertThat(response.distanciaDoInicioMetros()).isGreaterThan(5.0);
    }

    @Test
    @DisplayName("registrarPonto nao deve avisar no proprio ponto inicial (ordem 1)")
    void naoDeveAvisarNoPontoInicial() {
        PontoGpsRequest request = SessaoStub.umRequestPonto();
        SessaoRastreamento sessao = SessaoStub.umaSessao()
                .terminoAutomatico(true).distanciaTerminoMetros(5.0).build();
        when(sessaoRepository.findById(SessaoStub.SESSAO_ID)).thenReturn(Optional.of(sessao));
        when(pontoGpsRepository.countBySessaoId(SessaoStub.SESSAO_ID)).thenReturn(0); // ordem 1
        when(pontoGpsRepository.save(any(PontoGps.class))).thenAnswer(inv -> inv.getArgument(0));

        PontoGpsResponse response = service.registrarPonto(request);

        assertThat(response.proximoDoInicio()).isNull();
        verify(pontoGpsRepository, never()).findFirstBySessaoIdOrderByOrdemAsc(any());
    }

    @Test
    @DisplayName("iniciarSessao deve aplicar defaults de termino automatico (desligado, 5m)")
    void deveAplicarDefaultsTermino() {
        SessaoRequest request = SessaoStub.umRequest();
        when(sessaoRepository.findByUsuarioIdAndStatus(request.usuarioId(), StatusSessao.EM_ANDAMENTO))
                .thenReturn(Optional.empty());
        when(sessaoRepository.findByCaminhoId(request.caminhoId())).thenReturn(Optional.empty());
        when(sessaoRepository.save(any(SessaoRastreamento.class))).thenAnswer(inv -> inv.getArgument(0));

        SessaoResponse response = service.iniciarSessao(request);

        assertThat(response.terminoAutomatico()).isFalse();
        assertThat(response.distanciaTerminoMetros()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("iniciarSessao deve respeitar termino ligado com raio customizado")
    void deveRespeitarTerminoCustomizado() {
        SessaoRequest request = SessaoStub.umRequestComTermino(10.0);
        when(sessaoRepository.findByUsuarioIdAndStatus(request.usuarioId(), StatusSessao.EM_ANDAMENTO))
                .thenReturn(Optional.empty());
        when(sessaoRepository.findByCaminhoId(request.caminhoId())).thenReturn(Optional.empty());
        when(sessaoRepository.save(any(SessaoRastreamento.class))).thenAnswer(inv -> inv.getArgument(0));

        SessaoResponse response = service.iniciarSessao(request);

        assertThat(response.terminoAutomatico()).isTrue();
        assertThat(response.distanciaTerminoMetros()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("iniciarSessao deve aplicar visibilidade PRIVADO por padrao")
    void deveAplicarVisibilidadePadrao() {
        SessaoRequest request = SessaoStub.umRequest();
        when(sessaoRepository.findByUsuarioIdAndStatus(request.usuarioId(), StatusSessao.EM_ANDAMENTO))
                .thenReturn(Optional.empty());
        when(sessaoRepository.findByCaminhoId(request.caminhoId())).thenReturn(Optional.empty());
        when(sessaoRepository.save(any(SessaoRastreamento.class))).thenAnswer(inv -> inv.getArgument(0));

        SessaoResponse response = service.iniciarSessao(request);

        assertThat(response.visibilidade()).isEqualTo(VisibilidadeSessao.PRIVADO);
    }

    @Test
    @DisplayName("iniciarSessao deve respeitar a visibilidade escolhida pelo usuario")
    void deveRespeitarVisibilidadeEscolhida() {
        SessaoRequest request = SessaoStub.umRequestComVisibilidade(VisibilidadeSessao.PUBLICO);
        when(sessaoRepository.findByUsuarioIdAndStatus(request.usuarioId(), StatusSessao.EM_ANDAMENTO))
                .thenReturn(Optional.empty());
        when(sessaoRepository.findByCaminhoId(request.caminhoId())).thenReturn(Optional.empty());
        when(sessaoRepository.save(any(SessaoRastreamento.class))).thenAnswer(inv -> inv.getArgument(0));

        SessaoResponse response = service.iniciarSessao(request);

        assertThat(response.visibilidade()).isEqualTo(VisibilidadeSessao.PUBLICO);
    }

    @Test
    @DisplayName("registrarPonto deve falhar quando sessao nao esta em andamento")
    void deveFalharRegistrarEmSessaoFinalizada() {
        PontoGpsRequest request = SessaoStub.umRequestPonto();
        SessaoRastreamento sessao = SessaoStub.umaSessao().status(StatusSessao.FINALIZADA).build();
        when(sessaoRepository.findById(SessaoStub.SESSAO_ID)).thenReturn(Optional.of(sessao));

        assertThatThrownBy(() -> service.registrarPonto(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao esta em andamento");

        verify(pontoGpsRepository, never()).save(any());
    }

    @Test
    @DisplayName("finalizarSessao deve calcular distancia via Haversine e marcar FINALIZADA")
    void deveFinalizarComDistancia() {
        SessaoRastreamento sessao = SessaoStub.umaSessao().build();
        // Dois pontos separados por ~1 grau de longitude no equador (~104km nessa latitude).
        List<PontoGps> pontos = List.of(
                SessaoStub.umPonto(1, -20.4350, -41.7920).build(),
                SessaoStub.umPonto(2, -20.4350, -41.7820).build()
        );
        when(sessaoRepository.findById(SessaoStub.SESSAO_ID)).thenReturn(Optional.of(sessao));
        when(pontoGpsRepository.findBySessaoIdOrderByOrdemAsc(SessaoStub.SESSAO_ID)).thenReturn(pontos);
        when(sessaoRepository.save(any(SessaoRastreamento.class))).thenAnswer(inv -> inv.getArgument(0));

        SessaoResponse response = service.finalizarSessao(SessaoStub.SESSAO_ID);

        assertThat(response.status()).isEqualTo(StatusSessao.FINALIZADA);
        assertThat(response.finalizadaEm()).isNotNull();
        // ~0.01 grau de longitude nessa latitude ~ 1.04 km. Faixa generosa para evitar fragilidade.
        assertThat(response.distanciaTotalKm()).isBetween(0.9, 1.2);
    }

    @Test
    @DisplayName("finalizarSessao deve resultar em distancia zero com menos de 2 pontos")
    void deveFinalizarSemPontos() {
        SessaoRastreamento sessao = SessaoStub.umaSessao().build();
        when(sessaoRepository.findById(SessaoStub.SESSAO_ID)).thenReturn(Optional.of(sessao));
        when(pontoGpsRepository.findBySessaoIdOrderByOrdemAsc(SessaoStub.SESSAO_ID))
                .thenReturn(List.of(SessaoStub.umPonto(1, -20.43, -41.79).build()));
        when(sessaoRepository.save(any(SessaoRastreamento.class))).thenAnswer(inv -> inv.getArgument(0));

        SessaoResponse response = service.finalizarSessao(SessaoStub.SESSAO_ID);

        assertThat(response.distanciaTotalKm()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("finalizarSessao deve simplificar o trajeto removendo pontos redundantes")
    void deveSimplificarTrajetoAoFinalizar() {
        SessaoRastreamento sessao = SessaoStub.umaSessao().build();
        // 4 pontos colineares: os de ordem 2 e 3 sao redundantes e devem sair.
        List<PontoGps> pontos = List.of(
                SessaoStub.umPonto(1, -20.4300, -41.7900).build(),
                SessaoStub.umPonto(2, -20.4310, -41.7900).build(),
                SessaoStub.umPonto(3, -20.4320, -41.7900).build(),
                SessaoStub.umPonto(4, -20.4330, -41.7900).build()
        );
        when(sessaoRepository.findById(SessaoStub.SESSAO_ID)).thenReturn(Optional.of(sessao));
        when(pontoGpsRepository.findBySessaoIdOrderByOrdemAsc(SessaoStub.SESSAO_ID)).thenReturn(pontos);
        when(sessaoRepository.save(any(SessaoRastreamento.class))).thenAnswer(inv -> inv.getArgument(0));

        service.finalizarSessao(SessaoStub.SESSAO_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PontoGps>> captor = ArgumentCaptor.forClass(List.class);
        verify(pontoGpsRepository).deleteAll(captor.capture());
        assertThat(captor.getValue()).extracting(PontoGps::getOrdem).containsExactly(2, 3);
    }

    @Test
    @DisplayName("cancelarSessao deve marcar CANCELADA")
    void deveCancelarSessao() {
        SessaoRastreamento sessao = SessaoStub.umaSessao().build();
        when(sessaoRepository.findById(SessaoStub.SESSAO_ID)).thenReturn(Optional.of(sessao));
        when(sessaoRepository.save(any(SessaoRastreamento.class))).thenAnswer(inv -> inv.getArgument(0));

        SessaoResponse response = service.cancelarSessao(SessaoStub.SESSAO_ID);

        assertThat(response.status()).isEqualTo(StatusSessao.CANCELADA);
        assertThat(response.finalizadaEm()).isNotNull();
    }

    @Test
    @DisplayName("findSessaoAtiva deve falhar quando sessao nao existe")
    void deveFalharSessaoInexistente() {
        when(sessaoRepository.findById("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelarSessao("inexistente"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sessao nao encontrada");
    }

    @Test
    @DisplayName("getSessaoByCaminho deve retornar sessao do caminho")
    void deveBuscarPorCaminho() {
        when(sessaoRepository.findByCaminhoId(SessaoStub.CAMINHO_ID))
                .thenReturn(Optional.of(SessaoStub.umaSessao().build()));

        SessaoResponse response = service.getSessaoByCaminho(SessaoStub.CAMINHO_ID);

        assertThat(response.caminhoId()).isEqualTo(SessaoStub.CAMINHO_ID);
    }

    @Test
    @DisplayName("getSessaoByCaminho deve falhar quando nao ha sessao")
    void deveFalharBuscarPorCaminhoInexistente() {
        when(sessaoRepository.findByCaminhoId("sem-sessao")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSessaoByCaminho("sem-sessao"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sessao nao encontrada para esse caminho");
    }

    @Test
    @DisplayName("getPontosBySessao deve mapear lista ordenada")
    void deveListarPontosPorSessao() {
        when(pontoGpsRepository.findBySessaoIdOrderByOrdemAsc(SessaoStub.SESSAO_ID))
                .thenReturn(List.of(
                        SessaoStub.umPonto(1, -20.43, -41.79).build(),
                        SessaoStub.umPonto(2, -20.43, -41.78).build()
                ));

        List<PontoGpsResponse> response = service.getPontosBySessao(SessaoStub.SESSAO_ID);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).ordem()).isEqualTo(1);
    }

    @Test
    @DisplayName("getPontosByCaminho deve resolver a sessao e retornar seus pontos")
    void deveListarPontosPorCaminho() {
        when(sessaoRepository.findByCaminhoId(SessaoStub.CAMINHO_ID))
                .thenReturn(Optional.of(SessaoStub.umaSessao().build()));
        when(pontoGpsRepository.findBySessaoIdOrderByOrdemAsc(SessaoStub.SESSAO_ID))
                .thenReturn(List.of(SessaoStub.umPonto(1, -20.43, -41.79).build()));

        List<PontoGpsResponse> response = service.getPontosByCaminho(SessaoStub.CAMINHO_ID);

        assertThat(response).hasSize(1);
    }

    @Test
    @DisplayName("getProgresso em sessao em andamento recalcula a distancia pelos pontos")
    void deveCalcularProgressoEmAndamento() {
        // Sessao em andamento (sem distanciaTotalKm gravada): recalcula pelos pontos.
        SessaoRastreamento sessao = SessaoStub.umaSessao()
                .distanciaTotalKm(null)
                .iniciadaEm(java.time.LocalDateTime.now().minusMinutes(30))
                .build();
        List<PontoGps> pontos = List.of(
                SessaoStub.umPonto(1, -20.4350, -41.7920).build(),
                SessaoStub.umPonto(2, -20.4350, -41.7820).build()
        );
        when(sessaoRepository.findById(SessaoStub.SESSAO_ID)).thenReturn(Optional.of(sessao));
        when(pontoGpsRepository.findBySessaoIdOrderByOrdemAsc(SessaoStub.SESSAO_ID)).thenReturn(pontos);

        ProgressoSessaoResponse progresso = service.getProgresso(SessaoStub.SESSAO_ID);

        assertThat(progresso.totalPontos()).isEqualTo(2);
        assertThat(progresso.distanciaPercorridaKm()).isBetween(0.9, 1.2);
        assertThat(progresso.tempoDecorridoSegundos()).isGreaterThanOrEqualTo(1800L);
    }

    @Test
    @DisplayName("getProgresso em sessao finalizada usa a distancia gravada")
    void deveUsarDistanciaGravadaQuandoFinalizada() {
        SessaoRastreamento sessao = SessaoStub.umaSessao()
                .status(StatusSessao.FINALIZADA)
                .distanciaTotalKm(12.5)
                .finalizadaEm(java.time.LocalDateTime.now())
                .build();
        when(sessaoRepository.findById(SessaoStub.SESSAO_ID)).thenReturn(Optional.of(sessao));
        when(pontoGpsRepository.findBySessaoIdOrderByOrdemAsc(SessaoStub.SESSAO_ID)).thenReturn(List.of());

        ProgressoSessaoResponse progresso = service.getProgresso(SessaoStub.SESSAO_ID);

        assertThat(progresso.distanciaPercorridaKm()).isEqualTo(12.5);
        assertThat(progresso.status()).isEqualTo(StatusSessao.FINALIZADA);
    }

    @Test
    @DisplayName("getProgresso deve falhar quando sessao nao existe")
    void deveFalharProgressoInexistente() {
        when(sessaoRepository.findById("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProgresso("inexistente"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sessao nao encontrada");
    }
}
