package br.com.outbox;

/**
 * Pool de threads fixo disponibilizado como bean CDI para o ExecutorService.
 */
import br.com.outbox.config.ConfigClass;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;

@Slf4j
@ApplicationScoped
public class ThreadPoolBean {

    java.util.concurrent.ExecutorService executorService;

    ThreadPoolBean(ConfigClass configClass){
        log.info("creating fixed thread pool - threads: {}", configClass.getThreads());
        this.executorService = Executors.newFixedThreadPool(
                configClass.getThreads(), new ThreadFactory(configClass.getThreadName())
        );
    }

    public java.util.concurrent.ExecutorService getThreadPool(){
        return this.executorService;
    }
}
