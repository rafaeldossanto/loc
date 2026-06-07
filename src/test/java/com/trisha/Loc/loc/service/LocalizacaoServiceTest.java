package com.trisha.Loc.loc.service;

import com.trisha.Loc.loc.entity.PontoGps;
import com.trisha.Loc.loc.entity.SessaoRastreamento;
import com.trisha.Loc.loc.model.dto.request.PontoGpsRequest;
import com.trisha.Loc.loc.model.dto.request.SessaoRequest;
import com.trisha.Loc.loc.model.dto.response.PontoGpsResponse;
import com.trisha.Loc.loc.model.dto.response.ProgressoSessaoResponse;
import com.trisha.Loc.loc.model.dto.response.SessaoResponse;
import com.trisha.Loc.loc.model.enums.StatusSessao;
import com.trisha.Loc.loc.repository.PontoGpsRepository;
import com.trisha.Loc.loc.repository.SessaoRastreamentoRepository;
import com.trisha.Loc.loc.stub.SessaoStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
        assertThat(response.distanciaTerminoMetros()).isEqualTo(10