package br.com.outbox.sender.http;

/**
 * Implementação do envio HTTP que utiliza o Requester para enviar mensagens REST.
 */
import br.com.outbox.dto.Outbox;
import br.com.outbox.enums.Event;
import br.com.outbox.sender.Sender;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.net.http.HttpResponse;
import java.util.*;

@Slf4j
@ApplicationScoped
public class HTTPSender implements Sender {

    @Inject
    Requester httpRequest;

    @Override
    public Event typeSender(){
        return Event.HTTP;
    }

    @Override
    public String send(Outbox outbox) {
        long startTime = System.currentTimeMillis();
        MDC.put("transport", "HTTP");
        log.info("envio iniciado");

        String error = "";
        HttpResponse<String> response;
        
        try {
            response = httpRequest.request(outbox);
            Integer[] acceptStatuCode = {200, 201, 202};
            List<Integer> listAcceptStatuCode = Arrays.asList(acceptStatuCode);

            if(listAcceptStatuCode.contains(response.statusCode())){
                long duration = System.currentTimeMillis() - startTime;
                MDC.put("statusCode", String.valueOf(response.statusCode()));
                MDC.put("durationMs", String.valueOf(duration));
                log.info("envio sucesso");
            } else {
                error = "HTTP " + response.statusCode();
                if (response.body() != null && !response.body().isEmpty()){
                    error += " - " + response.body().substring(0, Math.min(100, response.body().length()));
                }
                MDC.put("statusCode", String.valueOf(response.statusCode()));
                MDC.put("error", error);
                log.warn("envio falhou");
            }
        } catch (Exception e) {
            error = e.getClass().getSimpleName() + ": " + e.getMessage();
            MDC.put("error", error);
            log.warn("envio falhou", e);
        } finally {
            MDC.remove("transport");
            MDC.remove("statusCode");
            MDC.remove("durationMs");
            MDC.remove("error");
        }

        return error;
    }
}
