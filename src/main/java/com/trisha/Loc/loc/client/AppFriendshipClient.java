package com.trisha.Loc.loc.client;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Consultas sociais no servico APP para autorizar o acompanhamento ao vivo:
 * amizade (visibilidade AMIGOS) e seguimento (visibilidade SEGUIDORES).
 * Propaga o Bearer do assinante (capturado no CONNECT do WebSocket).
 */
@Component
@RequiredArgsConstructor
public class AppFriendshipClient {

    private static final ParameterizedTypeReference<List<String>> LIST_IDS = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_PATHS = new ParameterizedTypeReference<>() {};

    private final RestClient appRestClient;

    public boolean areFriends(String userA, String userB, String bearerToken) {
        Boolean result = appRestClient.get()
                .uri(b -> b.path("/amizade/sao-amigos")
                        .queryParam("a", userA)
                        .queryParam("b", userB)
                        .build())
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve()
                .body(Boolean.class);
        return Boolean.TRUE.equals(result);
    }

    /** O assinante segue o dono da sessao? (seguir e direcional, sem aceite). */
    public boolean isFollower(String followerId, String followedId, String bearerToken) {
        Boolean result = appRestClient.get()
                .uri(b -> b.path("/seguidor/segue")
                        .queryParam("seguidorId", followerId)
                        .queryParam("seguidoId", followedId)
                        .build())
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve()
                .body(Boolean.class);
        return Boolean.TRUE.equals(result);
    }

    /** O portador do token enxerga a aventura deste caminho? (visibilidade no APP). */
    public boolean canViewPath(String pathId, String bearerToken) {
        Boolean result = appRestClient.get()
                .uri("/caminho/{id}/acesso", pathId)
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve()
                .body(Boolean.class);
        return Boolean.TRUE.equals(result);
    }

    /** Ids dos amigos do portador do token — checagem em lote (lista ao vivo). */
    public List<String> friendIds(String bearerToken) {
        List<String> ids = appRestClient.get()
                .uri("/amizade/amigos-ids")
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve()
                .body(LIST_IDS);
        return ids == null ? List.of() : ids;
    }

    /** Ids de quem o portador do token segue — checagem em lote (lista ao vivo). */
    public List<String> followingIds(String bearerToken) {
        List<String> ids = appRestClient.get()
                .uri("/seguidor/seguindo-ids")
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve()
                .body(LIST_IDS);
        return ids == null ? List.of() : ids;
    }

    /**
     * Dentre os caminhos informados, os ids que o portador do token pode ver no
     * mapa (visibilidade da aventura, no APP). Espelha o {@code /caminho/descobrir}
     * usado pelo BFF — o loc filtra a consulta por bbox por conta propria para que
     * uma chamada DIRETA ao loc nao vaze a geometria de trilhas privadas.
     */
    public Set<String> visiblePathIds(List<String> pathIds, String bearerToken) {
        if (pathIds.isEmpty()) {
            return Set.of();
        }
        List<Map<String, Object>> visible = appRestClient.get()
                .uri(b -> b.path("/caminho/descobrir")
                        .queryParam("ids", String.join(",", pathIds))
                        .build())
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve()
                .body(LIST_PATHS);
        if (visible == null) {
            return Set.of();
        }
        return visible.stream()
                .map(path -> (String) path.get("id"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
