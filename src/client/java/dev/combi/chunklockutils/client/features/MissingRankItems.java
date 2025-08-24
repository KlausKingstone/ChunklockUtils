package dev.combi.chunklockutils.client.features;

import com.mojang.serialization.DataResult;
import dev.combi.chunklockutils.client.config.ConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MissingRankItems {

	// ---------- Behavior & UI config ----------
	private static final String HEADER_TEXT   = "Rankup-Items left:";
	private static final int BG_COLOR         = 0x55000000; // less opaque
	private static final int TEXT_WHITE       = 0xFFFFFFFF;
	private static final int TEXT_YELLOW      = 0xFFFFFF55;
	private static final int TEXT_DIM         = 0xFFAAAAAA;

	private static final int BASE_Y_OFFSET    = 28;
	private static final String START_TRIGGER = "Teleporting to spawn";

	private static final int TRIGGER_SLOT = 9;
	private static final int DUMP_FROM   = 13;
	private static final int DUMP_TO     = 26;
	private static final Pattern PROGRESSION = Pattern.compile("(?i)\\bprogression:\\s*(\\d+)\\s*/\\s*(\\d+)");
	private static final int CHECK_EVERY_TICKS = 10;

	// ---------- State ----------
	private static Screen lastScreen = null;
	private static String lastSignature = null;
	private static int tickCounter = 0;

	private static final List<Entry> currentEntries = new ArrayList<>();
	private static volatile boolean RENDER_ACTIVATED = false;
	private static volatile boolean USER_ENABLED = true;

	public static void setUserToggle(boolean enabled) { USER_ENABLED = enabled; }

	// ---------- Persistence ----------
	private static final Path CONFIG_DIR = Paths.get("config", "chunklockutils");
	private static final Path SAVE_FILE  = CONFIG_DIR.resolve("rank_progression.json");

	private MissingRankItems() {}

	public static void register() {
		Storage.loadInto(currentEntries);

		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (message != null && message.getString().contains(START_TRIGGER)) {
				RENDER_ACTIVATED = true;
			}
		});

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			synchronized (currentEntries) {
				for (Entry e : currentEntries) e.ensureDecoded();
			}
		});

		// Render overlay
		HudRenderCallback.EVENT.register(MissingRankItems::drawHud);

		// Continuous scan
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (++tickCounter % CHECK_EVERY_TICKS != 0) return;

			final Screen current = MinecraftClient.getInstance().currentScreen;

			if (!Objects.equals(current, lastScreen)) {
				lastScreen = current;
				lastSignature = null;
			}

			if (!(current instanceof HandledScreen<?> handled)) return;
			if (TRIGGER_SLOT >= handled.getScreenHandler().slots.size()) return;

			ItemStack trigger = handled.getScreenHandler().getSlot(TRIGGER_SLOT).getStack();
			if (trigger.isEmpty() || !"Money".equals(trigger.getName().getString())) {
				lastSignature = null;
				return;
			}

			List<Entry> newEntries = new ArrayList<>();
			List<String> sigParts = new ArrayList<>();

			for (int i = DUMP_FROM; i <= DUMP_TO; i++) {
				if (i >= handled.getScreenHandler().slots.size()) break;

				ItemStack stack = handled.getScreenHandler().getSlot(i).getStack();
				if (stack.isEmpty()) continue;

				String name = stack.getName().getString();
				int left = readLeftFromLore(stack);
				newEntries.add(Entry.createFromLiveStack(stack.copy(), name, left));

				sigParts.add((i + 1) + ":" + name + ":" + left);
			}

			String signature = String.join("|", sigParts);
			if (!signature.equals(lastSignature)) {
				lastSignature = signature;

				synchronized (currentEntries) {
					currentEntries.clear();
					currentEntries.addAll(newEntries);
				}

				Storage.saveFrom(currentEntries);

				for (Entry e : newEntries) {
					String tail = (e.left >= 0) ? (e.left + " left") : "no progression";
					System.out.println("  - " + e.name + " → " + tail);
				}
			}
		});
	}

	private static int readLeftFromLore(ItemStack stack) {
		try {
			LoreComponent lore = stack.get(DataComponentTypes.LORE);
			if (lore != null) {
				for (Text t : lore.lines()) {
					Matcher m = PROGRESSION.matcher(t.getString());
					if (m.find()) {
						int x = Integer.parseInt(m.group(1));
						int y = Integer.parseInt(m.group(2));
						return Math.max(0, y - x);
					}
				}
			}
		} catch (Throwable ignored) {}
		return -1;
	}

	// ---------- HUD ----------
	private static void drawHud(DrawContext ctx, RenderTickCounter tickDelta) {
		var client = MinecraftClient.getInstance();
		if (client.options.hudHidden) return;
		if (!RENDER_ACTIVATED || !USER_ENABLED || !ConfigManager.get().showRankMenu) return;

		List<Entry> snapshot;
		synchronized (currentEntries) {
			if (currentEntries.isEmpty()) return;
			snapshot = new ArrayList<>(currentEntries);
		}

		for (Entry e : snapshot) e.ensureDecoded();

		snapshot.removeIf(e -> e.left == 0 || e.stack.isEmpty());
		if (snapshot.isEmpty()) return;

		var tr = client.textRenderer;

		final int pad = 2;
		final int iconSize = 16;
		final int lineH = 17;
		final int textOffsetX = iconSize + 4;

		int maxRowWidth = tr.getWidth(HEADER_TEXT);
		for (Entry e : snapshot) {
			String nameDash = e.name + " - ";
			String leftStr  = formatNumber(e.left);
			int w = textOffsetX + tr.getWidth(nameDash) + tr.getWidth(leftStr);
			if (w > maxRowWidth) maxRowWidth = w;
		}

		int screenW = ctx.getScaledWindowWidth();
		int xRight  = screenW - pad;

		int headerH = tr.fontHeight;
		int totalH  = pad + headerH + 2 + snapshot.size() * lineH + pad;
		int totalW  = maxRowWidth + pad * 2;

		int bgX0 = xRight - totalW;
		int bgY0 = BASE_Y_OFFSET;
		int bgX1 = xRight;
		int bgY1 = bgY0 + totalH;

		// Background
		ctx.fill(bgX0, bgY0, bgX1, bgY1, BG_COLOR);

		// Header
		int headerX = bgX0 + pad;
		int headerY = bgY0 + pad;
		ctx.drawText(tr, HEADER_TEXT, headerX, headerY, TEXT_WHITE, false);

		// Separator
		int sepY = headerY + headerH + 1;
		ctx.fill(bgX0 + pad, sepY, bgX1 - pad, sepY + 1, 0x66FFFFFF);

		// Rows
		int y = sepY + 2;
		for (Entry e : snapshot) {
			int iconX = bgX0 + pad;
			int textX = iconX + textOffsetX;

			// Icon
			ctx.drawItem(e.stack, iconX, y);

			String nameDash = e.name + " - ";
			String leftStr  = formatNumber(e.left);

			int baseY = y + (iconSize - tr.fontHeight) / 2;
			ctx.drawText(tr, nameDash, textX, baseY, TEXT_WHITE, false);
			int numX = textX + tr.getWidth(nameDash);
			ctx.drawText(tr, leftStr, numX, baseY, TEXT_YELLOW, false);

			y += lineH;
		}
	}

	private static final DecimalFormat THOUSAND_FORMAT;
	static {
		DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
		symbols.setGroupingSeparator('.');
		THOUSAND_FORMAT = new DecimalFormat("#,###", symbols);
	}

	private static String formatNumber(int left) {
		return THOUSAND_FORMAT.format(Math.max(0, left));
	}

	// ---------- Registry helpers ----------
	private static RegistryWrapper.WrapperLookup regs() {
		var mc = MinecraftClient.getInstance();
		if (mc.world != null) return mc.world.getRegistryManager();
		if (mc.getNetworkHandler() != null) return mc.getNetworkHandler().getRegistryManager();
		return null;
	}

	// ---------- ItemStack codec helpers (1.21.4) ----------
	private static NbtCompound encodeStack(ItemStack stack, RegistryWrapper.WrapperLookup lookup) {
		RegistryOps<NbtElement> ops = RegistryOps.of(NbtOps.INSTANCE, lookup);
		DataResult<NbtElement> res = ItemStack.CODEC.encodeStart(ops, stack);
		return res.result()
				.filter(tag -> tag instanceof NbtCompound)
				.map(tag -> (NbtCompound) tag)
				.orElse(null);
	}

	private static ItemStack decodeStack(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
		RegistryOps<NbtElement> ops = RegistryOps.of(NbtOps.INSTANCE, lookup);
		return ItemStack.CODEC.parse(ops, tag).result().orElse(ItemStack.EMPTY);
	}

	// ---------- Model ----------
	private static final class Entry {
		ItemStack stack;
		final String name;
		final int left;
		final NbtCompound rawStackTag;

		private Entry(ItemStack stack, String name, int left, NbtCompound rawTag) {
			this.stack = stack;
			this.name = name;
			this.left = left;
			this.rawStackTag = rawTag;
		}

		static Entry createFromLiveStack(ItemStack stack, String name, int left) {
			return new Entry(stack, name, left, null);
		}

		static Entry createDeferred(String name, int left, NbtCompound rawTag) {
			return new Entry(ItemStack.EMPTY, name, left, rawTag);
		}

		void ensureDecoded() {
			if (!this.stack.isEmpty()) return;
			if (this.rawStackTag == null) return;
			var lookup = regs();
			if (lookup == null) return;
			this.stack = decodeStack(this.rawStackTag, lookup);
		}

		NbtCompound toNbt() {
			NbtCompound root = new NbtCompound();
			root.putString("name", name);
			root.putInt("left", left);

			var lookup = regs();
			if (lookup != null) {
				NbtCompound stackTag = encodeStack(stack, lookup);
				if (stackTag != null) root.put("stack", stackTag);
			} else if (rawStackTag != null) {
				root.put("stack", rawStackTag);
			}
			return root;
		}

		static Entry fromNbt(NbtCompound nbt) {
			String nm = nbt.getString("name");
			int lf = nbt.contains("left") ? nbt.getInt("left") : -1;
			NbtCompound tag = nbt.contains("stack", NbtElement.COMPOUND_TYPE) ? nbt.getCompound("stack") : null;

			var lookup = regs();
			if (lookup != null && tag != null) {
				ItemStack st = decodeStack(tag, lookup);
				return createFromLiveStack(st, nm, lf);
			} else {
				return createDeferred(nm, lf, tag);
			}
		}
	}

	// ---------- Persistence ----------
	private static final class Storage {
		static void saveFrom(List<Entry> entries) {
			try {
				Files.createDirectories(CONFIG_DIR);

				List<String> snbtEntries = new ArrayList<>();
				for (Entry e : entries) {
					snbtEntries.add(e.toNbt().toString());
				}

				String json = toJsonArray(snbtEntries);
				try (BufferedWriter w = Files.newBufferedWriter(SAVE_FILE, StandardCharsets.UTF_8,
						StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
					w.write(json);
				}
			} catch (IOException ex) {
				System.out.println("[ChunklockUtils] RankProgression save failed: " + ex.getMessage());
			}
		}

		static void loadInto(List<Entry> target) {
			if (!Files.exists(SAVE_FILE)) return;
			try (BufferedReader r = Files.newBufferedReader(SAVE_FILE, StandardCharsets.UTF_8)) {
				String content = r.lines().reduce("", (a, b) -> a + b);
				List<String> snbtList = parseJsonArray(content);

				List<Entry> loaded = new ArrayList<>();
				for (String snbt : snbtList) {
					try {
						NbtCompound nbt = StringNbtReader.parse(snbt);
						loaded.add(Entry.fromNbt(nbt));
					} catch (Exception ignoreOne) {}
				}

				synchronized (target) {
					target.clear();
					target.addAll(loaded);
				}
			} catch (Exception ex) {
				System.out.println("[ChunklockUtils] RankProgression load failed: " + ex.getMessage());
			}
		}

		private static String toJsonArray(List<String> items) {
			StringBuilder sb = new StringBuilder();
			sb.append('[');
			for (int i = 0; i < items.size(); i++) {
				if (i > 0) sb.append(',');
				sb.append('"').append(escape(items.get(i))).append('"');
			}
			sb.append(']');
			return sb.toString();
		}

		private static List<String> parseJsonArray(String json) {
			List<String> out = new ArrayList<>();
			if (json == null) return out;
			int i = 0, n = json.length();
			while (i < n && Character.isWhitespace(json.charAt(i))) i++;
			if (i >= n || json.charAt(i++) != '[') return out;

			while (i < n) {
				while (i < n && Character.isWhitespace(json.charAt(i))) i++;
				if (i < n && json.charAt(i) == ']') { i++; break; }
				if (i >= n || json.charAt(i++) != '"') break;

				StringBuilder s = new StringBuilder();
				while (i < n) {
					char c = json.charAt(i++);
					if (c == '\\') {
						if (i < n) {
							char e = json.charAt(i++);
							switch (e) {
								case '\\': case '"': case '/': s.append(e); break;
								case 'b': s.append('\b'); break;
								case 'f': s.append('\f'); break;
								case 'n': s.append('\n'); break;
								case 'r': s.append('\r'); break;
								case 't': s.append('\t'); break;
								case 'u':
									if (i + 4 <= n) {
										String hex = json.substring(i, i + 4);
										s.append((char) Integer.parseInt(hex, 16));
										i += 4;
									}
									break;
								default: s.append(e);
							}
						}
					} else if (c == '"') {
						break;
					} else {
						s.append(c);
					}
				}
				out.add(s.toString());

				while (i < n && Character.isWhitespace(json.charAt(i))) i++;
				if (i < n && json.charAt(i) == ',') i++;
			}
			return out;
		}

		private static String escape(String s) {
			return s.replace("\\", "\\\\")
					.replace("\"", "\\\"")
					.replace("\n", "\\n")
					.replace("\r", "\\r")
					.replace("\t", "\\t");
		}
	}
}
