package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicMarkableReference;

import static course.concurrency.exams.auction.Bid.NEGATIVE_INFINITY_BID;

public class AuctionStoppableOptimistic implements AuctionStoppable {

    private Notifier notifier;

    public AuctionStoppableOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private final AtomicMarkableReference<Bid> latestBidOrStopped =
            new AtomicMarkableReference<>(NEGATIVE_INFINITY_BID, false);

    public boolean propose(Bid bid) {
        Bid currentLatestBid;

        do {
            boolean isAlreadyStopped = latestBidOrStopped.isMarked();
            currentLatestBid = latestBidOrStopped.getReference();
            if (isCanNotUpdateLatestBid(currentLatestBid, bid, isAlreadyStopped)) return false;
        } while (!latestBidOrStopped.compareAndSet(currentLatestBid, bid, false, false));

        notifier.sendOutdatedMessage(currentLatestBid);
        return true;
    }

    private boolean isCanNotUpdateLatestBid(Bid latestBid, Bid newBid, boolean isAuctionStopped) {
        return isAuctionStopped || newBid.getPrice() <= latestBid.getPrice();
    }

    public Bid getLatestBid() {
        return latestBidOrStopped.getReference();
    }

    public Bid stopAuction() {
        Bid latestBid;
        boolean isAlreadyStopped;

        do {
            latestBid = latestBidOrStopped.getReference();
            isAlreadyStopped = latestBidOrStopped.isMarked();
        } while (!isAlreadyStopped && !latestBidOrStopped.attemptMark(latestBid, true));

        return latestBid;
    }
}
