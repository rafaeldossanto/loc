package com.trisha.Loc.loc.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trisha.Loc.loc.model.dto.request.PontoGpsRequest;
import com.trisha.Loc.loc.model.dto.response.PontoGpsResponse;
import com.trisha.Loc.loc.service.LocalizacaoService;
import com.trisha.Loc.loc.stub.SessaoStub;
import com.trisha.Loc.loc.websocket.LocalizacaoEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.support.GenericMessage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocalizacaoMqttHandler")
class LocalizacaoMqttHandlerTest {

    @Mock
    private LocalizacaoService localizacaoService;
    @Mock
    private LocalizacaoEventPublisher eventPublisher;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private LocalizacaoMqttHandler handler;

    @Test
    @DisplayName("processa o ponto recebido: registra pelo service e difunde")
    void deveProcessarPonto() throws Exception {
        String payload = "{\"sessaoId\":\"sessao-1\"}";
        PontoGpsRequest request = SessaoStub.umRequestPonto();
        PontoGpsResponse response = PontoGpsResponse.builder()
                .sessaoId(SessaoStub.SESSAO_ID).ordem(1).build();
        when(objectMapper.readValue(payload, PontoGpsRequest.class)).thenReturn(request);
        when(localizacaoService.registrarPonto(request)).thenReturn(response);

        handler.processar(new GenericMessage<>(payload));

        verify(localizacaoService).registrarPonto(request);
        verify(eventPublisher).publicar(response);
    }

    @Test
    @DisplayName("payload invalido apenas loga e nao derruba o consumidor")
    void naoDeveQuebrarComPayloadInvalido() throws Exception {
        String payload = "invalido";
        when(objectMapper.readValue(payload, PontoGpsRequest.class))
                .thenThrow(new RuntimeException("payload invalido"));

        handler.processar(new GenericMessage<>(payload));

        verify(localizacaoService, never()).registrarPonto(any());
        verify(eventPublisher, never()).publicar(any());
    }
}
