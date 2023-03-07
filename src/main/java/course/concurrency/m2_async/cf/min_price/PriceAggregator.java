package course.concurrency.m2_async.cf.min_price;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static java.lang.Double.NaN;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class PriceAggregator {

    private PriceRetriever priceRetriever = new PriceRetriever();
    private Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);
    private Executor ioPool = Executors.newCachedThreadPool();

    private final int receivePriceTimeoutInMillis;

    public PriceAggregator(int receivePriceTimeoutInMillis) {
        this.receivePriceTimeoutInMillis = receivePriceTimeoutInMillis;
    }

    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }

    public double getMinPrice(long itemId) {
        return shopIds.stream()
                .map(shopId -> loadPriceAsync(itemId, shopId))
                .reduce((left, right) -> left.thenCombine(right, Math::min))
                .map(resultCF -> resultCF.thenApply(PriceAggregator::infinityToNaN))
                .orElseGet(PriceAggregator::shopListIsEmpty)
                .join();
    }

    private CompletableFuture<Double> loadPriceAsync(long itemId, long shopId) {
        return CompletableFuture
                .supplyAsync(() -> priceRetriever.getPrice(itemId, shopId), ioPool)
                .thenApply(result -> result == null ? POSITIVE_INFINITY : result)
                .exceptionally(exception -> POSITIVE_INFINITY)
                .completeOnTimeout(POSITIVE_INFINITY, receivePriceTimeoutInMillis, MILLISECONDS);
    }

    private static CompletableFuture<Double> shopListIsEmpty() {
        return CompletableFuture.completedFuture(NaN);
    }

    private static double infinityToNaN(Double d) {
        return d.isInfinite() ? NaN: d;
    }
}
