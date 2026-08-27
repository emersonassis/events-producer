# events-producer

Quarkus 3.12 / Java 17 outbox-pattern event producer. It dispatches application events from a Postgres `outboxes` table to external targets — REST/HTTP, RabbitMQ, or SQS — with retry, backoff, and at-least-once delivery.

## What it does

`events-producer` decouples writes from delivery. A producer (external app or an internal listener) inserts a row into the `outboxes` table; a periodic worker claims pending rows, sends each one to its configured transport, and records the outcome back on the row.

- **Resilient** — failed sends are retried up to 3 times with a 1-minute backoff, then marked `ERROR`.
- **Non-blocking DB** — sends happen outside any database transaction, so slow HTTP timeouts can no longer abort a transaction.
- **Extensible** — adding a transport is one new class; the processor is never touched.
- **Multi-instance safe** — rows are claimed with `SKIP LOCKED` and a lease, so concurrent workers never double-send.

## Architecture

```
              ENTRY POINTS
   external producer ──▶ outboxes (Postgres)
   SQSListener      ──▶ outboxes
   FetchMessagesDLQ ──▶ outboxes          (POST /fetch_message_dlq, header queue_dlq)
                              │
                              ▼
   Scheduler (@Scheduled every 10s)
      └─ ExecutorService.execute()        (number_thread_interation, CountDownLatch)
          └─ ThreadPoolBean (fixed pool: number_of_threads)
              └─ OutboxProcessor.process()
                         │
                         ▼
              CLAIM  (ObjectRepository.claimPending)   short TX
              SELECT ... PESSIMISTIC_WRITE + SKIP LOCKED (batch_size)
              UPDATE status='PROCESSING', claimed_until=now+lease
                         │
                         ▼
              resolveSender() ── CDI Instance<Sender>, match typeSender()==event
                         │
             ┌───────────┼───────────────┐
         HTTP         RabbitMQ          SQS
    (HTTPSender)   (RabbitMQSender)  (SQSSender)
                         │
                         ▼
              RetryPolicy.apply(outbox, error)
                         │
                         ▼
              updateOutbox()        (own short TX per record)
```

## Getting started

```shell
cp .env.example .env          # app dev/test mode and docker-compose both read this
docker compose up -d          # Postgres 12.5 + RabbitMQ (delayed-message-exchange image)
./mvnw compile quarkus:dev    # start in dev mode; Dev UI at http://localhost:8080/q/dev/
```

## Configuration

All values are read from environment variables / `.env`.

| Variable | Default | Purpose |
|---|---|---|
| `DB_USERNAME` / `DB_PASSWORD` / `DB_HOST` / `DATABASE_NAME` | — | Postgres connection (no default; app won't start without) |
| `RABBITMQ_HOST` / `PORT` / `USER` / `PASS` | — | RabbitMQ connection |
| `APPLICATION_BATCH_SIZE` | `10` | Rows claimed per cycle (`LIMIT` of the SELECT) |
| `APPLICATION_NUMBER_OF_THREADS` | `3` | Size of the fixed worker thread pool |
| `APPLICATION_NUMBER_THREAD_ITERATION` | `1` | Worker iterations per scheduled cycle |
| `APPLICATION_EVENT_NAME` | `events-producer` | Thread name prefix in the pool |
| `APPLICATION_DEFAULT_TRANSACTION_TIMEOUT` | `60s` | Quarkus transaction timeout |
| `APPLICATION_HTTP_SENDER_TIMEOUT` | `50` | HTTP request timeout (seconds). Keep below the transaction timeout |
| `APPLICATION_CLAIM_LEASE_MINUTES` | `5` | Claim lease validity; crashed workers' rows are reclaimed after this |

Database schema is versioned via Flyway in `src/main/resources/db/migration/` (e.g. `V1__add_claimed_until.sql`), applied automatically at startup.

## Transports and adding a new one

Existing transports implement the `Sender` interface:

- `HTTPSender` — outbound REST call (`Requester`)
- `RabbitMQSender` — publish to an exchange/queue (`Publisher`)
- `SQSSender` — publish to a queue (`Publisher`, `SqsClientBuilder`)

To add a transport:

```java
@ApplicationScoped
public class GrpcSender implements Sender {
    @Override
    public Event typeSender() { return Event.GRPC; }
    @Override
    public String send(Outbox outbox) { /* ... */ }
}
```

The processor resolves senders via CDI `Instance<Sender>`, matching `typeSender()` against the row's `event` — no changes to `OutboxProcessor`.

## Processing flow

1. **Scheduling** — every 10s the `Scheduler` submits `OutboxProcessor.process()` to the pool, `number_thread_interation` times, gated by a `CountDownLatch`.
2. **Claim** — `claimPending(batch_size, leaseUntil)` selects eligible rows in one short transaction (`tentatives < 3`, `integrated IS FALSE`, `process_in <= now`, status not `STANDBY`/`PROCESSED`, lease not active), with `PESSIMISTIC_WRITE` + `SKIP_LOCKED`, then marks them `PROCESSING` with `claimed_until = now + claim_lease_minutes`.
3. **Resolve & send** — each record is routed to its `Sender` and dispatched **outside any transaction**. An unknown event is treated as a send failure ("sem sender configurado").
4. **Retry & persist** — `RetryPolicy` classifies the result; `updateOutbox()` persists it in its own per-record transaction.
5. **Requeue** — failed (non-exhausted) rows get `WAITING_REPLY` and `process_in = now + 1min`, so they are reclaimed on a later cycle.

### Status lifecycle

```
FOR_PROCESS ──▶ PROCESSING ──▶ PROCESSED (success, integrated=true)
    ▲               │
    │  lease        │ failure
    │  expired      ▼
    └──PROCESSING ─ WAITING_REPLY ──▶ (backoff) reclaim
                     │
                     ▼ exhausted (3 attempts)
                   ERROR
STANDBY  (excluded from selection)
```

### Guarantees

- **At-least-once delivery**: a crash between send and persist redelivers the message. Consumers must be idempotent.
- **Concurrency**: `SKIP LOCKED` plus the `claimed_until` lease prevent double-claiming across workers and instances; a row whose worker died is reclaimed when its lease expires.
- **Poison rows**: rows with an unconfigured event are treated as a send failure and reach `ERROR` after 3 attempts instead of being re-fetched forever.

## Key config notes

- Keep `APPLICATION_HTTP_SENDER_TIMEOUT` below `APPLICATION_DEFAULT_TRANSACTION_TIMEOUT`.
- Tune `APPLICATION_BATCH_SIZE` × worker iterations to your throughput and DB load.

## Related guides

- [Quarkus - All guides](https://quarkus.io/guides/)
- [JDBC Driver - PostgreSQL](https://quarkus.io/guides/datasource)
- [Quarkus Scheduler](https://quarkus.io/guides/scheduler)
