package course.concurrency.exams.auction;

public class AuctionStoppablePessimistic implements AuctionStoppable {

    private Notifier notifier;

    public AuctionStoppablePessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private final Object pessimisticLock = new Object();
    private volatile boolean isAuctionStopped = false;
    private volatile Bid latestBid;

    public boolean propose(Bid bid) {
        if (!isCanUpdateLatestBid(bid)) return false;

        boolean isUpdatedLatestBid = false;
        Bid previousLatestBid = null;
        synchronized (pessimisticLock) {
            if (isCanUpdateLatestBid(bid)) {
                previousLatestBid = latestBid;
                latestBid = bid;
                isUpdatedLatestBid = true;
            }
        }

        if (isUpdatedLatestBid && previousLatestBid != null) {
            notifier.sendOutdatedMessage(previousLatestBid);
        }

        return isUpdatedLatestBid;
    }

    private boolean isCanUpdateLatestBid(Bid newBid) {
        return !isAuctionStopped && (latestBid == null || newBid.getPrice() > latestBid.getPrice());
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
