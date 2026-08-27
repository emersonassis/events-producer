package br.com.outbox.service;

/**
 * Processador que orquestra o claim, envio e persistência do resultado de registros outbox.
 */
import br.com.outbox.config.ConfigClass;
import br.com.outbox.dto.Outbox;
import br.com.outbox.repository.ObjectRepository;
import br.com.outbox.sender.Sender;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Slf4j
@ApplicationScoped
public class OutboxProcessor {

    @Inject
    private ObjectRepository objectRepository;

    @Inject
    private Instance<Sender> instanceSender;

    @Inject
    private RetryPolicy retryPolicy;

    @Inject
    private ConfigClass configClass;

    public void process() {
        long startTime = System.currentTimeMillis();

        var leaseUntil = Timestamp.from(Instant.now()
                .plus(configClass.getClaimLeaseMinutes(), ChronoUnit.MINUTES));
        var records = objectRepository.claimPending(configClass.getBatchSize(), leaseUntil);

        if (records.isEmpty()) {
            log.info("no pending registries");
            return;
        }

        log.info("processing {} registry(ies)", records.size());
        int successCount = 0;
        int errorCount = 0;

        for(Outbox outbox: records){
            MDC.put("outboxId", String.valueOf(outbox.getId()));
            MDC.put("event", outbox.getEvent());
            try{
                var senderOpt = resolveSender(outbox);
                String error;
                if (senderOpt.isEmpty()) {
                    error = "Evento sem sender configurado: " + outbox.getEvent();
                    log.error("sem sender configurado");
                } else {
                    error = sendMessage(senderOpt.get(), outbox);
                }
                retryPolicy.apply(outbox, error);
                objectRepository.updateOutbox(outbox);
                if (error.isBlank()) {
                    successCount++;
                } else {
                    errorCount++;
                }
            }catch (Exception e){
                errorCount++;
                log.error("erro ao processar registro", e);
            }finally{
                MDC.remove("outboxId");
                MDC.remove("event");
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("conclusão - sucesso: {}, erros: {}, tempo: {}ms",
                successCount, errorCount, duration);
    }

    private Optional<Sender> resolveSender(Outbox outbox) {
        return instanceSender.stream().filter(
                s -> s.typeSender()
                        .getValue()
                        .equals(outbox.getEvent())
        ).findAny();
    }

    private String sendMessage(Sender sender, Outbox outbox){
        log.info("envio iniciado");
        return sender.send(outbox);
    }
}
