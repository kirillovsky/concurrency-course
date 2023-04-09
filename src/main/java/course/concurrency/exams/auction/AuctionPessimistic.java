package course.concurrency.exams.auction;

public class AuctionPessimistic implements Auction {

    private Notifier notifier;

    public AuctionPessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private final Object pessimisticLock = new Object();

    private volatile Bid latestBid;

    public boolean propose(Bid bid) {
        if (!isNewBidGreaterThanLatest(bid)) return false;

        boolean isUpdatedLatestBid = false;
        Bid previousLatestBid = null;
        synchronized (pessimisticLock) {
            if (isNewBidGreaterThanLatest(bid)) {
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

    private boolean isNewBidGreaterThanLatest(Bid newBid) {
        return latestBid == null || newBid.getPrice() > latestBid.getPrice();
    }

    public Bid getLatestBid() {
        return latestBid;
    }
}
