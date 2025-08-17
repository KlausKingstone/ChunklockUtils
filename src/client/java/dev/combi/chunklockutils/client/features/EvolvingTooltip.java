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
		// Only target Evolving Tools
		if (!stack.getName().getString().toLowerCase(Locale.ROOT).contains("evolving")) return;

		Optional<Double> scalable = getScalableValue(stack);
		if (scalable.isEmpty()) return;

		String newVal = formatNumber(scalable.get());

		// Replace line immediately after a "LEVEL UP" line
		for (int i = 1; i < lines.size(); i++) {
			String prev = lines.get(i - 1).getString().trim();
			if (!prev.contains("LEVEL UP")) continue;

			Text line = lines.get(i);
			String plain = line.getString();
			int slash = plain.indexOf('/');
			if (slash > 0) {
				int end = slash;
				int pos = end - 1;
				while (pos >= 0) {
					char c = plain.charAt(pos);
					if (Character.isDigit(c) || c == '.' || c == ',') pos--;
					else break;
				}
				int start = pos + 1;
				if (start < end) {
					String oldNum = plain.substring(start, end);
					lines.set(i, TextReplace.replaceFirstPreservingStyle(line, oldNum, newVal));
				}
			}
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
}