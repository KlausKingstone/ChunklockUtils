package dev.combi.chunklockutils.client.features;

import dev.combi.chunklockutils.client.util.TextReplace;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class EvolvingTooltip {
	public static void rewriteTooltip(ItemStack stack, List<Text> lines) {
		// Only check Evolving tools
		if (!stack.getName().getString().toLowerCase(Locale.ROOT).contains("evolving")) return;

		Optional<Double> scalableOpt = getScalableValue(stack);
		if (scalableOpt.isEmpty()) return;
		double scalable = scalableOpt.get();

		for (int i = 1; i < lines.size(); i++) {
			String prev = lines.get(i - 1).getString().trim();
			if (!prev.contains("LEVEL UP")) continue;

			Text line = lines.get(i);
			String plain = line.getString();

			int slash = plain.indexOf('/');
			if (slash <= 0) continue;

			int firstEnd = slash;
			int p = firstEnd - 1;
			while (p >= 0) {
				char c = plain.charAt(p);
				if (Character.isDigit(c) || c == '.' || c == ',') p--;
				else break;
			}
			int firstStart = p + 1;
			if (firstStart >= firstEnd) continue;
			String firstNumOld = plain.substring(firstStart, firstEnd);

			String firstNew = formatNumber(scalable);

			String oldNum = plain.substring(firstStart, firstEnd);
			lines.set(i, TextReplace.replaceFirstPreservingStyle(line, oldNum, firstNew));
			return;
		}
	}

	// Get progress value
	private static Optional<Double> getScalableValue(ItemStack stack) {
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

	private static Double readNumberLike(NbtElement el) {
		if (el == null) return null;
		if (el instanceof NbtDouble d) return d.doubleValue();
		if (el instanceof NbtFloat f)  return (double) f.floatValue();
		if (el instanceof NbtInt i)    return (double) i.intValue();
		if (el instanceof NbtLong l)   return (double) l.longValue();
		if (el instanceof NbtShort s)  return (double) s.shortValue();
		if (el instanceof NbtByte b)   return (double) b.byteValue();
		if (el instanceof NbtString s) {
			try { return Double.parseDouble(s.asString()); } catch (NumberFormatException ignored) {}
		}
		if (el instanceof NbtCompound c) {
			return readNumberLike(c.get("value"));
		}
		return null;
	}

	private static String formatNumber(double v) {
		String s = String.format(Locale.US, "%.2f", v);
		if (s.contains(".")) s = s.replaceAll("\\.?0+$", "");
		return s;
	}

	private static double parseNumber(String s) {
		// tolerate "1,234" and "1234"
		String t = s.replace(",", "").trim();
		try { return Double.parseDouble(t); } catch (Exception e) { return 0; }
	}

	private static double clamp(double v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }
}