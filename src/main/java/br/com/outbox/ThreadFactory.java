package br.com.outbox;

/**
 * Factory de threads que aplica nome customizado para facilitar identificação no pool.
 */
public class ThreadFactory implements java.util.concurrent.ThreadFactory {

    private final String prefix;

    public ThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setName(prefix + thread.getId());
        return thread;
    }
}