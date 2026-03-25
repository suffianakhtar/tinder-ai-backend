package io.javabrains.tinderaibackend;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class AppConfig {

	@Bean
	public RestTemplate restTemplate() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(10_000);
		factory.setReadTimeout(600_000);
	    RestTemplate restTemplate = new RestTemplate(factory);
	    
	    // Bypass ngrok browser warning page
	    restTemplate.getInterceptors().add((request, body, execution) -> {
	        request.getHeaders().add("ngrok-skip-browser-warning", "true");
	        return execution.execute(request, body);
	    });
	    
	    return restTemplate;
	}
	
	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
}
