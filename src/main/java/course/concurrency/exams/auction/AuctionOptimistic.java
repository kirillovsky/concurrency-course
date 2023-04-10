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

        while (!latestBid.compareAndSet(currentLatestBid, bid)) {
            currentLatestBid = latestBid.get();
            if (bid.getPrice() <= currentLatestBid.getPrice()) return false;
        }


        notifier.sendOutdatedMessage(currentLatestBid);
        return true;
    }

    public Bid getLatestBid() {
        return latestBid.get();
    }
}
