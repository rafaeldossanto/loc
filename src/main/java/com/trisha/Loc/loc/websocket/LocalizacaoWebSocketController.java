package com.trisha.Loc.loc.websocket;

import com.trisha.Loc.loc.model.dto.request.PontoGpsRequest;
import com.trisha.Loc.loc.model.dto.response.PontoGpsResponse;
import com.trisha.Loc.loc.service.LocalizacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/**
 * Porta WebSocket do tempo real. Reusa a MESMA regra do REST
 * ({@link LocalizacaoService#registrarPonto}) e publica o resultado no Redis
 * para difusao entre instancias. O app publica em /app/sessao/ponto (o
 * PontoGpsRequest ja carrega o sessaoId).
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class LocalizacaoWebSocketController {

    private final LocalizacaoService localizacaoService;
    private final LocalizacaoEventPublisher eventPublisher;

    @MessageMapping("/sessao/ponto")
    public void registrarPonto(PontoGpsRequest request) {
        PontoGpsResponse ponto = localizacaoService.registrarPonto(request);
        eventPublisher.publicar(ponto);
    }
}
