package br.com.outbox.enums;

/**
 * Enum que define os tipos de eventos/transportes suportados pela aplicação.
 */
import lombok.Getter;

@Getter
public enum Event {
    RABBITMQ("RABBITMQ"),
    SQS("SQS"),
    HTTP("HTTP");

    private final String value;

    Event(String value) {
        this.value = value;
    }
}