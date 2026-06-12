package com.trisha.Loc.loc.websocket;

import com.trisha.Loc.loc.client.AppAmizadeClient;
import com.trisha.Loc.loc.entity.SessaoRastreamento;
import com.trisha.Loc.loc.model.enums.VisibilidadeSessao;
import com.trisha.Loc.loc.repository.SessaoRastreamentoRepository;
import com.trisha.Loc.loc.stub.SessaoStub;
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
@DisplayName("SubscribeAutorizacaoInterceptor")
class SubscribeAutorizacaoInterceptorTest {

    @Mock
    private SessaoRastreamentoRepository sessaoRepository;
    @Mock
    private AppAmizadeClient appAmizadeClient;
    @Mock
    private MessageChannel channel;

    @InjectMocks
    private SubscribeAutorizacaoInterceptor interceptor;

    private Message<byte[]> subscribe(String destino, String assinante, String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destino);
        if (assinante != null) {
            accessor.setUser(() -> assinante);
        }
        Map<String, Object> attrs = new HashMap<>();
        if (token != null) {
            attrs.put(ConnectAutenticacaoInterceptor.ATRIBUTO_TOKEN, token);
        }
        accessor.setSessionAttributes(attrs);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private String topico() {
        return "/topic/sessao/" + SessaoStub.SESSAO_ID;
    }

    @Test
    @DisplayName("sessao PUBLICO libera o subscribe")
    void devePermitirSessaoPublica() {
        SessaoRastreamento sessao = SessaoStub.umaSessao().visibilidade(VisibilidadeSessao.PUBLICO).build();
        when(sessaoRepository.findById(SessaoStub.SESSAO_ID)).thenReturn(Optional.of(sessao));

        Message<byte[]> msg = subscribe(topico(), null, null);

        assertThat(interceptor.preSend(msg, channel)).isSameAs(msg);
    }

    @Test
    @DisplayName("sessao PRIVADO bloqueia o subscribe")
    void deveBloquearSessaoPrivada() {
        SessaoRastreamento sessao = SessaoStub.umaSessao().visibilidade(VisibilidadeSessao.PRIVADO).build();
        when(sessaoRepository.findById(SessaoStub.SESSAO_ID)).thenReturn(Optional.of(sessao));

        Message<byte[]> msg = subscribe(topico(), null, null);

        assertThatThrownBy(() -> interceptor.preSend(msg, channel))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("privada");
    }

    @Test
    @DisplayName("AMIGOS libera quando o assinante e amigo do dono")
    void devePermitirAmigo() {
        SessaoRastreamento sessao = SessaoStub.umaSessao().visibilidade(VisibilidadeSessao.AMIGOS).build();
        when(sessaoRepository.findById(SessaoStub.SESSAO_ID)).thenReturn(Optional.of(sessao));
        when(appAmizadeClient.saoAmigos("amigo", SessaoStub.USUARIO_ID, "tok")).thenReturn(true);

        Message<byte[]> msg = subscribe(topico(), "amigo", "tok");

        assertThat(interceptor.preSend(msg, channel)).isSameAs(msg);
    }

    @Test
    @DisplayName("AMIGOS libera o proprio dono sem consultar amizade")
    void devePermitirProprioDono() {
        SessaoRastreamento sessao = SessaoStub.umaSessao().visibilidade(VisibilidadeSessao.AMIGOS).build();
        when(sessaoRepository.findById(SessaoStub.SESSAO_ID)).thenReturn(Optional.of(sessao));

        Message<byte[]> msg = subscribe(topico(), SessaoStub.USUARIO_ID, "tok");

        assertThat(interceptor.preSend(msg, channel)).isSameAs(msg);
    }

    @Test
    @DisplayName("AMIGOS bloqueia quem nao e amigo")
    void deveBloquearNaoAmigo() {
        SessaoRastreamento sessao = SessaoStub.umaSessao().visibilidade(VisibilidadeSessao.AMIGOS).build();
        when(sessaoRepository.findById(SessaoStub.SESSAO_ID)).thenReturn(Optional.of(sessao));
        when(appAmizadeClient.saoAmigos("estranho", SessaoStub.USUARIO_ID, "tok")).thenReturn(false);

        Message<byte[]> msg = subscribe(topico(), "estranho", "tok");

        assertThatThrownBy(() -> interceptor.preSend(msg, channel))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("amigos");
    }

    @Test
    @DisplayName("destino fora de /topic/sessao passa sem checar")
    void deveIgnorarOutrosDestinos() {
        Message<byte[]> msg = subscribe("/topic/outra-coisa", null, null);

        assertThat(interceptor.preSend(msg, channel)).isSameAs(msg);
    }
}
