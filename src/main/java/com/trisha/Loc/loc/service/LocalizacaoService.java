package com.trisha.Loc.loc.service;

import com.trisha.Loc.loc.entity.PontoGps;
import com.trisha.Loc.loc.entity.SessaoRastreamento;
import com.trisha.Loc.loc.mapper.PontoGpsMapper;
import com.trisha.Loc.loc.mapper.SessaoMapper;
import com.trisha.Loc.loc.model.dto.request.PontoGpsRequest;
import com.trisha.Loc.loc.model.dto.request.SessaoRequest;
import com.trisha.Loc.loc.model.dto.response.PontoGpsResponse;
import com.trisha.Loc.loc.model.dto.response.SessaoResponse;
import com.trisha.Loc.loc.model.enums.StatusSessao;
import com.trisha.Loc.loc.repository.PontoGpsRepository;
import com.trisha.Loc.loc.repository.SessaoRastreamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalizacaoService {

    private final SessaoRastreamentoRepository sessaoRepository;
    private final PontoGpsRepository pontoGpsRepository;

    private static final int RAIO_TERRA_METROS = 6371000;

    public SessaoResponse iniciarSessao(SessaoRequest request) {
        log.info("Iniciando sessao de rastreamento para caminho: {}", request.caminhoId());

        sessaoRepository.findByUsuarioIdAndStatus(request.usuarioId(), StatusSessao.EM_ANDAMENTO)
                .ifPresent(s -> {
                    throw new IllegalArgumentException("Usuario ja possui uma sessao em andamento");
                });

        sessaoRepository.findByCaminhoId(request.caminhoId())
                .ifPresent(s -> {
                    throw new IllegalArgumentException("Ja existe uma sessao para esse caminho");
                });

        var sessao = sessaoRepository.save(SessaoMapper.toEntity(request));
        log.info("Sessao {} iniciada", sessao.getId());
        return SessaoMapper.toResponse(sessao);
    }

    public PontoGpsResponse registrarPonto(PontoGpsRequest request) {
        SessaoRastreamento sessao = findSessaoAtiva(request.sessaoId());

        int ordem = pontoGpsRepository.countBySessaoId(sessao.getId()) + 1;
        var ponto = pontoGpsRepository.save(PontoGpsMapper.toEntity(request, sessao, ordem));

        log.debug("Ponto GPS #{} registrado na sessao {}", ordem, sessao.getId());
        return PontoGpsMapper.toResponse(ponto);
    }

    public SessaoResponse finalizarSessao(String sessaoId) {
        log.info("Finalizando sessao: {}", sessaoId);
        SessaoRastreamento sessao = findSessaoAtiva(sessaoId);

        List<PontoGps> pontos = pontoGpsRepository.findBySessaoIdOrderByOrdemAsc(sessaoId);

        double distanciaTotal = calcularDistanciaTotal(pontos);

        sessao.setStatus(StatusSessao.FINALIZADA);
        sessao.setDistanciaTotalKm(distanciaTotal / 1000.0);
        sessao.setFinalizadaEm(LocalDateTime.now());

        log.info("Sessao {} finalizada — distancia: {}km", sessaoId, sessao.getDistanciaTotalKm());
        return SessaoMapper.toResponse(sessaoRepository.save(sessao));
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

    private double calcularDistanciaTotal(List<PontoGps> pontos) {
        if (pontos.size() < 2) return 0.0;

        double total = 0.0;
        for (int i = 1; i < pontos.size(); i++) {
            total += calcularDistanciaMetros(
                    pontos.get(i - 1).getLatitude(), pontos.get(i - 1).getLongitude(),
                    pontos.get(i).getLatitude(), pontos.get(i).getLongitude()
            );
        }
        return total;
    }

    private double calcularDistanciaMetros(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return RAIO_TERRA_METROS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private SessaoRastreamento findSessaoAtiva(String sessaoId) {
        SessaoRastreamento sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new IllegalArgumentException("Sessao nao encontrada"));

        if (sessao.getStatus() != StatusSessao.EM_ANDAMENTO) {
            throw new IllegalArgumentException("Sessao nao esta em andamento");
        }
        return sessao;
    }
}
