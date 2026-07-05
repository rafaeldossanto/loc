package com.trisha.Loc.loc.websocket;

import com.trisha.Loc.loc.client.AppFriendshipClient;
import com.trisha.Loc.loc.entity.TrackingSession;
import com.trisha.Loc.loc.model.enums.SessionVisibility;
import com.trisha.Loc.loc.repository.TrackingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;

import static java.util.Objects.isNull;

/**
 * Autoriza o SUBSCRIBE a /topic/sessao/{id} conforme a visibilidade:
 *  - PUBLICO: qualquer um acompanha;
 *  - PRIVADO: ninguem (nem o dono via topico) — so o proprio app local;
 *  - AMIGOS: o proprio dono ou quem for amigo dele (consulta ao servico APP,
 *    propagando o Bearer capturado no CONNECT);
 *  - SEGUIDORES: o proprio dono ou quem o segue (mesma consulta ao APP).
 */
@Component
@RequiredArgsConstructor
public class SubscribeAuthorizationInterceptor implements ChannelInterceptor {

    private static final String SESSION_TOPIC_PREFIX = "/topic/sessao/";

    private final TrackingSessionRepository sessionRepository;
    private final AppFriendshipClient appFriendshipClient;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (!StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return message;
        }

        String destination = accessor.getDestination();
        if (isNull(destination) || !destination.startsWith(SESSION_TOPIC_PREFIX)) {
            return message;
        }

        String sessionId = destination.substring(SESSION_TOPIC_PREFIX.length());
        TrackingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sessao nao encontrada: " + sessionId));

        if (SessionVisibility.PRIVADO.equals(session.getVisibility())) {
            throw new IllegalStateException("Sessao privada — acompanhamento ao vivo nao permitido");
        }
        if (SessionVisibility.AMIGOS.equals(session.getVisibility())) {
            authorizeFriends(accessor, session);
        }
        if (SessionVisibility.SEGUIDORES.equals(session.getVisibility())) {
            authorizeFollowers(accessor, session);
        }
        return message;
    }

    private void authorizeFriends(StompHeaderAccessor accessor, TrackingSession session) {
        String subscriber = requireSubscriber(accessor);
        if (subscriber.equals(session.getUserId())) {
            return;
        }

        if (!appFriendshipClient.areFriends(subscriber, session.getUserId(), token(accessor))) {
            throw new IllegalStateException("Apenas amigos podem acompanhar esta sessao");
        }
    }

    private void authorizeFollowers(StompHeaderAccessor accessor, TrackingSession session) {
        String subscriber = requireSubscriber(accessor);
        if (subscriber.equals(session.getUserId())) {
            return;
        }

        if (!appFriendshipClient.isFollower(subscriber, session.getUserId(), token(accessor))) {
            throw new IllegalStateException("Apenas seguidores podem acompanhar esta sessao");
        }
    }

    private String requireSubscriber(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (isNull(user)) {
            throw new IllegalStateException("Assinante nao autenticado");
        }
        return user.getName();
    }

    private String token(StompHeaderAccessor accessor) {
        Map<String, Object> attributes = accessor.getSessionAttributes();
        return isNull(attributes) ? null : (String) attributes.get(ConnectAuthenticationInterceptor.TOKEN_ATTRIBUTE);
    }
}
