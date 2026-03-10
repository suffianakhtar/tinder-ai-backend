package io.javabrains.tinderaibackend.profiles;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class ProfileTools {

	private List<Profile> generatedProfiles;

	public void setGeneratedProfiles(List<Profile> generatedProfiles) {
		this.generatedProfiles = generatedProfiles;
	}

	@Tool(name = "saveProfile", description = "Saves the profile information")
	public void saveProfile(String firstName, String lastName, int age, String ethnicity, String gender, String bio,
			String myersBriggsPersonalityType) {
		Gender genderEnum = Gender.valueOf(gender.trim().toUpperCase());
		try {
			Profile profile = new Profile(null, firstName, lastName, age, ethnicity, genderEnum, bio, null,
					myersBriggsPersonalityType);
			System.out.println("\n\nGenerated Profile: " + profile);
			generatedProfiles.add(profile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
