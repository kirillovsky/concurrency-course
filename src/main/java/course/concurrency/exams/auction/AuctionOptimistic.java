package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicReference;

public class AuctionOptimistic implements Auction {

    private Notifier notifier;

    public AuctionOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private final AtomicReference<Bid> latestBid = new AtomicReference<>();

    public boolean propose(Bid bid) {
        boolean isUpdatedLatestBid;
        Bid currentLatestBid;
        do {
            currentLatestBid = latestBid.get();
            isUpdatedLatestBid = currentLatestBid == null || bid.getPrice() > currentLatestBid.getPrice();
        } while (isUpdatedLatestBid && !latestBid.compareAndSet(currentLatestBid, bid));

        if (isUpdatedLatestBid && currentLatestBid != null) {
            notifier.sendOutdatedMessage(currentLatestBid);
        }
        return isUpdatedLatestBid;
    }

    public Bid getLatestBid() {
        return latestBid.get();
    }
}
