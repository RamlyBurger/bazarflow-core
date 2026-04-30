package io.ramlyburger.bazarflow;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class BazarflowCoreApplicationTests {

	@Test
	void verifiesModularBoundaries() {
		ApplicationModules.of(BazarflowCoreApplication.class).verify();
	}

}
