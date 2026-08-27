package br.com.outbox.config;

/**
 * Centraliza leitura de configurações da aplicação via Eclipse MicroProfile Config.
 */
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.Config;

@ApplicationScoped
@AllArgsConstructor
@Slf4j
public class ConfigClass {

    @PostConstruct
    public void post() { }

    private final Config config;

    public String getThreadName() {
        String threadName = config.getValue("application.event_name", String.class);
        return threadName == null ? "Executor-Thread-" : threadName;
    }

    public Integer getBatchSize() {
        Integer batchSize = config.getValue("application.batch_size", Integer.class);
        return batchSize == null ? 10 : batchSize;
    }

    public Integer getThreads() {
        Integer threads = config.getValue("application.number_of_threads", Integer.class);
        return threads == null ? 1 : threads;
    }

    public Integer getNumberThreadInteration() {
        Integer numberThreadInteration = config.getValue("application.number_thread_interation", Integer.class);
        return numberThreadInteration == null ? 1 : numberThreadInteration;
    }

    public Integer getHttpSenderTimeout() {
        Integer httpSenderTimeout = config.getValue("application.http_sender_timeout", Integer.class);
        return httpSenderTimeout == null ? 50 : httpSenderTimeout;
    }

    public Integer getClaimLeaseMinutes() {
        Integer claimLeaseMinutes = config.getValue("application.claim_lease_minutes", Integer.class);
        return claimLeaseMinutes == null ? 5 : claimLeaseMinutes;
    }

    public Long getDefaultTransactionTimeoutMillis() {
        String raw = config.getOptionalValue("application.default_transaction_timeout", String.class).orElse("60s");
        try {
            if (raw.endsWith("ms")) {
                return Long.parseLong(raw.replace("ms", ""));
            } else if (raw.endsWith("s")) {
                return Long.parseLong(raw.replace("s", "")) * 1000;
            } else {
                return Long.parseLong(raw) * 1000;
            }
        } catch (NumberFormatException e) {
            log.warn("invalid default_transaction_timeout - value: {}, using 60s", raw);
            return 60000L;
        }
    }
}
