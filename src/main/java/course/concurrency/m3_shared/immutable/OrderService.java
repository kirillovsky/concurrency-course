package course.concurrency.m3_shared.immutable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static course.concurrency.m3_shared.immutable.Order.Status.DELIVERED;
import static course.concurrency.m3_shared.immutable.Order.Status.DELIVERING;

public class OrderService {

    private final Map<Long, Order> currentOrders = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(0);

    private long nextId() {
        return nextId.getAndIncrement();
    }

    public long createOrder(List<Item> items) {
        long id = nextId();
        Order order = new Order(id, items);
        currentOrders.put(id, order);
        return id;
    }

    public void updatePaymentInfo(long orderId, PaymentInfo paymentInfo) {
        var newOrder = currentOrders.computeIfPresent(
                orderId,
                (__, oldOrder) -> deliveringOrderIfPermits(oldOrder.withPaymentInfo(paymentInfo))
        );

        deliverIfNeeds(newOrder);
    }

    public void setPacked(long orderId) {
        var newOrder = currentOrders.computeIfPresent(
                orderId,
                (__, oldOrder) -> deliveringOrderIfPermits(oldOrder.withPacked(true))
        );

        deliverIfNeeds(newOrder);
    }

    private Order deliveringOrderIfPermits(Order order) {
        return order.checkStatus() ? order.deliveringOrder() : order;
    }

    private void deliverIfNeeds(Order order) {
        if (order == null || !order.isDelivering()) return;

        deliver(order);
    }

    private void deliver(Order order) {
        /* ... */
        currentOrders.put(order.getId(), order.deliveredOrder());
    }

    public boolean isDelivered(long orderId) {
        return currentOrders.get(orderId).isDelivered();
    }
}
