package course.concurrency.exams.auction;

import static course.concurrency.exams.auction.Bid.NEGATIVE_INFINITY_BID;

public class AuctionStoppablePessimistic implements AuctionStoppable {

    private Notifier notifier;

    public AuctionStoppablePessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private final Object pessimisticLock = new Object();
    private volatile boolean isAuctionStopped = false;
    private volatile Bid latestBid = NEGATIVE_INFINITY_BID;

    public boolean propose(Bid bid) {
        if (!isCanUpdateLatestBid(latestBid, bid)) return false;

        Bid previousLatestBid;
        synchronized (pessimisticLock) {
            previousLatestBid = latestBid;
            if (isCanUpdateLatestBid(latestBid, bid)) {
                latestBid = bid;
            }
        }

        boolean isUpdatedLatestBid = isCanUpdateLatestBid(previousLatestBid, bid);
        if (isUpdatedLatestBid && previousLatestBid != NEGATIVE_INFINITY_BID) {
            notifier.sendOutdatedMessage(previousLatestBid);
        }

        return isUpdatedLatestBid;
    }

    private boolean isCanUpdateLatestBid(Bid latestBid, Bid newBid) {
        return !isAuctionStopped && newBid.getPrice() > latestBid.getPrice();
    }

    public Bid getLatestBid() {
        return latestBid;
    }

    public Bid stopAuction() {
        isAuctionStopped = true;
        //it's have to use pessimisticLock instead direct access to latestBid, case in time when we set isAuctionStopped
        //actual latestBid can be in updating, but not updated yet.
        //And in this case will be returned previous latestBid but not last
        synchronized (pessimisticLock) {
            return latestBid;
        }
    }
}
