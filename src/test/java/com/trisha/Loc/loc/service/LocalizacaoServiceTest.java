package com.trisha.Loc.loc.service;

import com.trisha.Loc.loc.entity.PontoGps;
import com.trisha.Loc.loc.entity.SessaoRastreamento;
import com.trisha.Loc.loc.model.dto.request.PontoGpsRequest;
import com.trisha.Loc.loc.model.dto.request.SessaoRequest;
import com.trisha.Loc.loc.model.dto.response.PontoGpsResponse;
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
}
