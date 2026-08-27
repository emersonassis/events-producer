package br.com.outbox.sender;

/**
 * Contrato que define operação de envio de mensagens e identificação do transporte.
 */
import br.com.outbox.dto.Outbox;
import br.com.outbox.enums.Event;

public interface Sender {
    String send(Outbox outbox);
    Event typeSender();
}
