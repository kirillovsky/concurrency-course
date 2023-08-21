package course.concurrency.exams.refactoring;


import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class MountTableRefresherService {

    private Others.MountTableManagerFactory managerFactory;
    private Others.RouterStore routerStore = new Others.RouterStore();
    private long cacheUpdateTimeout;
    private ExecutorService refreshExecutor;

    /**
     * All router admin clients cached. So no need to create the client again and
     * again. Router admin address(host:port) is used as key to cache RouterClient
     * objects.
     */
    private Others.LoadingCache<String, Others.RouterClient> routerClientsCache;

    /**
     * Removes expired RouterClient from routerClientsCache.
     */
    private ScheduledExecutorService clientCacheCleanerScheduler;

    public MountTableRefresherService(Others.MountTableManagerFactory managerFactory, int refreshExecutorPoolSize) {
        this.managerFactory = managerFactory;
        this.refreshExecutor = Executors.newFixedThreadPool(
                refreshExecutorPoolSize,
                new MountTableRefresher.MountTableRefresherThreadFactory()
        );
    }

    public void serviceInit() {
        long routerClientMaxLiveTime = 15L;
        this.cacheUpdateTimeout = 10L;
        routerClientsCache = new Others.LoadingCache<String, Others.RouterClient>();
        routerStore.getCachedRecords().stream().map(Others.RouterState::getAdminAddress)
                .forEach(addr -> routerClientsCache.add(addr, new Others.RouterClient()));

        initClientCacheCleaner(routerClientMaxLiveTime);
    }

    public void serviceStop() {
        clientCacheCleanerScheduler.shutdown();
        // remove and close all admin clients
        routerClientsCache.cleanUp();
    }

    private void initClientCacheCleaner(long routerClientMaxLiveTime) {
        ThreadFactory tf = r -> {
            Thread t = new Thread(r);
            t.setName("MountTableRefresh_ClientsCacheCleaner");
            t.setDaemon(true);
            return t;
        };

        clientCacheCleanerScheduler =
                Executors.newSingleThreadScheduledExecutor(tf);
        /*
         * When cleanUp() method is called, expired RouterClient will be removed and
         * closed.
         */
        clientCacheCleanerScheduler.scheduleWithFixedDelay(
                () -> routerClientsCache.cleanUp(), routerClientMaxLiveTime,
                routerClientMaxLiveTime, MILLISECONDS);
    }

    /**
     * Refresh mount table cache of this router as well as all other routers.
     */
    public void refresh() {
        List<MountTableRefresher> refreshers = routerStore
                .getCachedRecords()
                .stream()
                .filter(routerState -> {
                    String adminAddress = routerState.getAdminAddress();
                    return adminAddress != null && adminAddress.length() > 0;
                })
                .map(routerState -> {
                    String adminAddress = routerState.getAdminAddress();
                    return isLocalAdmin(adminAddress) ? localRefresher(adminAddress) : remoteRefresher(adminAddress);
                }).collect(Collectors.toList());

        if (!refreshers.isEmpty()) {
            invokeRefresh(refreshers);
        }
    }

    private boolean isLocalAdmin(String adminAddress) {
        return adminAddress.contains("local");
    }

    protected MountTableRefresher localRefresher(String adminAddress) {
        return new MountTableRefresher(managerFactory.create("local"), adminAddress);
    }

    private MountTableRefresher remoteRefresher(String adminAddress) {
        return new MountTableRefresher(managerFactory.create(adminAddress), adminAddress);
    }

    private void invokeRefresh(List<MountTableRefresher> refreshers) {
        var refreshResults = refreshers.stream()
                .map(refresher -> CompletableFuture.runAsync(refresher::run, refreshExecutor))
                .collect(Collectors.toList());

        try {
            CompletableFuture
                    .allOf(refreshResults.toArray(CompletableFuture[]::new))
                    .orTimeout(cacheUpdateTimeout, MILLISECONDS)
                    .handle((__, e) -> {
                        if (e instanceof TimeoutException) {
                            log("Not all router admins updated their cache");
                        }
                        return null;
                    }).get();
        } catch (InterruptedException e) {
            log("Mount table cache refresher was interrupted.");
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        logResult(refreshers);
    }

    private void logResult(List<MountTableRefresher> refreshers) {
        long failureRefreshCount = refreshers
                .stream()
                .filter(refresher -> !refresher.isSuccess)
                .peek(refresher -> removeFromCache(refresher.adminAddress))
                .count();

        long successRefreshCount = refreshers.size() - failureRefreshCount;

        log(String.format(
                "Mount table entries cache refresh successCount=%d,failureCount=%d",
                successRefreshCount, failureRefreshCount));
    }

    private void removeFromCache(String adminAddress) {
        routerClientsCache.invalidate(adminAddress);
    }

    public void log(String message) {
        System.out.println(message);
    }

    public void setCacheUpdateTimeout(long cacheUpdateTimeout) {
        this.cacheUpdateTimeout = cacheUpdateTimeout;
    }

    public void setRouterClientsCache(Others.LoadingCache cache) {
        this.routerClientsCache = cache;
    }

    public void setRouterStore(Others.RouterStore routerStore) {
        this.routerStore = routerStore;
    }
}
