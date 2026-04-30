package io.ramlyburger.bazarflow.ordering;

import io.ramlyburger.bazarflow.common.AuditTrailEvent;
import io.ramlyburger.bazarflow.common.ConflictException;
import io.ramlyburger.bazarflow.common.NotFoundException;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderFulfillmentService {

	private final CustomerOrderRepository orderRepository;
	private final OrderStatusHistoryRepository statusHistoryRepository;
	private final ApplicationEventPublisher eventPublisher;

	OrderFulfillmentService(
			CustomerOrderRepository orderRepository,
			OrderStatusHistoryRepository statusHistoryRepository,
			ApplicationEventPublisher eventPublisher
	) {
		this.orderRepository = orderRepository;
		this.statusHistoryRepository = statusHistoryRepository;
		this.eventPublisher = eventPublisher;
	}

	@Transactional
	public void markDelivered(UUID orderId) {
		CustomerOrder order = findAcceptedOrderForUpdate(
				orderId,
				"ORDER_NOT_DELIVERABLE",
				"Only accepted orders can be marked delivered"
		);

		order.markDelivered();
		statusHistoryRepository.save(OrderStatusHistory.transition(
				order.id(),
				OrderStatus.ACCEPTED,
				OrderStatus.DELIVERED,
				"Order delivered",
				"system"
		));
		publishDelivered(order);
	}

	@Transactional
	public void markDeliveryFailed(UUID orderId, String reason) {
		CustomerOrder order = findAcceptedOrderForUpdate(
				orderId,
				"ORDER_DELIVERY_NOT_FAILABLE",
				"Only accepted orders can be marked delivery failed"
		);

		order.markDeliveryFailed();
		statusHistoryRepository.save(OrderStatusHistory.transition(
				order.id(),
				OrderStatus.ACCEPTED,
				OrderStatus.DELIVERY_FAILED,
				"Delivery failed",
				"system"
		));
		publishDeliveryFailed(order, reason);
	}

	private CustomerOrder findAcceptedOrderForUpdate(UUID orderId, String errorCode, String message) {
		CustomerOrder order = orderRepository.findByIdForUpdate(orderId)
				.orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order was not found"));
		if (order.status() != OrderStatus.ACCEPTED) {
			throw new ConflictException(errorCode, message);
		}
		return order;
	}

	private void publishDelivered(CustomerOrder order) {
		eventPublisher.publishEvent(AuditTrailEvent.record(
				"ordering",
				"ORDER",
				order.id(),
				"ORDER_DELIVERED",
				"Order delivered",
				Map.of(
						"orderNumber", order.orderNumber(),
						"status", order.status().name()
				)
		));
	}

	private void publishDeliveryFailed(CustomerOrder order, String reason) {
		eventPublisher.publishEvent(AuditTrailEvent.record(
				"ordering",
				"ORDER",
				order.id(),
				"ORDER_DELIVERY_FAILED",
				"Order delivery failed",
				Map.of(
						"orderNumber", order.orderNumber(),
						"status", order.status().name(),
						"reason", reason
				)
		));
	}
}
