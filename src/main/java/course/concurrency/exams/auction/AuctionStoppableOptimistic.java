package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class AuctionStoppableOptimistic implements AuctionStoppable {

    private Notifier notifier;

    public AuctionStoppableOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private final AtomicMarkableReference<Bid> latestBidOrStopped =
            new AtomicMarkableReference<>(null, false);

    public boolean propose(Bid bid) {
        boolean isUpdatedLatestBid;
        Bid currentLatestBid;
        do {
            boolean[] isAlreadyStoppedHolder = new boolean[1];
            currentLatestBid = latestBidOrStopped.get(isAlreadyStoppedHolder);

            if (isAlreadyStoppedHolder[0]) return false;

            isUpdatedLatestBid = currentLatestBid == null || bid.getPrice() > currentLatestBid.getPrice();
        } while (isUpdatedLatestBid && !latestBidOrStopped.compareAndSet(currentLatestBid, bid, false, false));

        if (isUpdatedLatestBid && currentLatestBid != null) {
            notifier.sendOutdatedMessage(currentLatestBid);
        }
        return isUpdatedLatestBid;
    }

    public Bid getLatestBid() {
        return latestBidOrStopped.getReference();
    }

    public Bid stopAuction() {
        boolean[] isAlreadyStoppedHolder = new boolean[1];
        Bid latestBid;

        do {
            latestBid = latestBidOrStopped.get(isAlreadyStoppedHolder);
        } while (!isAlreadyStoppedHolder[0] && !latestBidOrStopped.attemptMark(latestBid,  true));

        return latestBid;
    }
}
