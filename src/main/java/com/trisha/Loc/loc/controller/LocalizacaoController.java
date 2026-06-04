package com.trisha.Loc.loc.controller;

import com.trisha.Loc.loc.model.dto.request.PontoGpsRequest;
import com.trisha.Loc.loc.model.dto.request.SessaoRequest;
import com.trisha.Loc.loc.model.dto.response.PontoGpsResponse;
import com.trisha.Loc.loc.model.dto.response.SessaoResponse;
import com.trisha.Loc.loc.service.LocalizacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/localizacao")
@RequiredArgsConstructor
public class LocalizacaoController {

    private final LocalizacaoService localizacaoService;

    @PostMapping("/sessao")
    public SessaoResponse iniciarSessao(@RequestBody @Valid SessaoRequest request) {
        return localizacaoService.iniciarSessao(request);
    }

    @PostMapping("/ponto")
    public PontoGpsResponse registrarPonto(@RequestBody @Valid PontoGpsRequest request) {
        return localizacaoService.registrarPonto(request);
    }

    @PatchMapping("/sessao/{id}/finalizar")
    public SessaoResponse finalizarSessao(@PathVariable String id) {
        return localizacaoService.finalizarSessao(id);
    }

    @PatchMapping("/sessao/{id}/cancelar")
    public SessaoResponse cancelarSessao(@PathVariable String id) {
        return localizacaoService.cancelarSessao(id);
    }

    @GetMapping("/sessao/caminho/{caminhoId}")
    public SessaoResponse getSessaoByCaminho(@PathVariable String caminhoId) {
        return localizacaoService.getSessaoByCaminho(caminhoId);
    }

    @GetMapping("/pontos/sessao/{sessaoId}")
    public List<PontoGpsResponse> getPontosBySessao(@PathVariable String sessaoId) {
        return localizacaoService.getPontosBySessao(sessaoId);
    }

    @GetMapping("/pontos/caminho/{caminhoId}")
    public List<PontoGpsResponse> getPontosByCaminho(@PathVariable String caminhoId) {
        return localizacaoService.getPontosByCaminho(caminhoId);
    }
}
