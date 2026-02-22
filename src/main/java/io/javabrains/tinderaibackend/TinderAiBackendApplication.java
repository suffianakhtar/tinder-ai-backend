package io.javabrains.tinderaibackend;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.javabrains.tinderaibackend.conversations.ChatMessage;
import io.javabrains.tinderaibackend.conversations.Conversation;
import io.javabrains.tinderaibackend.conversations.ConversationRepository;
import io.javabrains.tinderaibackend.profiles.Gender;
import io.javabrains.tinderaibackend.profiles.Profile;
import io.javabrains.tinderaibackend.profiles.ProfileRepository;

@SpringBootApplication
public class TinderAiBackendApplication implements CommandLineRunner {
	@Autowired
	private ProfileRepository profileRepository;
	@Autowired
	private ConversationRepository conversationRepository;

	public static void main(String[] args) {
		SpringApplication.run(TinderAiBackendApplication.class, args);
	}

	@Override
	public void run(String... args) {
		// Clearing existing profiles and conversations
		profileRepository.deleteAll();
		conversationRepository.deleteAll();
		
		Profile profile = new Profile("1", "Sheifu", "Saif", 30, "Paki", Gender.MALE, "Software Engineer", "foo.jpg",
				"INTP");
		profileRepository.save(profile);
		profile = new Profile("2", "Foo", "Bar", 35, "Irani", Gender.FEMALE, "Cook", "foo.jpg", "INTP");
		profileRepository.save(profile);
		profileRepository.findAll().forEach(System.out::println);

		Conversation conversation = new Conversation("1", profile.id(),
				List.of(new ChatMessage("Hello", profile.id(), LocalDateTime.now())));
		conversationRepository.save(conversation);
		conversationRepository.findAll().forEach(System.out::println);
	}

}
