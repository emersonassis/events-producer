package br.com.outbox.enums;

/**
 * Enum que define os estados possíveis de um registro durante o ciclo de vida do outbox.
 * <ul>
 *   <li>FOR_PROCESS: registros aguardando o horário de processamento definido em process_in;</li>
 *   <li>PROCESSED: registros integrados com sucesso ao destino;</li>
 *   <li>WAITING_REPLY: falha transitória aguardando nova tentativa dentro do backoff;</li>
 *   <li>PROCESSING: registros reivindicados e em processamento por um worker no momento;</li>
 *   <li>ERROR: falha definitiva após esgotar o número máximo de tentativas;</li>
 *   <li>STANDBY: registros pausados e excluídos manualmente do processamento.</li>
 * </ul>
 */
import lombok.Getter;

@Getter
public enum Status {
    FOR_PROCESS("FOR_PROCESS"),
    PROCESSED("PROCESSED"),
    WAITING_REPLY("WAITING_REPLY"),
    PROCESSING("PROCESSING"),
    ERROR("ERROR"),
    STANDBY("STANDBY");

    private final String value;

    Status(String value) {
        this.value = value;
    }
}
