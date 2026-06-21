package com.trisha.Loc.loc.websocket;

import com.trisha.Loc.loc.client.AppFriendshipClient;
import com.trisha.Loc.loc.entity.TrackingSession;
import com.trisha.Loc.loc.model.enums.SessionVisibility;
import com.trisha.Loc.loc.repository.TrackingSessionRepository;
import com.trisha.Loc.loc.stub.SessionStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscribeAuthorizationInterceptor")
class SubscribeAuthorizationInterceptorTest {

    @Mock
    private TrackingSessionRepository sessionRepository;
    @Mock
    private AppFriendshipClient appFriendshipClient;
    @Mock
    private MessageChannel channel;

    @InjectMocks
    private SubscribeAuthorizationInterceptor interceptor;

    private Message<byte[]> subscribe(String destination, String subscriber, String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        if (subscriber != null) {
            accessor.setUser(() -> subscriber);
        }
        Map<String, Object> attrs = new HashMap<>();
        if (token != null) {
            attrs.put(ConnectAuthenticationInterceptor.TOKEN_ATTRIBUTE, token);
        }
        accessor.setSessionAttributes(attrs);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private String topic() {
        return "/topic/sessao/" + SessionStub.SESSION_ID;
    }

    @Test
    @DisplayName("sessao PUBLICO libera o subscribe")
    void shouldAllowPublicSession() {
        TrackingSession session = SessionStub.aSession().visibility(SessionVisibility.PUBLICO).build();
        when(sessionRepository.findById(SessionStub.SESSION_ID)).thenReturn(Optional.of(session));

        Message<byte[]> msg = subscribe(topic(), null, null);

        assertThat(interceptor.preSend(msg, channel)).isSameAs(msg);
    }

    @Test
    @DisplayName("sessao PRIVADO bloqueia o subscribe")
    void shouldBlockPrivateSession() {
        TrackingSession session = SessionStub.aSession().visibility(SessionVisibility.PRIVADO).build();
        when(sessionRepository.findById(SessionStub.SESSION_ID)).thenReturn(Optional.of(session));

        Message<byte[]> msg = subscribe(topic(), null, null);

        assertThatThrownBy(() -> interceptor.preSend(msg, channel))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("privada");
    }

    @Test
    @DisplayName("AMIGOS libera quando o assinante e amigo do dono")
    void shouldAllowFriend() {
        TrackingSession session = SessionStub.aSession().visibility(SessionVisibility.AMIGOS).build();
        when(sessionRepository.findById(SessionStub.SESSION_ID)).thenReturn(Optional.of(session));
        when(appFriendshipClient.areFriends("amigo", SessionStub.USER_ID, "tok")).thenReturn(true);

        Message<byte[]> msg = subscribe(topic(), "amigo", "tok");

        assertThat(interceptor.preSend(msg, channel)).isSameAs(msg);
    }

    @Test
    @DisplayName("AMIGOS libera o proprio dono sem consultar amizade")
    void shouldAllowOwner() {
        TrackingSession session = SessionStub.aSession().visibility(SessionVisibility.AMIGOS).build();
        when(sessionRepository.findById(SessionStub.SESSION_ID)).thenReturn(Optional.of(session));

        Message<byte[]> msg = subscribe(topic(), SessionStub.USER_ID, "tok");

        assertThat(interceptor.preSend(msg, channel)).isSameAs(msg);
    }

    @Test
    @DisplayName("AMIGOS bloqueia quem nao e amigo")
    void shouldBlockNonFriend() {
        TrackingSession session = SessionStub.aSession().visibility(SessionVisibility.AMIGOS).build();
        when(sessionRepository.findById(SessionStub.SESSION_ID)).thenReturn(Optional.of(session));
        when(appFriendshipClient.areFriends("estranho", SessionStub.USER_ID, "tok")).thenReturn(false);

        Message<byte[]> msg = subscribe(topic(), "estranho", "tok");

        assertThatThrownBy(() -> interceptor.preSend(msg, channel))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("amigos");
    }

    @Test
    @DisplayName("destino fora de /topic/sessao passa sem checar")
    void shouldIgnoreOtherDestinations() {
        Message<byte[]> msg = subscribe("/topic/outra-coisa", null, null);

        assertThat(interceptor.preSend(msg, channel)).isSameAs(msg);
    }
}
