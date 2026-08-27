package br.com.outbox;

/**
 * Serviço de orquestração do ciclo de processamento, controlando iterações e pool de threads.
 */
import br.com.outbox.config.ConfigClass;
import br.com.outbox.service.OutboxProcessor;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.CountDownLatch;

import jakarta.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@AllArgsConstructor
public class ExecutorService {

    private final ConfigClass configClass;
    private final OutboxProcessor outboxProcessor;

    @Inject
    ThreadPoolBean threadPoolBean;

    public void execute() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        var iterations = configClass.getNumberThreadInteration();
        log.info("executing {} iteration(s) with {} thread(s)",
                iterations, configClass.getThreads());

        CountDownLatch countDownLatch = new CountDownLatch(iterations);
        for (int i = 0; i < iterations; i++) {
            threadPoolBean.getThreadPool().submit(() -> this.process(countDownLatch));
        }
        countDownLatch.await();

        long duration = System.currentTimeMillis() - startTime;
        log.info("{} iteration(s) processed in {}ms", iterations, duration);
    }

    private void process(CountDownLatch countDownLatch) {
        this.outboxProcessor.process();
        countDownLatch.countDown();
    }
}
