package com.trisha.Loc.loc.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Consultas sociais no servico APP para autorizar o acompanhamento ao vivo:
 * amizade (visibilidade AMIGOS) e seguimento (visibilidade SEGUIDORES).
 * Propaga o Bearer do assinante (capturado no CONNECT do WebSocket).
 */
@Component
@RequiredArgsConstructor
public class AppFriendshipClient {

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
}
