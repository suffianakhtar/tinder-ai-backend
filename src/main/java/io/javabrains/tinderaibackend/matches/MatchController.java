package io.javabrains.tinderaibackend.matches;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.javabrains.tinderaibackend.conversations.Conversation;
import io.javabrains.tinderaibackend.conversations.ConversationRepository;
import io.javabrains.tinderaibackend.profiles.Profile;
import io.javabrains.tinderaibackend.profiles.ProfileRepository;

@RestController
public class MatchController {

	private final ProfileRepository profileRepository;
	private final ConversationRepository conversationRepository;
	private final MatchRepository matchRepository;

	@Autowired
	public MatchController(ProfileRepository profileRepository, ConversationRepository conversationRepository,
			MatchRepository matchRepository) {
		this.profileRepository = profileRepository;
		this.conversationRepository = conversationRepository;
		this.matchRepository = matchRepository;
	}

	@PostMapping("/matches")
	public Match createNewMatch(@RequestBody CreateMatchRequest request) {
		String profileId = request.profileId;

		Profile profile = profileRepository.findById(profileId).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find profile with ID" + profileId));

		// TODO: Make sure there are no existing conversations with this profile already
		List<Match> currentMatches = matchRepository.findAll();
		for (Match match : currentMatches) {
			if (match.profile().id().equalsIgnoreCase(profileId)) {
				throw new ResponseStatusException(HttpStatus.CONFLICT,
						"Match with profile ID already exists: " + profileId);
			}
		}

		Conversation conversation = new Conversation(UUID.randomUUID().toString(), profile.id(), new ArrayList<>());

		conversationRepository.save(conversation);

		Match match = new Match(UUID.randomUUID().toString(), profile, conversation.id());
		matchRepository.save(match);

		return match;
	}

	@GetMapping("/matches")
	public List<Match> getAllMatches() {
		return matchRepository.findAll();
	}

	public record CreateMatchRequest(String profileId) {
	}
}
