package dev.combi.chunklockutils.client.features;

import dev.combi.chunklockutils.client.config.CluConfig;
import dev.combi.chunklockutils.client.mixin.HandledScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.screen.slot.Slot;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class EvolvingProgressBar {

	private EvolvingProgressBar() {}

	private static final Pattern PROGRESS = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*/\\s*(\\d[\\d,]*)");

	public static void renderGui(HandledScreen<?> screen, DrawContext ctx) {
		HandledScreenAccessor acc = (HandledScreenAccessor) screen;

		for (Slot slot : screen.getScreenHandler().slots) {
			if (!slot.hasStack()) continue;

			ItemStack stack = slot.getStack();
			if (!isEvolving(stack)) continue;

			Optional<Double> current = readScalable(stack);
			Optional<Double> target  = readTargetFromLore(stack);

			if (current.isEmpty() || target.isEmpty()) continue;

			double pct = clamp(current.get() / Math.max(1d, target.get()));

			int slotX = acc.getX() + slot.x;
			int slotY = acc.getY() + slot.y;
			drawBar(ctx, slotX, slotY, pct);
		}
	}

	public static void renderHotbar(DrawContext ctx, RenderTickCounter tickCounter) {
		if (!CluConfig.get().showEvolvedProgressBar) return;
		var mc = MinecraftClient.getInstance();
		if (mc.player == null || mc.options.hudHidden) return;

		int sel = mc.player.getInventory().selectedSlot;
		ItemStack stack = mc.player.getInventory().getStack(sel);
		if (stack.isEmpty() || !isEvolving(stack)) return;

		var current = readScalable(stack);
		var target  = readTargetFromLore(stack);
		if (current.isEmpty() || target.isEmpty()) return;

		double pct = clamp(current.get() / Math.max(1d, target.get()));

		int sw = mc.getWindow().getScaledWidth();
		int sh = mc.getWindow().getScaledHeight();
		int baseX = sw / 2 - 91;
		int baseY = sh - 22;
		int slotX = baseX + sel * 20 + 2;
		int slotY = baseY + 2;

		drawBar(ctx, slotX, slotY, pct);
	}

	private static void drawBar(DrawContext ctx, int slotX, int slotY, double pct) {
		int x = slotX + 2;
		int y = slotY + 14;
		int w = 12;
		int h = 1;

		int bg = 0x88000000;
		int fg = 0xFF66CCFF;
		int bd = 0x802F4A60;
		ctx.fill(x, y, x + w, y + h, bg);
		int pw = (int) Math.round(w * pct);
		if (pw > 0) ctx.fill(x, y, x + pw, y + h, fg);
		ctx.drawBorder(x - 1, y - 1, w + 2, h + 2, bd);
	}

	private static boolean isEvolving(ItemStack stack) {
		return stack.getName().getString().toLowerCase(Locale.ROOT).contains("evolving");
	}

	private static Optional<Double> readScalable(ItemStack stack) {
		NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
		if (custom == null) return Optional.empty();
		final NbtCompound[] ref = new NbtCompound[1];
		custom.apply(tag -> ref[0] = tag);
		NbtCompound root = ref[0];
		if (root == null) return Optional.empty();

		NbtCompound pdc = root.contains("PublicBukkitValues", NbtElement.COMPOUND_TYPE)
				? root.getCompound("PublicBukkitValues")
				: (root.contains("BukkitValues", NbtElement.COMPOUND_TYPE)
				? root.getCompound("BukkitValues") : null);
		if (pdc == null) return Optional.empty();

		return Optional.ofNullable(readNumberLike(pdc.get("items:scalable_value")));
	}

	private static Optional<Double> readTargetFromLore(ItemStack stack) {
		LoreComponent lore = stack.get(DataComponentTypes.LORE);
		if (lore == null) return Optional.empty();
		List<net.minecraft.text.Text> lines;
		try { lines = lore.lines(); }
		catch (Throwable t) {
			try { lines = lore.styledLines(); } catch (Throwable ex) { return Optional.empty(); }
		}
		for (int i = 1; i < lines.size(); i++) {
			if (lines.get(i - 1).getString().trim().contains("LEVEL UP")) {
				String s = lines.get(i).getString();
				var m = PROGRESS.matcher(s);
				if (m.find()) {
					String target = m.group(2).replace(",", "");
					try { return Optional.of(Double.parseDouble(target.substring(0, target.length() - 2))); } catch (Exception ignored) {}
				}
			}
		}
		return Optional.empty();
	}

	private static Double readNumberLike(NbtElement el) {
		if (el == null) return null;
		if (el instanceof NbtDouble d) return d.doubleValue();
		if (el instanceof NbtFloat f)  return (double) f.floatValue();
		if (el instanceof NbtInt i)    return (double) i.intValue();
		if (el instanceof NbtLong l)   return (double) l.longValue();
		if (el instanceof NbtShort s)  return (double) s.shortValue();
		if (el instanceof NbtByte b)   return (double) b.byteValue();
		if (el instanceof NbtString s) { try { return Double.parseDouble(s.asString()); } catch (Exception ignored) {} }
		if (el instanceof NbtCompound c) return readNumberLike(c.get("value"));
		return null;
	}

	private static double clamp(double v) { return v < 0 ? 0 : Math.min(1, v); }
}
