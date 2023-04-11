package course.concurrency.exams.auction;

import static course.concurrency.exams.auction.Bid.NEGATIVE_INFINITY_BID;

public class AuctionPessimistic implements Auction {

    private Notifier notifier;

    public AuctionPessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private final Object pessimisticLock = new Object();

    private volatile Bid latestBid = NEGATIVE_INFINITY_BID;

    public boolean propose(Bid bid) {
        if (bid.getPrice() <= latestBid.getPrice()) return false;

        Bid previousLatestBid;
        synchronized (pessimisticLock) {
            previousLatestBid = latestBid;
            if (bid.getPrice() <= previousLatestBid.getPrice()) return false;
            latestBid = bid;
        }

        notifier.sendOutdatedMessage(previousLatestBid);

        return true;
    }

    public Bid getLatestBid() {
        return latestBid;
    }
}
