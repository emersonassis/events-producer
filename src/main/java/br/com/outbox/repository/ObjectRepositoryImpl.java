package br.com.outbox.repository;

/**
 * Implementação do repositório com claim de posse, queries de seleção e atualização via Panache.
 */
import br.com.outbox.dto.Outbox;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.LockOptions;

@Slf4j
@ApplicationScoped
public class ObjectRepositoryImpl implements ObjectRepository {

    @Inject
    EntityManager entityManager;

    @Override
    @Transactional
    public List<Outbox> claimPending(int batchSize, Timestamp leaseUntil) {
        Instant instant = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        log.debug("claim iniciado - batchSize: {}, now: {}, leaseUntil: {}",
                batchSize, Timestamp.from(instant), leaseUntil);

        // O lease de posse fica em claimed_until: linhas com lease válido (futuro)
        // ficam bloqueadas; com lease vencido ou inexistente voltam à seleção.
        // process_in preserva a semântica original de agendamento do client.
        String query = "SELECT outbox FROM Outbox outbox " +
                "where outbox.tentatives < 3 " +
                "and outbox.integrated IS FALSE " +
                "and outbox.process_in <= :processInDate " +
                "and outbox.status NOT IN ('STANDBY', 'PROCESSED') " +
                "and (outbox.claimed_until IS NULL OR outbox.claimed_until <= :processInDate) " +
                "order by outbox.id";

        var records = entityManager.createQuery(query, Outbox.class)
                .setParameter("processInDate", Timestamp.from(instant))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setHint("jakarta.persistence.lock.timeout", LockOptions.SKIP_LOCKED)
                .setMaxResults(batchSize)
                .getResultList();

        if (!records.isEmpty()) {
            var ids = records.stream().map(Outbox::getId).toList();

            entityManager.createQuery(
                            "UPDATE Outbox outbox SET outbox.status = 'PROCESSING', " +
                                    "outbox.claimed_until = :leaseUntil WHERE outbox.id IN :ids")
                    .setParameter("leaseUntil", leaseUntil)
                    .setParameter("ids", ids)
                    .executeUpdate();
        }

        log.debug("claim concluído - claimed: {}", records.size());
        return records;
    }

    @Override
    @Transactional
    public void updateOutbox(Outbox outbox){
        log.debug("update outbox - id: {}, status: {}, tentativas: {}, integrado: {}",
                outbox.getId(), outbox.getStatus(), outbox.getTentatives(), outbox.getIntegrated());

        Outbox.update("integrated = ?1, " +
                "tentatives = ?2, " +
                "status = ?3, " +
                "error = ?4, processed_at = ?5, " +
                "process_in = ?6 " +
                "where id = ?7", outbox.getIntegrated(),
                outbox.getTentatives(), outbox.getStatus(),
                outbox.getError(),
                outbox.getProcessed_at(), outbox.getProcess_in(),
                outbox.getId());
    }
}
