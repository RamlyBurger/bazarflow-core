package io.ramlyburger.bazarflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

@Modulithic(systemName = "BazarFlow Core", sharedModules = "common")
@SpringBootApplication
public class BazarflowCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(BazarflowCoreApplication.class, args);
	}

}
