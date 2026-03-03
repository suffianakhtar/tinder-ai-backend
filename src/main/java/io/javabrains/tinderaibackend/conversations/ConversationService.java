package io.javabrains.tinderaibackend.conversations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.javabrains.tinderaibackend.profiles.Profile;

@Service
public class ConversationService {
	private final OllamaChatModel ollamaChatModel;
	
	@Autowired
	public ConversationService(OllamaChatModel ollamaChatModel) {
		this.ollamaChatModel = ollamaChatModel;
	}

	public Conversation generateProfileResponce(Conversation conversation, Profile profile, Profile user) {
		// Types of messages:
		// System message - instructions for the model
		StringBuilder systemMessageBuilder = new StringBuilder();
		systemMessageBuilder.append("You are a ").append(profile.age());
		systemMessageBuilder.append(" years old ").append(profile.ethnicity()).append("\\").append(profile.gender());
		systemMessageBuilder.append(" called ").append(profile.firstName()).append(" ").append(profile.lastName());
		systemMessageBuilder.append(" \nmatched with a ").append(user.age()).append(" old ");
		systemMessageBuilder.append(user.ethnicity()).append("\\").append(user.gender());
		systemMessageBuilder.append(" called ").append(user.firstName()).append(" ").append(user.lastName());
		systemMessageBuilder.append(" on Tinder. \nThis is an in-app text conversation between you two.");
		systemMessageBuilder.append(" Pretend to be the provided person and respond to the conversation as if writing on Tinder.");
		systemMessageBuilder.append(" \nYour bio is: ").append(profile.bio()); 
		systemMessageBuilder.append(" and your Myers-Briggs personality type is ").append(profile.myersBriggsPersonalityType()); 
		systemMessageBuilder.append(" Respond in the role of this person only.");
		systemMessageBuilder.append(" \nDo not use hashtags. Only respond with user's text. Keep the resposne brief.");
		
		SystemMessage systemMessage = new SystemMessage(systemMessageBuilder.toString());
		
		List<Message> conversationMessages =
		        conversation.messages()
		                .stream()
		                .<Message>map(message ->
		                        message.authorId().equals(profile.id())
		                                ? new AssistantMessage(message.messageText())
		                                : new UserMessage(message.messageText())
		                )
		                .toList();
		
		List<Message> allMessages = new ArrayList<>();
		allMessages.add(systemMessage);
		allMessages.addAll(conversationMessages);
		
		Prompt prompt = new Prompt(allMessages);
		ChatResponse response = ollamaChatModel.call(prompt);
        conversation.messages().add(new ChatMessage(
        	response.getResult().getOutput().getText(),
        	profile.id(),
        	LocalDateTime.now()
        ));
		return conversation;
	}
}
