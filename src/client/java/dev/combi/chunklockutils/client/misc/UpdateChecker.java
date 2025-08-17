package dev.combi.chunklockutils.client.misc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class UpdateChecker {
	private static boolean notifiedThisSession = false;

	public static void checkAsync(MinecraftClient client, String modId, String owner, String repo) {
		if (notifiedThisSession) return;

		String current = FabricLoader.getInstance()
				.getModContainer(modId).orElseThrow()
				.getMetadata().getVersion().getFriendlyString();

		var http = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(6))
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();

		var req = HttpRequest.newBuilder()
				.uri(URI.create("https://api.github.com/repos/" + owner + "/" + repo + "/releases/latest"))
				.timeout(Duration.ofSeconds(6))
				.header("User-Agent", modId + "-update-checker")
				.header("Accept", "application/vnd.github+json")
				.build();

		http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
				.thenApply(resp -> resp.statusCode() == 200 ? parseLatest(resp.body())
						: followLatestRedirect(http, owner, repo))
				.exceptionally(ex -> null)
				.thenAccept(latest -> {
					if (latest == null) return;

					String latestTag = stripV(latest.tag);
					String currentTag = stripV(current);

					if (isNewer(latestTag, currentTag)) {
						client.execute(() -> notify(client, modId, latestTag, latest.url));
						notifiedThisSession = true; // <- only once per run
					}
				});
	}

	private static record Latest(String tag, String url) {}

	private static Latest parseLatest(String body) {
		JsonObject o = JsonParser.parseString(body).getAsJsonObject();
		return new Latest(o.get("tag_name").getAsString(), o.get("html_url").getAsString());
	}

	private static Latest followLatestRedirect(HttpClient http, String owner, String repo) {
		try {
			var r = HttpRequest.newBuilder()
					.uri(URI.create("https://github.com/" + owner + "/" + repo + "/releases/latest"))
					.timeout(Duration.ofSeconds(6))
					.header("User-Agent", repo + "-update-checker")
					.method("HEAD", HttpRequest.BodyPublishers.noBody())
					.build();
			var resp = http.send(r, HttpResponse.BodyHandlers.discarding());
			var uri = resp.uri().toString();
			int idx = uri.lastIndexOf("/tag/");
			if (idx > 0) {
				String tag = uri.substring(idx + 5);
				return new Latest(tag, "https://github.com/" + owner + "/" + repo + "/releases/tag/" + tag);
			}
		} catch (Exception ignored) {}
		return null;
	}

	private static void notify(MinecraftClient client, String modId, String latest, String url) {
		if (client.player == null) return;
		var base = Text.literal("[" + modId + "] Update available: v" + latest).styled(s -> s.withColor(0x55FF55));
		var link = Text.literal("  [Open]").styled(s -> s.withUnderline(true).withColor(0x00AAFF)
				.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(url))));
		client.player.sendMessage(base.copy().append(link), false);

		SystemToast.add(client.getToastManager(), SystemToast.Type.PERIODIC_NOTIFICATION,
				Text.literal(modId + " update"), Text.literal("New version: v" + latest));
	}

	private static String stripV(String s) {
		return s != null && (s.startsWith("v") || s.startsWith("V")) ? s.substring(1) : s;
	}

	// very lenient semver-ish compare (1.10.0 > 1.2.9)
	private static boolean isNewer(String latest, String current) {
		int[] a = split(latest), b = split(current);
		for (int i = 0; i < Math.max(a.length, b.length); i++) {
			int ai = i < a.length ? a[i] : 0, bi = i < b.length ? b[i] : 0;
			if (ai != bi) return ai > bi;
		}
		return false;
	}
	private static int[] split(String v) {
		if (v == null || v.isEmpty()) return new int[0];
		return java.util.Arrays.stream(v.split("[^0-9]+"))
				.filter(s -> !s.isEmpty())
				.mapToInt(s -> { try { return Integer.parseInt(s); } catch (Exception e) { return 0; } })
				.toArray();
	}
}
