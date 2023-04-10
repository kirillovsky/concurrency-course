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
        boolean[] isAlreadyStoppedHolder = new boolean[1];
        Bid currentLatestBid = latestBidOrStopped.get(isAlreadyStoppedHolder);

        if (isCanNotUpdateLatestBid(currentLatestBid, bid, isAlreadyStoppedHolder[0])) return false;

        while (!latestBidOrStopped.compareAndSet(currentLatestBid, bid, false, false)) {
            currentLatestBid = latestBidOrStopped.get(isAlreadyStoppedHolder);
            if (isCanNotUpdateLatestBid(currentLatestBid, bid, isAlreadyStoppedHolder[0])) return false;
        }

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
        boolean[] isAlreadyStoppedHolder = new boolean[1];
        Bid latestBid;

        do {
            latestBid = latestBidOrStopped.get(isAlreadyStoppedHolder);
        } while (!isAlreadyStoppedHolder[0] && !latestBidOrStopped.attemptMark(latestBid, true));

        return latestBid;
    }
}
