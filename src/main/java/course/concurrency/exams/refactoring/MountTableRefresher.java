package course.concurrency.exams.refactoring;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static course.concurrency.exams.refactoring.Others.MountTableManager;

public class MountTableRefresher {

    public static class MountTableRefresherThreadFactory implements ThreadFactory {
        private final AtomicInteger threadCounter = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("MountTableRefresher#" + threadCounter.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }

    /**
     * Admin server on which refreshed to be invoked.
     */
    public final String adminAddress;
    public volatile boolean isSuccess;
    private final MountTableManager manager;

    public MountTableRefresher(MountTableManager manager,
                               String adminAddress) {
        this.manager = manager;
        this.adminAddress = adminAddress;
    }

    /**
     * Refresh mount table cache of local and remote routers. Local and remote
     * routers will be refreshed differently. Lets understand what are the
     * local and remote routers and refresh will be done differently on these
     * routers. Suppose there are three routers R1, R2 and R3. User want to add
     * new mount table entry. He will connect to only one router, not all the
     * routers. Suppose He connects to R1 and calls add mount table entry through
     * API or CLI. Now in this context R1 is local router, R2 and R3 are remote
     * routers. Because add mount table entry is invoked on R1, R1 will update the
     * cache locally it need not to make RPC call. But R1 will make RPC calls to
     * update cache on R2 and R3.
     */
    public void run() {
        try {
            isSuccess = manager.refresh();
        } catch (Exception e) {
            isSuccess = false;
        }
    }
}
