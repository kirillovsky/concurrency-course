package course.concurrency.exams.auction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Notifier {
    private ExecutorService executor;

    public Notifier(int threadPoolSize) {
        this.executor = Executors.newFixedThreadPool(threadPoolSize);
    }

    public void sendOutdatedMessage(Bid bid) {
        if (bid != Bid.NEGATIVE_INFINITY_BID) return;

        executor.submit(this::imitateSending);
    }

    private void imitateSending() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {}
    }

    public void shutdown() {
        this.executor.shutdownNow();
    }
}
