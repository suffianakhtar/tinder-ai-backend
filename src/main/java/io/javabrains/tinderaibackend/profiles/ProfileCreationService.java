package io.javabrains.tinderaibackend.profiles;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
	
	@Autowired
	private ProfileRepository profileRepository;

	public void saveProfilesToDb() {
		Gson gson = new Gson();
		
		try {
			List<Profile> existingProfiles = gson.fromJson(new FileReader(PROFILES_FILE_PATH), new TypeToken<ArrayList<Profile>>() {}.getType());
			profileRepository.deleteAll();
			profileRepository.saveAll(existingProfiles);
		} catch(FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		
		Profile profile = new Profile(
				userProfileProperties.get("id"),
				userProfileProperties.get("firstName"),
				userProfileProperties.get("lastName"),
				Integer.parseInt(userProfileProperties.get("age")),
				userProfileProperties.get("ethnicity"),
				Gender.valueOf(userProfileProperties.get("gender")),
				userProfileProperties.get("bio"),
				userProfileProperties.get("imageUrl"),
				userProfileProperties.get("myersBriggsPersonalityType")
		);
		profileRepository.save(profile);
	}
}
