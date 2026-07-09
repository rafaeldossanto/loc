package com.trisha.Loc.loc.mqtt;

import tools.jackson.databind.ObjectMapper;
import com.trisha.Loc.loc.entity.TrackingSession;
import com.trisha.Loc.loc.model.dto.request.GpsPointRequest;
import com.trisha.Loc.loc.model.dto.response.GpsPointResponse;
import com.trisha.Loc.loc.repository.TrackingSessionRepository;
import com.trisha.Loc.loc.service.LocationService;
import com.trisha.Loc.loc.stub.SessionStub;
import com.trisha.Loc.loc.websocket.LocationEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.support.GenericMessage;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocationMqttHandler")
class LocationMqttHandlerTest {

    @Mock
    private LocationService locationService;
    @Mock
    private LocationEventPublisher eventPublisher;
    @Mock
    private TrackingSessionRepository sessionRepository;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private LocationMqttHandler handler;

    @Test
    @DisplayName("processa o ponto do dono da sessao: registra e difunde")
    void shouldProcessPoint() throws Exception {
        String payload = "{\"sessaoId\":\"sessao-1\"}";
        GpsPointRequest request = SessionStub.aPointRequest();
        TrackingSession session = SessionStub.aSession().build();
        GpsPointResponse response = GpsPointResponse.builder().sessionId(SessionStub.SESSION_ID).order(1).build();
        when(objectMapper.readValue(payload, GpsPointRequest.class)).thenReturn(request);
        when(sessionRepository.findById(SessionStub.SESSION_ID)).thenReturn(Optional.of(session));
        when(locationService.registerPoint(request)).thenReturn(response);

        handler.process(new GenericMessage<>(payload));

        verify(locationService).registerPoint(request);
        verify(eventPublisher).publish(response);
    }

    @Test
    @DisplayName("descarta ponto de quem nao e dono da sessao")
    void shouldDiscardPointFromAnotherUser() throws Exception {
        String payload = "{\"sessaoId\":\"sessao-1\"}";
        GpsPointRequest request = new GpsPointRequest(SessionStub.SESSION_ID, SessionStub.LATITUDE, SessionStub.LONGITUDE,
                800.0, 5.0, 1.2, "intruso");
        TrackingSession session = SessionStub.aSession().build();
        when(objectMapper.readValue(payload, GpsPointRequest.class)).thenReturn(request);
        when(sessionRepository.findById(SessionStub.SESSION_ID)).thenReturn(Optional.of(session));

        handler.process(new GenericMessage<>(payload));

        verify(locationService, never()).registerPoint(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("payload invalido apenas loga e nao derruba o consumidor")
    void shouldNotBreakOnInvalidPayload() throws Exception {
        String payload = "invalido";
        when(objectMapper.readValue(payload, GpsPointRequest.class))
                .thenThrow(new RuntimeException("payload invalido"));

        handler.process(new GenericMessage<>(payload));

        verify(locationService, never()).registerPoint(any());
        verify(eventPublisher, never()).publish(any());
    }
}
