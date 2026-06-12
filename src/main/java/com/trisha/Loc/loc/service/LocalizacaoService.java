package com.trisha.Loc.loc.service;

import com.trisha.Loc.loc.entity.PontoGps;
import com.trisha.Loc.loc.entity.SessaoRastreamento;
import com.trisha.Loc.loc.mapper.PontoGpsMapper;
import com.trisha.Loc.loc.mapper.SessaoMapper;
import com.trisha.Loc.loc.model.dto.request.PontoGpsRequest;
import com.trisha.Loc.loc.model.dto.request.SessaoRequest;
import com.trisha.Loc.loc.model.dto.response.PontoGpsResponse;
import com.trisha.Loc.loc.model.dto.response.ProgressoSessaoResponse;
import com.trisha.Loc.loc.model.dto.response.SessaoResponse;
import com.trisha.Loc.loc.model.enums.StatusSessao;
import com.trisha.Loc.loc.repository.PontoGpsRepository;
import com.trisha.Loc.loc.repository.SessaoRastreamentoRepository;
import com.trisha.Loc.loc.util.GeoUtils;
import com.trisha.Loc.loc.util.TrajetoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalizacaoService {

    private final SessaoRastreamentoRepository sessaoRepository;
    private final PontoGpsRepository pontoGpsRepository;

    /** Desvio maximo (m) para um ponto ser considerado redundante num trecho reto. */
    private static final double TOLERANCIA_SIMPLIFICACAO_METROS = 8.0;

    public SessaoResponse iniciarSessao(String usuarioId, SessaoRequest request) {
        log.info("Iniciando sessao de rastreamento para caminho: {}", request.caminhoId());

        sessaoRepository.findByUsuarioIdAndStatus(usuarioId, StatusSessao.EM_ANDAMENTO)
                .ifPresent(s -> {
                    throw new IllegalArgumentException("Usuario ja possui uma sessao em andamento");
                });

        sessaoRepository.findByCaminhoId(request.caminhoId())
                .ifPresent(s -> {
                    throw new IllegalArgumentException("Ja existe uma sessao para esse caminho");
                });

        var sessao = sessaoRepository.save(SessaoMapper.toEntity(request, usuarioId));
        log.info("Sessao {} iniciada", sessao.getId());
        return SessaoMapper.toResponse(sessao);
    }

    public PontoGpsResponse registrarPonto(PontoGpsRequest request) {
        SessaoRastreamento sessao = findSessaoAtiva(request.sessaoId());

        int ordem = pontoGpsRepository.countBySessaoId(sessao.getId()) + 1;
        PontoGps ponto = pontoGpsRepository.save(PontoGpsMapper.toEntity(request, sessao, ordem));

        log.debug("Ponto GPS #{} registrado na sessao {}", ordem, sessao.getId());

        return montarRespostaComProximidade(sessao, ponto, ordem);
    }

    private PontoGpsResponse montarRespostaComProximidade(SessaoRastreamento sessao, PontoGps ponto, int ordem) {
        boolean recursoDesligado = !Boolean.TRUE.equals(sessao.getTerminoAutomatico());
        boolean ehPontoInicial = ordem <= 1;

        if (recursoDesligado || ehPontoInicial) {
            return PontoGpsMapper.toResponse(ponto);
        }

        return pontoGpsRepository.findFirstBySessaoIdOrderByOrdemAsc(sessao.getId())
                .map(inicial -> {
                    double distancia = GeoUtils.distanciaMetros(
                            inicial.getLatitude(), inicial.getLongitude(),
                            ponto.getLatitude(), ponto.getLongitude());
                    boolean proximo = distancia <= sessao.getDistanciaTerminoMetros();

                    if (proximo) {
                        log.info("Sessao {} a {}m do inicio (limite {}m) — sugerindo termino",
                                sessao.getId(), (int) distancia, sessao.getDistanciaTerminoMetros());
                    }
                    return PontoGpsMapper.toResponse(ponto, proximo, distancia);
                })
                .orElseGet(() -> PontoGpsMapper.toResponse(ponto));
    }

    public SessaoResponse finalizarSessao(String sessaoId) {
        log.info("Finalizando sessao: {}", sessaoId);
        SessaoRastreamento sessao = findSessaoAtiva(sessaoId);

        List<PontoGps> pontos = pontoGpsRepository.findBySessaoIdOrderByOrdemAsc(sessaoId);

        double distanciaTotal = calcularDistanciaTotal(pontos);
        simplificarTrajeto(pontos);

        sessao.setStatus(StatusSessao.FINALIZADA);
        sessao.setDistanciaTotalKm(distanciaTotal / 1000.0);
        sessao.setFinalizadaEm(LocalDateTime.now());

        log.info("Sessao {} finalizada — distancia: {}km", sessaoId, sessao.getDistanciaTotalKm());
        return SessaoMapper.toResponse(sessaoRepository.save(sessao));
    }

    /**
     * Remove do banco os pontos redundantes de trechos retilineos (Douglas-Peucker),
     * preservando a forma da trilha. Roda na finalizacao, com o trajeto ja completo.
     */
    private void simplificarTrajeto(List<PontoGps> pontos) {
        if (pontos.size() < 3) {
            return;
        }

        List<PontoGps> mantidos = TrajetoUtils.simplificar(pontos, TOLERANCIA_SIMPLIFICACAO_METROS);
        Set<String> idsMantidos = mantidos.stream().map(PontoGps::getId).collect(Collectors.toSet());

        List<PontoGps> removidos = pontos.stream()
                .filter(ponto -> !idsMantidos.contains(ponto.getId()))
                .toList();

        if (!removidos.isEmpty()) {
            pontoGpsRepository.deleteAll(removidos);
            log.info("Trajeto da sessao simplificado: {} de {} pontos removidos ({} mantidos)",
                    removidos.size(), pontos.size(), mantidos.size());
        }
    }

    public SessaoResponse cancelarSessao(String sessaoId) {
        log.info("Cancelando sessao: {}", sessaoId);
        SessaoRastreamento sessao = findSessaoAtiva(sessaoId);

        sessao.setStatus(StatusSessao.CANCELADA);
        sessao.setFinalizadaEm(LocalDateTime.now());

        return SessaoMapper.toResponse(sessaoRepository.save(sessao));
    }

    public SessaoResponse getSessaoByCaminho(String caminhoId) {
        return SessaoMapper.toResponse(
                sessaoRepository.findByCaminhoId(caminhoId)
                        .orElseThrow(() -> new IllegalArgumentException("Sessao nao encontrada para esse caminho"))
        );
    }

    public List<PontoGpsResponse> getPontosBySessao(String sessaoId) {
        return pontoGpsRepository.findBySessaoIdOrderByOrdemAsc(sessaoId)
                .stream().map(PontoGpsMapper::toResponse).toList();
    }

    public List<PontoGpsResponse> getPontosByCaminho(String caminhoId) {
        SessaoRastreamento sessao = sessaoRepository.findByCaminhoId(caminhoId)
                .orElseThrow(() -> new IllegalArgumentException("Sessao nao encontrada para esse caminho"));

        return getPontosBySessao(sessao.getId());
    }

    /**
     * Progresso em tempo real da sessao: distancia ja percorrida (recalculada a
     * partir dos pontos GPS) e tempo decorrido desde o inicio. Para o app exibir
     * durante a trilha em andamento. Em sessao finalizada, usa a distancia ja
     * gravada; em andamento, recalcula com os pontos atuais.
     */
    public ProgressoSessaoResponse getProgresso(String sessaoId) {
        SessaoRastreamento sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new IllegalArgumentException("Sessao nao encontrada"));

        List<PontoGps> pontos = pontoGpsRepository.findBySessaoIdOrderByOrdemAsc(sessaoId);

        double distanciaKm = nonNull(sessao.getDistanciaTotalKm())
                ? sessao.getDistanciaTotalKm()
                : calcularDistanciaTotal(pontos) / 1000.0;

        LocalDateTime fim = nonNull(sessao.getFinalizadaEm()) ? sessao.getFinalizadaEm() : LocalDateTime.now();
        long tempoSegundos = Duration.between(sessao.getIniciadaEm(), fim).getSeconds();

        return SessaoMapper.toProgresso(sessao, distanciaKm, tempoSegundos, pontos.size());
    }

    private double calcularDistanciaTotal(List<PontoGps> pontos) {
        if (pontos.size() < 2) return 0.0;

        double total = 0.0;
        for (int i = 1; i < pontos.size(); i++) {
            total += GeoUtils.distanciaMetros(
                    pontos.get(i - 1).getLatitude(), pontos.get(i - 1).getLongitude(),
                    pontos.get(i).getLatitude(), pontos.get(i).getLongitude()
            );
        }
        return total;
    }

    private SessaoRastreamento findSessaoAtiva(String sessaoId) {
        SessaoRastreamento sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new IllegalArgumentException("Sessao nao encontrada"));

        if (!StatusSessao.EM_ANDAMENTO.equals(sessao.getStatus())) {
            throw new IllegalArgumentException("Sessao nao esta em andamento");
        }
        return sessao;
    }
}
