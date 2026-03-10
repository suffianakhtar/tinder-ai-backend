package io.javabrains.tinderaibackend.profiles;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@Service
public class ProfileCreationService {
	private static final String PROFILES_FILE_PATH = "profiles.json";

	@Value("#{${tinderai.character.user}}")
	private Map<String, String> userProfileProperties;

	private ProfileRepository profileRepository;

	private OllamaChatModel ollamaChatModel;

	private ProfileTools profileTools;

	private List<Profile> generatedProfiles = new ArrayList<>();

	@Value("${startup-actions.initializeProfile}")
	private Boolean initializeProfiles;

	@Value("${tinderai.lookingForGender}")
	private String lookingForGender;

	@Autowired
	public ProfileCreationService(ProfileRepository profileRepository, OllamaChatModel ollamaChatModel,
			ProfileTools profileTools) {
		this.profileRepository = profileRepository;
		this.ollamaChatModel = ollamaChatModel;
		this.profileTools = profileTools;
	}

	public void createProfiles(int numberOfProfiles) {
		if (!initializeProfiles) {
			return;
		}
		List<Integer> ages = List.of(20, 25, 30, 35, 40);
		List<String> ethnicities = List.of("White", "Black", "Asian", "Indian", "Native American", "Hispanic");

		profileTools.setGeneratedProfiles(this.generatedProfiles);

		for (int i = 0; i < numberOfProfiles; i++) {
			int age = ages.get(ThreadLocalRandom.current().nextInt(ages.size()));
			Gender gender = Gender.valueOf(this.lookingForGender);
			String ethnicity = ethnicities.get(ThreadLocalRandom.current().nextInt(ethnicities.size()));

			StringBuilder sb = new StringBuilder();
			sb.append("Create a unique and creative Tinder profile for a ");
			sb.append(age).append(" year old ");
			sb.append(ethnicity).append(" ").append(toNaturalLanguage(gender)).append(". ");
			sb.append("Generate a unique first name and last name suitable for their ethnicity. ");
			sb.append("Write a fun and original tinder bio in one complete sentence under 150 characters. ");
			sb.append("The bio must be complete and not cut off. ");
			sb.append("Choose a Myers Briggs personality type. ");
			sb.append("The gender field must be saved as exactly: ").append(gender.name());
			sb.append(" Save the profile.");

			Prompt prompt = new Prompt(sb.toString(),
					OllamaChatOptions.builder().toolCallbacks(ToolCallbacks.from(profileTools)).build());
			ollamaChatModel.call(prompt);
		}

		saveProfilesToJson(this.generatedProfiles);
	}

	private void saveProfilesToJson(List<Profile> generatedProfiles) {
		Gson gson = new Gson();
		File file = new File(PROFILES_FILE_PATH);

		if (file.exists()) {
			try {
				List<Profile> existingProfiles = gson.fromJson(new FileReader(file),
						new TypeToken<ArrayList<Profile>>() {
						}.getType());
				if (existingProfiles != null && !existingProfiles.isEmpty()) {
					generatedProfiles.addAll(existingProfiles);
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		for (Profile profile : generatedProfiles) {
			if (profile.imageUrl() == null) {
				profile = generateProfileImage(profile);
			}
		}

		try (FileWriter writer = new FileWriter(file)) {
			String jsonString = gson.toJson(generatedProfiles);
			writer.write(jsonString);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Profile generateProfileImage(Profile profile) {
		
		return null;
	}

	private String toNaturalLanguage(Gender gender) {
		return switch (gender) {
		case MALE -> "man";
		case FEMALE -> "woman";
		case NON_BINARY -> "person";
		};
	}

	public void saveProfilesToDb() {
		Gson gson = new Gson();
		File file = new File(PROFILES_FILE_PATH);
		
		if (!file.exists()) {
			return;
		}
		
		try {
			List<Profile> existingProfiles = gson.fromJson(new FileReader(file),
					new TypeToken<ArrayList<Profile>>() {
					}.getType());
			profileRepository.deleteAll();
			profileRepository.saveAll(existingProfiles);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		Profile profile = new Profile(userProfileProperties.get("id"), userProfileProperties.get("firstName"),
				userProfileProperties.get("lastName"), Integer.parseInt(userProfileProperties.get("age")),
				userProfileProperties.get("ethnicity"), Gender.valueOf(userProfileProperties.get("gender")),
				userProfileProperties.get("bio"), userProfileProperties.get("imageUrl"),
				userProfileProperties.get("myersBriggsPersonalityType"));
		profileRepository.save(profile);
	}
}
