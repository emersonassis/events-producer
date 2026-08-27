package br.com.outbox.repository;

/**
 * Interface de repositório que define operações de claim, busca e atualização de registros outbox.
 */
import br.com.outbox.dto.Outbox;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import java.sql.Timestamp;
import java.util.List;

public interface ObjectRepository extends PanacheRepository<Outbox> {
    List<Outbox> claimPending(int batchSize, Timestamp leaseUntil);
    void updateOutbox(Outbox outbox);
}
