package com.trisha.Loc.loc.websocket;

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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscribeAutorizacaoInterceptor")
class SubscribeAutorizacaoInterceptorTest {

    @Mock
    private SessaoRastreamentoRepository sessaoRepository;
    @Mock
    private MessageChannel channel;

    @InjectMocks
    private SubscribeAutorizacaoInterceptor interceptor;

    private Message<byte[]> subscribe(String destino) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destino);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    @DisplayName("sessao PUBLICO libera o subscribe")
    void devePermitirSessaoPublica() {
        SessaoRastreamento sessao = SessaoStub.umaSessao().visibilidade(VisibilidadeSessao.PUBLICO).build();
        when(sessaoRepository.findById(SessaoStub.SESSAO_ID)).thenReturn(Optional.of(sessao));

        Message<byte[]> msg = subscribe("/topic/sessao/" + SessaoStub.SESSAO_ID);

        assertThat(interceptor.preSend(msg, channel)).isSameAs(msg);
    }

    @Test
    @DisplayName("sessao PRIVADO bloqueia o subscribe")
    void deveBloquearSessaoPrivada() {
        SessaoRastreamento sessao = SessaoStub.umaSessao().visibilidade(VisibilidadeSessao.PRIVADO).build();
        when(sessaoRepository.findById(SessaoStub.SESSAO_ID)).thenReturn(Optional.of(sessao));

        Message<byte[]> msg = subscribe("/topic/sessao/" + SessaoStub.SESSAO_ID);

        assertThatThrownBy(() -> interceptor.preSend(msg, channel))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("privada");
    }

    @Test
    @DisplayName("sessao AMIGOS bloqueia ate o auth existir")
    void deveBloquearSessaoAmigos() {
        SessaoRastreamento sessao = SessaoStub.umaSessao().visibilidade(VisibilidadeSessao.AMIGOS).build();
        when(sessaoRepository.findById(SessaoStub.SESSAO_ID)).thenReturn(Optional.of(sessao));

        Message<byte[]> msg = subscribe("/topic/sessao/" + SessaoStub.SESSAO_ID);

        assertThatThrownBy(() -> interceptor.preSend(msg, channel))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("destino fora de /topic/sessao passa sem checar a sessao")
    void deveIgnorarOutrosDestinos() {
        Message<byte[]> msg = subscribe("/topic/outra-coisa");

        assertThat(interceptor.preSend(msg, channel)).isSameAs(msg);
        verifyNoInteractions(sessaoRepository);
    }
}
