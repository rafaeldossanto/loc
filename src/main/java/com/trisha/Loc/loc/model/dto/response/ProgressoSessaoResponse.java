package com.trisha.Loc.loc.model.dto.response;

import com.trisha.Loc.loc.model.enums.StatusSessao;
import lombok.Builder;

/**
 * Progresso em tempo real de uma sessao de rastreamento — para o app exibir
 * "distancia percorrida" e "tempo decorrido" enquanto a trilha esta em andamento.
 */
@Builder
public record ProgressoSessaoResponse(
        String sessaoId,
        StatusSessao status,
        Double distanciaPercorridaKm,
        Long tempoDecorridoSegundos,
        Integer totalPontos
) {}
