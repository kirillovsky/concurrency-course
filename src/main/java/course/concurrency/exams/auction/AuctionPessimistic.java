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
            if (bid.getPrice() > latestBid.getPrice()) {
                latestBid = bid;
            }
        }

        boolean isUpdatedLatestBid = bid.getPrice() > previousLatestBid.getPrice();
        if (isUpdatedLatestBid && previousLatestBid != NEGATIVE_INFINITY_BID) {
            notifier.sendOutdatedMessage(previousLatestBid);
        }

        return isUpdatedLatestBid;
    }

    public Bid getLatestBid() {
        return latestBid;
    }
}
