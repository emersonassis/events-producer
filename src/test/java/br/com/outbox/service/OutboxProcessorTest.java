package br.com.outbox.service;

import br.com.outbox.config.ConfigClass;
import br.com.outbox.dto.Outbox;
import br.com.outbox.enums.Event;
import br.com.outbox.enums.Status;
import br.com.outbox.repository.ObjectRepository;
import br.com.outbox.sender.Sender;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OutboxProcessorTest {

    private OutboxProcessor processor;
    private ObjectRepository objectRepository;
    private ConfigClass configClass;

    @BeforeEach
    void setUp() throws Exception {
        objectRepository = mock(ObjectRepository.class);
        configClass = mock(ConfigClass.class);

        processor = new OutboxProcessor();
        inject("objectRepository", objectRepository);
        inject("retryPolicy", new RetryPolicy());
        inject("configClass", configClass);

        when(configClass.getBatchSize()).thenReturn(10);
        when(configClass.getClaimLeaseMinutes()).thenReturn(5);
    }

    private void inject(String campo, Object valor) throws Exception {
        Field field = OutboxProcessor.class.getDeclaredField(campo);
        field.setAccessible(true);
        field.set(processor, valor);
    }

    @SuppressWarnings("unchecked")
    private void givenSenders(Sender... senders) throws Exception {
        Instance<Sender> instance = mock(Instance.class);
        doAnswer(inv -> Stream.of(senders)).when(instance).stream();
        inject("instanceSender", instance);
    }

    private Outbox novaOutbox(Long id, String evento) {
        Outbox outbox = new Outbox();
        outbox.setId(id);
        outbox.setEvent(evento);
        outbox.setIntegrated(false);
        outbox.setTentatives(0);
        outbox.setStatus(Status.FOR_PROCESS.getValue());
        return outbox;
    }

    @Test
    void quandoNenhumRegistroPendente_NaoGravaNada() {
        when(objectRepository.claimPending(eq(10), any())).thenReturn(List.of());

        processor.process();

        verify(objectRepository, never()).updateOutbox(any());
    }

    @Test
    void quandoEventoConhecido_EnviaEGuardaComoProcessado() throws Exception {
        Sender sender = mock(Sender.class);
        when(sender.typeSender()).thenReturn(Event.RABBITMQ);
        when(sender.send(any())).thenReturn("");
        givenSenders(sender);

        when(objectRepository.claimPending(eq(10), any())).thenReturn(
                List.of(novaOutbox(1L, Event.RABBITMQ.getValue())));

        processor.process();

        ArgumentCaptor<Outbox> captor = ArgumentCaptor.forClass(Outbox.class);
        verify(objectRepository).updateOutbox(captor.capture());

        assertTrue(captor.getValue().getIntegrated());
        assertNull(captor.getValue().getError());
        assertEquals(Status.PROCESSED.getValue(), captor.getValue().getStatus());
        assertEquals(1, captor.getValue().getTentatives());
    }

    @Test
    void quandoEventoDesconhecido_GravaErroEAguardaNovaTentativa() throws Exception {
        Sender sender = mock(Sender.class);
        when(sender.typeSender()).thenReturn(Event.RABBITMQ);
        givenSenders(sender);

        Outbox venenosa = novaOutbox(99L, "RAABBITMQ");
        when(objectRepository.claimPending(eq(10), any())).thenReturn(List.of(venenosa));

        processor.process();

        ArgumentCaptor<Outbox> captor = ArgumentCaptor.forClass(Outbox.class);
        verify(objectRepository).updateOutbox(captor.capture());

        assertFalse(captor.getValue().getIntegrated());
        assertEquals(1, captor.getValue().getTentatives());
        assertEquals(Status.WAITING_REPLY.getValue(), captor.getValue().getStatus());
        assertTrue(captor.getValue().getError().startsWith("Evento sem sender configurado"));
        assertNotNull(captor.getValue().getProcess_in());
        verify(sender, never()).send(any());
    }

    @Test
    void quandoLinhaComLeaseExpirado_RepocessaNormalmente() throws Exception {
        Sender sender = mock(Sender.class);
        when(sender.typeSender()).thenReturn(Event.RABBITMQ);
        when(sender.send(any())).thenReturn("");
        givenSenders(sender);

        Outbox abandonada = novaOutbox(7L, Event.RABBITMQ.getValue());
        abandonada.setStatus(Status.PROCESSING.getValue());
        abandonada.setProcess_in(Date.from(java.time.Instant.now().minusSeconds(60)));
        when(objectRepository.claimPending(eq(10), any())).thenReturn(List.of(abandonada));

        processor.process();

        ArgumentCaptor<Outbox> captor = ArgumentCaptor.forClass(Outbox.class);
        verify(objectRepository).updateOutbox(captor.capture());

        assertEquals(Status.PROCESSED.getValue(), captor.getValue().getStatus());
        assertTrue(captor.getValue().getIntegrated());
    }

    @Test
    void quandoFalhaAoGravarUmRegistro_LoteContinuaSendoProcessado() throws Exception {
        Sender sender = mock(Sender.class);
        when(sender.typeSender()).thenReturn(Event.RABBITMQ);
        when(sender.send(any())).thenReturn("");
        givenSenders(sender);

        doThrow(new RuntimeException("boom")).doNothing()
                .when(objectRepository).updateOutbox(any());
        when(objectRepository.claimPending(eq(10), any())).thenReturn(
                List.of(novaOutbox(1L, Event.RABBITMQ.getValue()),
                        novaOutbox(2L, Event.RABBITMQ.getValue())));

        processor.process();

        verify(sender, times(2)).send(any());
        verify(objectRepository, times(2)).updateOutbox(any());
    }
}
