package com.trisha.Loc.loc.websocket;

import com.trisha.Loc.loc.model.dto.request.PontoGpsRequest;
import com.trisha.Loc.loc.model.dto.response.PontoGpsResponse;
import com.trisha.Loc.loc.service.LocalizacaoService;
import com.trisha.Loc.loc.stub.SessaoStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocalizacaoWebSocketController")
class LocalizacaoWebSocketControllerTest {

    @Mock
    private LocalizacaoService localizacaoService;
    @Mock
    private LocalizacaoEventPublisher eventPublisher;

    @InjectMocks
    private LocalizacaoWebSocketController controller;

    @Test
    @DisplayName("registrarPonto deve reusar o service e publicar o resultado para difusao")
    void deveRegistrarPontoEPublicar() {
        PontoGpsRequest request = SessaoStub.umRequestPonto();
        PontoGpsResponse response = PontoGpsResponse.builder()
                .sessaoId(SessaoStub.SESSAO_ID)
                .ordem(1)
                .build();
        when(localizacaoService.registrarPonto(request)).thenReturn(response);

        controller.registrarPonto(request);

        verify(localizacaoService).registrarPonto(request);
        verify(eventPublisher).publicar(response);
    }
}
