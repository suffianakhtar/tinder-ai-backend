package io.javabrains.tinderaibackend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.javabrains.tinderaibackend.profiles.ProfileCreationService;

@SpringBootApplication
public class TinderAiBackendApplication implements CommandLineRunner {
	@Autowired
	private ProfileCreationService profileCreationService;


	public static void main(String[] args) {
		SpringApplication.run(TinderAiBackendApplication.class, args);
	}

	@Override
	public void run(String... args) {
		profileCreationService.saveProfilesToDb();
	}

}
