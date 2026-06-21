package com.trisha.Loc.loc.controller;

import com.trisha.Loc.loc.auth.AuthenticatedUser;
import com.trisha.Loc.loc.model.dto.request.GpsPointRequest;
import com.trisha.Loc.loc.model.dto.request.SessionRequest;
import com.trisha.Loc.loc.model.dto.response.GpsPointResponse;
import com.trisha.Loc.loc.model.dto.response.SessionProgressResponse;
import com.trisha.Loc.loc.model.dto.response.SessionResponse;
import com.trisha.Loc.loc.service.LocationService;
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
public class LocationController {

    private final LocationService locationService;

    @PostMapping("/sessao")
    public SessionResponse startSession(AuthenticatedUser user, @RequestBody @Valid SessionRequest request) {
        return locationService.startSession(user.id(), request);
    }

    @PostMapping("/ponto")
    public GpsPointResponse registerPoint(@RequestBody @Valid GpsPointRequest request) {
        return locationService.registerPoint(request);
    }

    @PatchMapping("/sessao/{id}/finalizar")
    public SessionResponse finishSession(@PathVariable String id) {
        return locationService.finishSession(id);
    }

    @PatchMapping("/sessao/{id}/cancelar")
    public SessionResponse cancelSession(@PathVariable String id) {
        return locationService.cancelSession(id);
    }

    @GetMapping("/sessao/caminho/{caminhoId}")
    public SessionResponse getSessionByPath(@PathVariable("caminhoId") String pathId) {
        return locationService.getSessionByPath(pathId);
    }

    @GetMapping("/sessao/{sessaoId}/progresso")
    public SessionProgressResponse getProgress(@PathVariable("sessaoId") String sessionId) {
        return locationService.getProgress(sessionId);
    }

    @GetMapping("/pontos/sessao/{sessaoId}")
    public List<GpsPointResponse> getPointsBySession(@PathVariable("sessaoId") String sessionId) {
        return locationService.getPointsBySession(sessionId);
    }

    @GetMapping("/pontos/caminho/{caminhoId}")
    public List<GpsPointResponse> getPointsByPath(@PathVariable("caminhoId") String pathId) {
        return locationService.getPointsByPath(pathId);
    }
}
