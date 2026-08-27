package br.com.outbox.sender.message.rabbitmq;

/**
 * Implementação do envio RabbitMQ que delega publicação ao Publisher.
 */
import br.com.outbox.dto.Outbox;
import br.com.outbox.enums.Event;
import br.com.outbox.sender.Sender;
import br.com.outbox.sender.message.DestinyParser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@ApplicationScoped
public class RabbitMQSender implements Sender {

    @Inject
    Publisher publisher;

    @Inject
    DestinyParser destinyParser;

    @Override
    public Event typeSender(){
        return Event.RABBITMQ;
    }

    @Override
    public String send(Outbox outbox){
        long startTime = System.currentTimeMillis();
        MDC.put("transport", "RABBITMQ");
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

        if(params != null){
            try{
                publisher.publish(params);
                long duration = System.currentTimeMillis() - startTime;
                MDC.put("queue", params.get("queue"));
                MDC.put("durationMs", String.valueOf(duration));
                log.info("envio sucesso");
            } catch (Exception ex) {
                error = "Publicação falhou: " + ex.getClass().getSimpleName();
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
        params.put("routing_key", destiny.get("routing_key"));
        params.put("exchange_name", destiny.get("exchange_name"));
        params.put("queue", destiny.get("queue"));
        params.put("payload", outbox.getMessage());

        return params;
    }
}
