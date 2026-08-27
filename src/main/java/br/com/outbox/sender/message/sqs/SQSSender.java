package br.com.outbox.sender.message.sqs;

/**
 * Implementação do envio SQS que delega publicação ao Publisher.
 */
import br.com.outbox.dto.Outbox;
import br.com.outbox.enums.Event;
import br.com.outbox.sender.Sender;
import br.com.outbox.sender.message.DestinyParser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@ApplicationScoped
public class SQSSender implements Sender {

    @Inject
    Publisher publisher;

    @Inject
    DestinyParser destinyParser;

    @Override
    public Event typeSender(){
        return Event.SQS;
    }

    @Override
    public String send(Outbox outbox) {
        long startTime = System.currentTimeMillis();
        MDC.put("transport", "SQS");
        log.info("envio iniciado");

        String error = "";
        HashMap<String, String> params = null;
        
        try {
            params = getParams(outbox);
        } catch (Exception ex) {
            error = "Parse falhou: " + ex.getClass().getSimpleName();
            MDC.put("error", error);
            log.warn("envio falhou", ex);
        }

        if (params != null){
            try {
                publisher.publish(params);
                long duration = System.currentTimeMillis() - startTime;
                MDC.put("queue", params.get("queue_url"));
                MDC.put("durationMs", String.valueOf(duration));
                log.info("envio sucesso");
            } catch (SdkClientException ex) {
                error = "Conexão falhou: " + ex.getMessage();
                MDC.put("error", error);
                log.warn("envio falhou", ex);
            } catch (QueueDoesNotExistException ex) {
                error = "Fila inexistente: " + ex.getMessage();
                MDC.put("error", error);
                log.warn("envio falhou", ex);
            }
        }

        MDC.remove("transport");
        MDC.remove("queue");
        MDC.remove("durationMs");
        MDC.remove("error");
        return error;
    }

    private HashMap<String, String> getParams(Outbox outbox) {
        HashMap<String, String> params = new HashMap<>();

        Map<String, String> destiny = destinyParser.parse(outbox.getDestiny());
        params.put("queue_url", destiny.get("queue_url"));
        params.put("region", destiny.get("region"));
        params.put("payload", outbox.getMessage());
        params.put("outbox_id", String.valueOf(outbox.getId()));

        return params;
    }
}
