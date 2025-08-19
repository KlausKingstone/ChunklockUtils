package dev.combi.chunklockutils.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
	private static final Path PATH = Path.of("config", "chunklockutils.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static ConfigManager INSTANCE;

	/* ==== Event Notifications ==== */
	public boolean notifyCrabRave = true;
	public boolean notifyEnvoy    = true;
	public boolean notifyChatEvents = true;

	public boolean showEvolvedProgressBar = true;

	private ConfigManager() {}

	public static ConfigManager get() {
		if (INSTANCE == null) load();
		return INSTANCE;
	}

	public static void load() {
		try {
			if (Files.exists(PATH)) {
				INSTANCE = GSON.fromJson(Files.readString(PATH), ConfigManager.class);
			}
		} catch (Exception ignored) {}
		if (INSTANCE == null) INSTANCE = new ConfigManager();
	}

	public static void save() {
		try {
			Files.createDirectories(PATH.getParent());
			Files.writeString(PATH, GSON.toJson(INSTANCE));
		} catch (Exception ignored) {}
	}
}
