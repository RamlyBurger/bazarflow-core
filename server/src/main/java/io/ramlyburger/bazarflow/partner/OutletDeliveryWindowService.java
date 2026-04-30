package io.ramlyburger.bazarflow.partner;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutletDeliveryWindowService {

	private final OutletRepository outletRepository;

	OutletDeliveryWindowService(OutletRepository outletRepository) {
		this.outletRepository = outletRepository;
	}

	@Transactional(readOnly = true)
	public Map<UUID, OutletDeliveryWindow> findByOutletIds(Collection<UUID> outletIds) {
		if (outletIds == null || outletIds.isEmpty()) {
			return Map.of();
		}

		return outletRepository.findByIdIn(outletIds)
				.stream()
				.map(outlet -> new OutletDeliveryWindow(
						outlet.id(),
						outlet.deliveryZone(),
						outlet.deliveryWindowStart(),
						outlet.deliveryWindowEnd()
				))
				.collect(Collectors.toMap(OutletDeliveryWindow::outletId, Function.identity()));
	}
}
