package dev.combi.chunklockutils.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.file.Files;
import java.nio.file.Path;

public final class CluConfig {
	private static final Path PATH = Path.of("config", "chunklockutils.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static CluConfig INSTANCE;

	public boolean showEvolvedProgressBar = true;

	private CluConfig() {}

	public static CluConfig get() {
		if (INSTANCE == null) load();
		return INSTANCE;
	}

	public static void load() {
		try {
			if (Files.exists(PATH)) {
				INSTANCE = GSON.fromJson(Files.readString(PATH), CluConfig.class);
			}
		} catch (Exception ignored) {}
		if (INSTANCE == null) INSTANCE = new CluConfig();
	}

	public static void save() {
		try {
			Files.createDirectories(PATH.getParent());
			Files.writeString(PATH, GSON.toJson(INSTANCE));
		} catch (Exception ignored) {}
	}
}
