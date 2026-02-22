package io.javabrains.tinderaibackend.conversations;

import java.util.ArrayList;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.javabrains.tinderaibackend.profiles.ProfileRepository;

@RestController
public class ConversationController {
	private ConversationRepository	conversationRepository;
	private ProfileRepository		profileRepository;		
	
	@Autowired
	public ConversationController(ConversationRepository conversationRepository, ProfileRepository profileRespository) {
		this.conversationRepository = conversationRepository;
		this.profileRepository = profileRespository;
	}

	@PostMapping("/conversations")
	public Conversation createNewConverstion(@RequestBody CreateConversationRequest request) {
		String profileId = request.profileId;
		
		profileRepository.findById(profileId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));			

		Conversation conversation = new Conversation(UUID.randomUUID().toString(), profileId,
				new ArrayList<>());
		
		conversationRepository.save(conversation);
		return conversation;
	}

	public record CreateConversationRequest(String profileId) {}
}
