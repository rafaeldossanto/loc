package com.trisha.Loc.loc.controller;

import com.trisha.Loc.loc.auth.AuthenticatedUser;
import com.trisha.Loc.loc.model.dto.request.GpsPointRequest;
import com.trisha.Loc.loc.model.dto.request.SessionRequest;
import com.trisha.Loc.loc.model.dto.response.GpsPointResponse;
import com.trisha.Loc.loc.model.dto.response.SessionProgressResponse;
import com.trisha.Loc.loc.model.dto.response.LiveSessionResponse;
import com.trisha.Loc.loc.model.dto.response.SessionResponse;
import com.trisha.Loc.loc.model.dto.response.TrailPointsResponse;
import com.trisha.Loc.loc.model.enums.SessionVisibility;
import com.trisha.Loc.loc.service.LocationService;
import com.trisha.Loc.loc.websocket.LocationEventPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/localizacao")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;
    private final LocationEventPublisher eventPublisher;

    @PostMapping("/sessao")
    public SessionResponse startSession(AuthenticatedUser user, @RequestBody @Valid SessionRequest request) {
        return locationService.startSession(user.id(), request);
    }

    /**
     * Registra o ponto e o difunde no Redis para os assinantes do WebSocket —
     * mesmo padrao do fluxo MQTT: quem recebe o ponto publica o evento.
     */
    @PostMapping("/ponto")
    public GpsPointResponse registerPoint(AuthenticatedUser user, @RequestBody @Valid GpsPointRequest request) {
        GpsPointResponse point = locationService.registerPoint(user.id(), request);
        eventPublisher.publish(point);
        return point;
    }

    @PatchMapping("/sessao/{id}/finalizar")
    public SessionResponse finishSession(AuthenticatedUser user, @PathVariable String id) {
        return locationService.finishSession(user.id(), id);
    }

    @PatchMapping("/sessao/{id}/cancelar")
    public SessionResponse cancelSession(AuthenticatedUser user, @PathVariable String id) {
        return locationService.cancelSession(user.id(), id);
    }

    /** Troca quem acompanha ao vivo (PUBLICO/SEGUIDORES/AMIGOS/PRIVADO); so o dono. */
    @PatchMapping("/sessao/{id}/visibilidade")
    public SessionResponse updateVisibility(AuthenticatedUser user,
                                            @PathVariable String id,
                                            @RequestParam("visibilidade") SessionVisibility visibility) {
        return locationService.updateVisibility(user.id(), id, visibility);
    }

    @GetMapping("/sessao/caminho/{caminhoId}")
    public SessionResponse getSessionByPath(@PathVariable("caminhoId") String pathId) {
        return locationService.getSessionByPath(pathId);
    }

    @GetMapping("/sessao/{id}")
    public SessionResponse getSession(@PathVariable String id) {
        return locationService.getSession(id);
    }

    /** O usuario autenticado pode acompanhar esta sessao? (regra do SUBSCRIBE). */
    @GetMapping("/sessao/{id}/acesso")
    public boolean canWatchSession(AuthenticatedUser user,
                                   @PathVariable String id,
                                   @RequestHeader("Authorization") String authorization) {
        return locationService.canWatchSession(user.id(), id, authorization.replaceFirst("^Bearer ", ""));
    }

    @GetMapping("/sessao/{sessaoId}/progresso")
    public SessionProgressResponse getProgress(@PathVariable("sessaoId") String sessionId) {
        return locationService.getProgress(sessionId);
    }

    @GetMapping("/pontos/sessao/{sessaoId}")
    public List<GpsPointResponse> getPointsBySession(AuthenticatedUser user,
                                                     @PathVariable("sessaoId") String sessionId,
                                                     @RequestHeader("Authorization") String authorization) {
        return locationService.getPointsBySession(user.id(), sessionId, authorization.replaceFirst("^Bearer ", ""));
    }

    @GetMapping("/pontos/caminho/{caminhoId}")
    public List<GpsPointResponse> getPointsByPath(AuthenticatedUser user,
                                                  @PathVariable("caminhoId") String pathId,
                                                  @RequestHeader("Authorization") String authorization) {
        return locationService.getPointsByPath(user.id(), pathId, authorization.replaceFirst("^Bearer ", ""));
    }

    /**
     * Sessoes que o usuario pode acompanhar ao vivo. O Bearer cru e repassado
     * ao service porque a checagem social (amigos/seguidores) no APP e feita
     * em nome do proprio usuario.
     */
    @GetMapping("/sessoes/ao-vivo")
    public List<LiveSessionResponse> getLiveSessions(AuthenticatedUser user,
                                                     @RequestHeader("Authorization") String authorization) {
        return locationService.getLiveSessions(user.id(), authorization.replaceFirst("^Bearer ", ""));
    }

    @GetMapping("/pontos/bbox")
    public List<TrailPointsResponse> getPointsInBoundingBox(
            @RequestParam Double minLat,
            @RequestParam Double minLng,
            @RequestParam Double maxLat,
            @RequestParam Double maxLng,
            @RequestParam(name = "limitePorCaminho", defaultValue = "200") Integer maxPointsPerPath) {
        return locationService.getPointsInBoundingBox(minLat, minLng, maxLat, maxLng, maxPointsPerPath);
    }
}
