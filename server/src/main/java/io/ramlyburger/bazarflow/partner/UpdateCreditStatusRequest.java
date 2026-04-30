package io.ramlyburger.bazarflow.partner;

import jakarta.validation.constraints.NotNull;

public record UpdateCreditStatusRequest(
		@NotNull CreditStatus creditStatus
) {
}
