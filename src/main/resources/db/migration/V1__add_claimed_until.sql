-- Lease de posse do claim em coluna dedicada: preserva a semântica original
-- de process_in (agendamento definido pelo client / backoff do RetryPolicy).
ALTER TABLE outboxes ADD COLUMN IF NOT EXISTS claimed_until TIMESTAMP NULL;
