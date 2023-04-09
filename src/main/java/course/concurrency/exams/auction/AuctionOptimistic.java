package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicReference;

import static course.concurrency.exams.auction.Bid.NEGATIVE_INFINITY_BID;

public class AuctionOptimistic implements Auction {

    private Notifier notifier;

    public AuctionOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private final AtomicReference<Bid> latestBid = new AtomicReference<>(NEGATIVE_INFINITY_BID);

    public boolean propose(Bid bid) {
        Bid currentLatestBid = latestBid.get();

        if (bid.getPrice() <= currentLatestBid.getPrice()) return false;

        while (bid.getPrice() > currentLatestBid.getPrice() && !latestBid.compareAndSet(currentLatestBid, bid)) {
            currentLatestBid = latestBid.get();
        }

        boolean isUpdatedLatestBid = bid.getPrice() > currentLatestBid.getPrice();
        if (isUpdatedLatestBid && currentLatestBid != NEGATIVE_INFINITY_BID) {
            notifier.sendOutdatedMessage(currentLatestBid);
        }
        return isUpdatedLatestBid;
    }

    public Bid getLatestBid() {
        return latestBid.get();
    }
}
