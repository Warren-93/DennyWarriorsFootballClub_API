package mark.warren93.dev.DennyWarriorsAPI;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DennyWarriorsApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(DennyWarriorsApiApplication.class, args);
	}

}
