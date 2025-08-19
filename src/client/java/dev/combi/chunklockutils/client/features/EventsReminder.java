package dev.combi.chunklockutils.client.features;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.combi.chunklockutils.client.config.ConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public final class EventsReminder {
	// EST, fixed offset (no DST)
	private static final ZoneId ZONE_EST = ZoneOffset.ofHours(-5);

	private static final long PERIOD_MS = Duration.ofHours(3).toMillis(); // 3h block
	private static final int  CRAB_MINUTE  = 4;   // hh:04
	private static final int  ENVOY_MINUTE = 19;  // hh:19

	// Persistence
	private static final Path SAVE = Path.of("config", "chunklockutils-events.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static long lastCrabCycle  = Long.MIN_VALUE;
	private static long lastEnvoyCycle = Long.MIN_VALUE;
	private static long lastChatEpochMinute = Long.MIN_VALUE;

	// Throttle
	private static long nextCheckAtMs = 0L;
	private static final long CHECK_EVERY_MS = 250L;

	private EventsReminder() {}

	public static void init() {
		load();

		ClientPlayConnectionEvents.JOIN.register((h, s, client) -> primeCurrentCycle());
		ClientPlayConnectionEvents.DISCONNECT.register((h, client) -> save());

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) return;
			if (!ConfigManager.get().notifyCrabRave && !ConfigManager.get().notifyEnvoy) return;

			long now = System.currentTimeMillis();
			if (now < nextCheckAtMs) return;
			nextCheckAtMs = now + CHECK_EVERY_MS;

			CyclePos pos = currentCyclePos(now);

			if (ConfigManager.get().notifyCrabRave) checkCrabRave(client, pos);
			if (ConfigManager.get().notifyEnvoy)    checkEnvoy(client, pos);
			if (ConfigManager.get().notifyChatEvents) checkChatEvents(client, now);
		});
	}

	public static void checkCrabRave(MinecraftClient client, CyclePos pos) {
		if (pos.minuteInCycle == CRAB_MINUTE && lastCrabCycle != pos.absCycle) {
			lastCrabCycle = pos.absCycle;
			notify(client, "Crab Rave starting in 1 minute!");
			save();
		}
	}

	public static void checkEnvoy(MinecraftClient client, CyclePos pos) {
		if (pos.minuteInCycle == ENVOY_MINUTE && lastEnvoyCycle != pos.absCycle) {
			lastEnvoyCycle = pos.absCycle;
			notify(client, "Envoy starting in 1 minute!");
			save();
		}
	}

	public static void checkChatEvents(MinecraftClient client, long nowMs) {
		var zdt = Instant.ofEpochMilli(nowMs).atZone(ZONE_EST);
		int minute  = zdt.getMinute();
		int second  = zdt.getSecond();
		long epochMinute = zdt.toEpochSecond() / 60L;

		boolean isMarkMinute = ((minute + 1) % 15) == 0;

		if (isMarkMinute && second >= 50 && lastChatEpochMinute != epochMinute) {
			lastChatEpochMinute = epochMinute;
			notify(client, "Chat Event starting in 10 seconds!");
			save();
		}
	}

	private static CyclePos currentCyclePos(long nowEpochMs) {
		var nowEst = Instant.ofEpochMilli(nowEpochMs).atZone(ZONE_EST);

		var midnight = nowEst.toLocalDate().atStartOfDay(ZONE_EST);

		long sinceMidnightMs = Duration.between(midnight.toInstant(), nowEst.toInstant()).toMillis();
		long cycleInDay      = Math.floorDiv(sinceMidnightMs, PERIOD_MS);       // 0..7
		long remMs           = Math.floorMod(sinceMidnightMs, PERIOD_MS);
		int  minuteInCycle   = (int) Math.floorDiv(remMs, 60_000L);             // 0..179

		long absCycle = nowEst.toLocalDate().toEpochDay() * 8L + cycleInDay;

		return new CyclePos(absCycle, minuteInCycle);
	}

	private static void primeCurrentCycle() {
		long now = System.currentTimeMillis();
		var zdt = Instant.ofEpochMilli(now).atZone(ZONE_EST);

		CyclePos pos = currentCyclePos(now);
		if (pos.minuteInCycle > CRAB_MINUTE)  lastCrabCycle  = pos.absCycle;
		if (pos.minuteInCycle > ENVOY_MINUTE) lastEnvoyCycle = pos.absCycle;

		int minute = zdt.getMinute();
		int second = zdt.getSecond();
		boolean isMarkMinute = ((minute + 1) % 15) == 0;
		if (isMarkMinute && second >= 50) {
			lastChatEpochMinute = zdt.toEpochSecond() / 60L;
		}
		save();
	}

	private static void notify(MinecraftClient client, String msg) {
		if (client.player == null) return;
		client.player.sendMessage(
				Text.empty()
						.append(Text.literal("["))
						.append(Text.literal("ChunklockUtils")
								.styled(s -> s.withBold(true).withColor(0x00FFC8)))
						.append(Text.literal("] "))
						.append(Text.literal(msg).styled(s -> s.withColor(0xAAAAAA))),
				false
		);
		client.execute(() -> client.player.playSound(SoundEvents.BLOCK_BELL_USE, 1f, 1f));
	}

	private static void load() {
		try {
			if (Files.exists(SAVE)) {
				State st = GSON.fromJson(Files.readString(SAVE), State.class);
				if (st != null) { lastCrabCycle = st.lastCrab; lastEnvoyCycle = st.lastEnvoy; }
			}
		} catch (Exception ignored) {}
	}
	private static void save() {
		try {
			Files.createDirectories(SAVE.getParent());
			Files.writeString(SAVE, GSON.toJson(new State(lastCrabCycle, lastEnvoyCycle)));
		} catch (Exception ignored) {}
	}

	public record CyclePos(long absCycle, int minuteInCycle) {}
	private record State(long lastCrab, long lastEnvoy) {}
}
