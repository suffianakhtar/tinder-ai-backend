package io.javabrains.tinderaibackend.profiles;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

import io.javabrains.tinderaibackend.common.comfyui.ComfyUIRequest;
import io.javabrains.tinderaibackend.common.comfyui.ComfyUIService;

@Service
public class ProfileCreationService {
	private static final String PROFILES_FILE_PATH = "profiles.json";

	@Value("#{${tinderai.character.user}}")
	private Map<String, String> userProfileProperties;

	private ProfileRepository profileRepository;

	private OllamaChatModel ollamaChatModel;

	private ProfileTools profileTools;

	private ComfyUIService comfyUIService;

	private List<Profile> generatedProfiles = new ArrayList<>();

	@Value("${startup-actions.initializeProfile}")
	private Boolean initializeProfiles;

	@Value("${tinderai.lookingForGender}")
	private String lookingForGender;

	@Value("${images-output-folder}")
	private String imagesFolder;

	@Autowired
	public ProfileCreationService(ProfileRepository profileRepository, OllamaChatModel ollamaChatModel,
			ProfileTools profileTools, ComfyUIService comfyUIService) {
		this.profileRepository = profileRepository;
		this.ollamaChatModel = ollamaChatModel;
		this.profileTools = profileTools;
		this.comfyUIService = comfyUIService;
	}

	public void createProfiles(int numberOfProfiles) {
		if (!initializeProfiles) {
			return;
		}
		List<Integer> ages = List.of(18, 20, 22, 25, 27, 30, 33, 36);
		List<String> ethnicities = List.of("White", "Black", "Asian", "Indian", "Native American", "Hispanic", "Middle Eastern", "Pacific Islander", "Multiracial");

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

	public void saveProfilesToJson(List<Profile> generatedProfiles) {
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

		for (int i = 0; i < generatedProfiles.size(); i++) {
			Profile profile = generatedProfiles.get(i);
			if (profile.imageUrl() == null) {
				Profile updatedProfile = generateProfileImage(profile);
				generatedProfiles.set(i, updatedProfile);
			}
		}

		try (FileWriter writer = new FileWriter(file)) {
			String jsonString = gson.toJson(generatedProfiles);
			writer.write(jsonString);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Profile generateProfileImage(Profile profile) {
		StringBuilder positivePrompt = new StringBuilder();
		positivePrompt.append("RAW photo, (realistic:1.4), portrait of a ").append(profile.age());
		positivePrompt.append(" year old ");
		positivePrompt.append(profile.ethnicity()).append(" ");
		positivePrompt.append(toNaturalLanguage(profile.gender())).append(" ");
		positivePrompt.append("beautiful face, detailed eyes, natural skin,");
		positivePrompt.append(" photorealistic, 8k, sharp focus, professional headshot");

		String negativePrompt = "ugly, deformed, disfigured, mutated, extra limbs, "
				+ "blurry, low quality, cartoon, anime, painting, "
				+ "bad anatomy, bad eyes, neon, overexposed, psychedelic";

		ComfyUIRequest request = new ComfyUIRequest(positivePrompt.toString(), negativePrompt, 30, "dpmpp_2m", 512,
				512, 7.0);

		// Generate image
		// Save the generated image in the resources folder
		// Link the image name to the profile's image URL field
		try {
			byte[] imageBytes = comfyUIService.generateImage(request);
			Path folder = Path.of(imagesFolder);
			if (!Files.exists(folder)) {
				Files.createDirectories(folder);
			}
			String filename = "profile_" + UUID.randomUUID() + ".png";
			Path filePath = folder.resolve(filename);
			Files.write(filePath, imageBytes);

			return new Profile(profile.id(), profile.firstName(), profile.lastName(), profile.age(),
					profile.ethnicity(), profile.gender(), profile.bio(), "/images/" + filename,
					profile.myersBriggsPersonalityType());
		} catch (Exception e) {
			e.printStackTrace();
			return profile;
		}
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
			List<Profile> existingProfiles = gson.fromJson(new FileReader(file), new TypeToken<ArrayList<Profile>>() {
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
