package course.concurrency.m2_async.cf.min_price;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static java.lang.Double.NaN;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class PriceAggregator {

    private PriceRetriever priceRetriever = new PriceRetriever();
    private Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);

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
                .reduce((left, right) -> left.thenCombine(right, this::minWithNanConsideration))
                .orElseGet(this::shopListIsEmpty)
                .join();
    }

    private CompletableFuture<Double> loadPriceAsync(long itemId, long shopId) {
        return CompletableFuture
                .supplyAsync(() -> priceRetriever.getPrice(itemId, shopId))
                .thenApply(result -> result == null ? NaN : result)
                .exceptionally(exception -> NaN)
                .completeOnTimeout(NaN, receivePriceTimeoutInMillis, MILLISECONDS);
    }

    private CompletableFuture<Double> shopListIsEmpty() {
        return CompletableFuture.failedFuture(new IllegalStateException("shops list is empty"));
    }

    private Double minWithNanConsideration(Double left, Double right) {
        if (left.isNaN()) return right;
        if (right.isNaN()) return left;
        return Math.min(left, right);
    }
}
