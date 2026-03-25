package io.javabrains.tinderaibackend.common.comfyui;

public record ComfyUIRequest(String positivePrompt, String negativePrompt, int steps, String samplerName, int width,
		int height, double cfg) {
	// Default values — use this for quick calls
	public static ComfyUIRequest withDefaults(String positivePrompt) {
		return new ComfyUIRequest(positivePrompt, "ugly, blurry, deformed, low quality, watermark, text", 6, "lcm",
				512, 512, 8.0);
	}
}