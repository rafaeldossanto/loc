package com.trisha.Loc.loc.stub;

import com.trisha.Loc.loc.entity.PontoGps;
import com.trisha.Loc.loc.entity.SessaoRastreamento;
import com.trisha.Loc.loc.model.dto.request.PontoGpsRequest;
import com.trisha.Loc.loc.model.dto.request.SessaoRequest;
import com.trisha.Loc.loc.model.enums.StatusSessao;

import java.time.LocalDateTime;

/**
 * Facilitador de testes para SessaoRastreamento e PontoGps.
 * Builders ja preenchidos — usar direto com {@code .build()}
 * ou sobrescrever campos pontuais quando o teste precisar.
 */
public final class SessaoStub {

    public static final String SESSAO_ID = "sessao-1";
    public static final String CAMINHO_ID = "caminho-1";
    public static final String USUARIO_ID = "usuario-1";

    public static final double LATITUDE = -20.4350;
    public static final double LONGITUDE = -41.7920;

    private SessaoStub() {
    }

    public static SessaoRastreamento.SessaoRastreamentoBuilder umaSessao() {
        return SessaoRastreamento.builder()
                .id(SESSAO_ID)
                .caminhoId(CAMINHO_ID)
                .usuarioId(USUARIO_ID)
                .status(StatusSessao.EM_ANDAMENTO)
                .iniciadaEm(LocalDateTime.now());
    }

    /** Request padrao: termino automatico desligado (defaults aplicados no mapper). */
    public static SessaoRequest umRequest() {
        return new SessaoRequest(CAMINHO_ID, USUARIO_ID, null, null);
    }

    /** Request com termino automatico ligado e raio customizado. */
    public static SessaoRequest umRequestComTermino(double distanciaMetros) {
        return new SessaoRequest(CAMINHO_ID, USUARIO_ID, true, distanciaMetros);
    }

    public static PontoGps.PontoGpsBuilder umPonto(int ordem, double latitude, double longitude) {
        return PontoGps.builder()
                .id("ponto-" + ordem)
                .sessao(umaSessao().build())
                .latitude(latitude)
                .longitude(longitud