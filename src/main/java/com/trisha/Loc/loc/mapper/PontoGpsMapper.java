package com.trisha.Loc.loc.mapper;

import com.trisha.Loc.loc.entity.PontoGps;
import com.trisha.Loc.loc.entity.SessaoRastreamento;
import com.trisha.Loc.loc.model.dto.request.PontoGpsRequest;
import com.trisha.Loc.loc.model.dto.response.PontoGpsResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public class PontoGpsMapper {

    private PontoGpsMapper() {}

    public static PontoGps toEntity(PontoGpsRequest request, SessaoRastreamento sessao, int ordem) {
        return PontoGps.builder()
                .id(UUID.randomUUID().toString())
                .sessao(sessao)
                .latitude(request.latitude())
                .longitude(request.longitude())
                .altitude(request.altitude())
                .precisao(request.precisao())
                .velocidade(request.velocidade())
                .ordem(ordem)
                .registradoEm(LocalDateTime.now())
                .build();
    }

    /**
     * Resposta sem informacao de proximidade — usada em listagens de pontos,
     * onde o aviso de termino nao se aplica.
     */
    public static PontoGpsResponse toResponse(PontoGps entity) {
        return toResponse(entity, null, null);
    }

    /**
     * Resposta enriquecida com o estado de proximidade ao ponto inicial,
     * usada na resposta de registrarPonto quando o termino automatico esta ligado.
     */
    public static PontoGpsResponse toResponse(PontoGps entity, Boolean proximoDoInicio, Double distanciaDoInicioMetros) {
        return PontoGpsResponse.builder()
                .id(entity.getId())
                .sessaoId(entity.getSessao().getId())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .altitude(entity.getAltitude())
                .precisao(entity.getPrecisao())
                .velocidade(entity.getVelocidade())
                .ordem(entity.getOrdem())
                .registradoEm(entity.getRegistradoEm())
                .proximoDoInicio(proximoDoInicio)
                .distanciaDoInicioMetros(distanciaDoInicioMetros)
                .build();
    }
}
