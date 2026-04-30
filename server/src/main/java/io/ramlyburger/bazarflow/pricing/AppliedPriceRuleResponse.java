package io.ramlyburger.bazarflow.pricing;

import java.util.UUID;

public record AppliedPriceRuleResponse(
		UUID ruleId,
		String ruleCode,
		String description,
		int priority
) {
}
