package br.com.outbox.service;

/**
 * Política de retry que determina status, backoff e tentativas de um registro.
 */
import br.com.outbox.dto.Outbox;
import br.com.outbox.enums.Status;
import jakarta.enterprise.context.ApplicationScoped;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class RetryPolicy {

    private static final int MAX_TENTATIVES = 3;
    private static final int BACKOFF_MINUTES = 1;

    public Outbox apply(Outbox outbox, String error) {
        String normalizedError = error == null ? "" : error;
        var responseSuccess = normalizedError.isBlank();
        outbox.setIntegrated(responseSuccess);
        outbox.setTentatives(outbox.getTentatives() + 1);
        outbox.setStatus(fetchStatus(normalizedError, outbox.getTentatives()).getValue());

        Instant instant = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        if (responseSuccess) {
            outbox.setError(null);
            outbox.setProcessed_at(Date.from(instant));
        } else {
            outbox.setError(normalizedError);
            outbox.setProcessed_at(Date.from(instant));

            if (outbox.getTentatives() < MAX_TENTATIVES) {
                var nextProcessIn = sumMinutes(Timestamp.from(instant), BACKOFF_MINUTES);
                outbox.setProcess_in(new Date(nextProcessIn.getTime()));
            }
        }

        return outbox;
    }

    private Status fetchStatus(String error, int tentatives) {
        if (error.isBlank()) {
            return Status.PROCESSED;
        }
        if (tentatives < MAX_TENTATIVES) {
            return Status.WAITING_REPLY;
        }
        return Status.ERROR;
    }

    private Timestamp sumMinutes(Timestamp time, int minutes) {
        time.setTime(time.getTime() + TimeUnit.MINUTES.toMillis(minutes));
        return time;
    }
}
