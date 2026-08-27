package br.com.outbox;

/**
 * Disparador periódico que inicia o ciclo de processamento do outbox a cada intervalo configurado.
 */
import io.quarkus.scheduler.Scheduled;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class Scheduler {

    private final ExecutorService executorTestsService;

    @Scheduled(every="10s")
    void job() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        log.info("cycle started - thread: {}", Thread.currentThread().getName());
        
        try {
            executorTestsService.execute();
            long duration = System.currentTimeMillis() - startTime;
            log.info("cycle finished - durationMs: {}, thread: {}", duration, Thread.currentThread().getName());
        } catch (Exception e) {
            log.error("cycle failed - thread: {}", Thread.currentThread().getName(), e);
        }
    }
}
