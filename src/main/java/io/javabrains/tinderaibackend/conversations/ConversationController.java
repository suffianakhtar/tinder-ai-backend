package io.javabrains.tinderaibackend.conversations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.javabrains.tinderaibackend.profiles.ProfileRepository;

@RestController
public class ConversationController {
	private ConversationRepository conversationRepository;
	private ProfileRepository profileRepository;

	@Autowired
	public ConversationController(ConversationRepository conversationRepository, ProfileRepository profileRespository) {
		this.conversationRepository = conversationRepository;
		this.profileRepository = profileRespository;
	}

	@PostMapping("/conversations")
	public Conversation createNewConverstion(@RequestBody CreateConversationRequest request) {
		String profileId = request.profileId;

		profileRepository.findById(profileId).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find profile with ID" + profileId));

		List<Conversation> allConversations = conversationRepository.findAll();

		for (Conversation conversation : allConversations) {
			if (conversation.profileId().equalsIgnoreCase(profileId)) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN,
						"Conversation with this profile ID already exists");
			}
		}

		Conversation conversation = new Conversation(UUID.randomUUID().toString(), profileId, new ArrayList<>());

		conversationRepository.save(conversation);
		return conversation;
	}

	@GetMapping("/conversations/{conversationId}")
	public Conversation getConversation(@PathVariable String conversationId) {
		Conversation conversation = conversationRepository.findById(conversationId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Unable to find conversation with ID" + conversationId));
		return conversation;
	}

	@PostMapping("/conversations/{conversationId}")
	public Conversation addMessageToConversation(@PathVariable String conversationId,
			@RequestBody ChatMessage chatMessage) {
		Conversation conversation = conversationRepository.findById(conversationId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Unable to find conversation with ID" + conversationId));

		profileRepository.findById(chatMessage.authorId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Unable to find profile with ID" + chatMessage.authorId()));

		// TODO: Need to validate that the author of a message happens to be only the
		// profile associated with the message user

		ChatMessage messageWithTime = new ChatMessage(chatMessage.messageText(), chatMessage.authorId(),
				LocalDateTime.now());
		conversation.messages().add(messageWithTime);
		conversationRepository.save(conversation);
		return conversation;
	}

	public record CreateConversationRequest(String profileId) {
	}
}
