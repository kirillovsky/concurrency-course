package course.concurrency.m3_shared.immutable;

import java.util.List;

import static course.concurrency.m3_shared.immutable.Order.Status.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

public final class Order {

    public enum Status {NEW, IN_PROGRESS, DELIVERING, DELIVERED}

    private final Long id;
    private final List<Item> items;
    private final PaymentInfo paymentInfo;
    private final boolean isPacked;
    private final Status status;

    public Order(Long id, List<Item> items) {
        this.id = id;
        this.items = items == null ? emptyList() : unmodifiableList(items);
        this.status = NEW;
        this.isPacked = false;
        this.paymentInfo = null;
    }

    private Order(Long id, List<Item> items, Status status, boolean isPacked, PaymentInfo paymentInfo) {
        this.id = id;
        this.items = items;
        this.status = status;
        this.isPacked = isPacked;
        this.paymentInfo = paymentInfo;
    }

    public boolean checkStatus() {
        return !items.isEmpty() && paymentInfo != null && isPacked && status != DELIVERING && status != DELIVERED;
    }

    public Long getId() {
        return id;
    }

    public List<Item> getItems() {
        return items;
    }

    public PaymentInfo getPaymentInfo() {
        return paymentInfo;
    }

    public Order withPaymentInfo(PaymentInfo paymentInfo) {
        return new Order(id, items, IN_PROGRESS, isPacked, paymentInfo);
    }

    public boolean isPacked() {
        return isPacked;
    }

    public Order withPacked(boolean packed) {
        return new Order(id, items, IN_PROGRESS, packed, paymentInfo);
    }

    public Status getStatus() {
        return status;
    }

    public Order withStatus(Status status) {
        return new Order(id, items, status, isPacked, paymentInfo);
    }
}
