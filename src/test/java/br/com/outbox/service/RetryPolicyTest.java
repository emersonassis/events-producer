package br.com.outbox.service;

import br.com.outbox.dto.Outbox;
import br.com.outbox.enums.Status;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RetryPolicyTest {

    private final RetryPolicy retryPolicy = new RetryPolicy();

    private Outbox novaOutbox(int tentativas) {
        Outbox outbox = new Outbox();
        outbox.setId(1L);
        outbox.setIntegrated(false);
        outbox.setTentatives(tentativas);
        outbox.setStatus(Status.FOR_PROCESS.getValue());
        return outbox;
    }

    @Test
    void quandoSemErro_MarcaComoProcessado() {
        Outbox outbox = novaOutbox(0);

        retryPolicy.apply(outbox, "");

        assertTrue(outbox.getIntegrated());
        assertEquals(1, outbox.getTentatives());
        assertEquals(Status.PROCESSED.getValue(), outbox.getStatus());
        assertNull(outbox.getError());
        assertNotNull(outbox.getProcessed_at());
        assertNull(outbox.getProcess_in());
    }

    @Test
    void quandoComErro_AntesDoLimite_AgendaNovaTentativa() {
        Outbox outbox = novaOutbox(0);

        retryPolicy.apply(outbox, "Conexão falhou");

        assertFalse(outbox.getIntegrated());
        assertEquals(1, outbox.getTentatives());
        assertEquals(Status.WAITING_REPLY.getValue(), outbox.getStatus());
        assertEquals("Conexão falhou", outbox.getError());
        assertNotNull(outbox.getProcessed_at());
        assertNotNull(outbox.getProcess_in());
    }

    @Test
    void quandoComErro_NoLimite_MarcaErroSemNovaTentativa() {
        Outbox outbox = novaOutbox(2);

        retryPolicy.apply(outbox, "Fila inexistente");

        assertFalse(outbox.getIntegrated());
        assertEquals(3, outbox.getTentatives());
        assertEquals(Status.ERROR.getValue(), outbox.getStatus());
        assertNotNull(outbox.getError());
        assertNull(outbox.getProcess_in());
    }
}
