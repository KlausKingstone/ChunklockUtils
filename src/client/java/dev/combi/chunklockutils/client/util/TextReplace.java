package dev.combi.chunklockutils.client.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class TextReplace {

	private record Run(Style style, String s) {}

	private TextReplace() {}

	// Replace first occurrence of target, preserving styles
	public static Text replaceFirstPreservingStyle(Text input, String target, String repl) {
		if (target == null || target.isEmpty()) return input;

		List<Run> runs = flattenByOrdered(input);
		if (runs.isEmpty()) return input;

		StringBuilder all = new StringBuilder();
		for (Run r : runs) all.append(r.s);
		int start = all.indexOf(target);
		if (start < 0) return input;
		int end = start + target.length();

		MutableText out = Text.empty();
		out.setStyle(input.getStyle());

		int cursor = 0;
		boolean inserted = false;

		for (Run r : runs) {
			int segStart = cursor;
			int segEnd   = cursor + r.s.length();

			int preLen = Math.max(0, Math.min(r.s.length(), start - segStart));
			if (preLen > 0) {
				out.append(Text.literal(r.s.substring(0, preLen)).setStyle(r.style()));
			}

			boolean overlaps = segEnd > start && segStart < end;
			if (overlaps && !inserted) {
				out.append(Text.literal(repl).setStyle(r.style())); // Keep styles
				inserted = true;
			}

			int postFrom = Math.max(0, end - segStart);
			if (postFrom < r.s.length()) {
				out.append(Text.literal(r.s.substring(postFrom)).setStyle(r.style()));
			}

			cursor = segEnd;
		}
		return out;
	}

	private static List<Run> flattenByOrdered(Text t) {
		List<Run> runs = new ArrayList<>();
		StringBuilder buf = new StringBuilder();
		Style[] cur = { Style.EMPTY };

		OrderedText ordered = t.asOrderedText();
		ordered.accept((i, style, cp) -> {
			if (!style.equals(cur[0]) && buf.length() > 0) {
				runs.add(new Run(cur[0], buf.toString()));
				buf.setLength(0);
			}
			cur[0] = style;
			buf.appendCodePoint(cp);
			return true;
		});
		if (buf.length() > 0) runs.add(new Run(cur[0], buf.toString()));
		return runs;
	}
}