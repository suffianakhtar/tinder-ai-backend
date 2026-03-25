package io.javabrains.tinderaibackend.common.comfyui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ComfyUIService {

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	@Value("${comfyui.base-url}")
	private String COMFY_URL;
	private static final String CLIENT_ID = UUID.randomUUID().toString();

	@Autowired
	public ComfyUIService(RestTemplate restTemplate, ObjectMapper objectMapper) {
		this.restTemplate = restTemplate;
		this.objectMapper = objectMapper;
	}

	public byte[] generateImage(ComfyUIRequest request) throws Exception {

		String promptId = submitJob(request);
		String filename = pollForResult(promptId);

		return downloadImage(filename);
	}

	private String submitJob(ComfyUIRequest request) throws Exception {
		String comfyPrompt = """
				{
				  "3": {
				    "class_type": "KSampler",
				    "inputs": {
				      "seed": %d,
				      "steps": %d,
				      "cfg": %s,
				      "sampler_name": "%s",
				      "scheduler": "normal",
				      "denoise": 1,
				      "model":        ["4", 0],
				      "positive":     ["6", 0],
				      "negative":     ["7", 0],
				      "latent_image": ["5", 0]
				    }
				  },
				  "4": {
				    "class_type": "CheckpointLoaderSimple",
				    "inputs": {
				      "ckpt_name": "realisticVisionV60B1_v51HyperVAE.safetensors"
				    }
				  },
				  "5": {
				    "class_type": "EmptyLatentImage",
				    "inputs": {
				      "width": %d,
				      "height": %d,
				      "batch_size": 1
				    }
				  },
				  "6": {
				    "class_type": "CLIPTextEncode",
				    "inputs": { "text": "%s", "clip": ["4", 1] }
				  },
				  "7": {
				    "class_type": "CLIPTextEncode",
				    "inputs": { "text": "%s", "clip": ["4", 1] }
				  },
				  "8": {
				    "class_type": "VAEDecode",
				    "inputs": { "samples": ["3", 0], "vae": ["4", 2] }
				  },
				  "9": {
				    "class_type": "SaveImage",
				    "inputs": { "filename_prefix": "tinder_ai", "images": ["8", 0] }
				  }
				}
				""".formatted(new Random().nextLong(999999999), request.steps(), request.cfg(), request.samplerName(),
				request.width(), request.height(), request.positivePrompt(), request.negativePrompt());

		Map<String, Object> body = new HashMap<>();
		body.put("prompt", objectMapper.readValue(comfyPrompt, Map.class));
		body.put("client_id", CLIENT_ID);

		ResponseEntity<Map> response = restTemplate.postForEntity(COMFY_URL + "/prompt", body, Map.class);

		String promptId = (String) response.getBody().get("prompt_id");
		return promptId;
	}

	private String pollForResult(String promptId) throws Exception {
		System.out.println(" Polling for result of image generation request ... ");

		for (int i = 0; i < 300; i++) {
			Thread.sleep(3000);

			ResponseEntity<Map> response = restTemplate.getForEntity(COMFY_URL + "/history/" + promptId, Map.class);

			Map body = response.getBody();

			if (body == null || !body.containsKey(promptId)) {
		        if (i >= 100) { 
		            System.out.println("Not ready yet... attempt " + (i + 1));
		        }
				continue;
			}

			Map promptData = (Map) body.get(promptId);
			Map outputs = (Map) promptData.get("outputs");
			Map node9 = (Map) outputs.get("9");
			List images = (List) node9.get("images");
			Map imageInfo = (Map) images.get(0);

			String filename = (String) imageInfo.get("filename");
			System.out.println("Image ready: " + filename);
			return filename;
		}
		throw new RuntimeException("Timed out waiting for ComfyUI");
	}

	private byte[] downloadImage(String filename) {
		String url = COMFY_URL + "/view?filename=" + filename + "&type=output";
		System.out.println("Downloading: " + url);
		return restTemplate.getForObject(url, byte[].class);
	}
}
