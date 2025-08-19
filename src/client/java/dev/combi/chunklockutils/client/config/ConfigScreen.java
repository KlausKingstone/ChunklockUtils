package dev.combi.chunklockutils.client.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {
	private final Screen parent;
	private ButtonWidget evotoggleBtn;
	private ButtonWidget smallHandBtn;  // NEW
	private ButtonWidget crabBtn;
	private ButtonWidget envoyBtn;
	private ButtonWidget chatBtn;
	private ButtonWidget eventsHeader;
	private ButtonWidget doneBtn;

	public ConfigScreen(Screen parent) {
		super(Text.literal("ChunklockUtils Settings"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int cx   = this.width / 2;
		int w    = 200;
		int h    = 20;
		int gap  = 6;
		int step = h + gap;

		int top  = Math.max(24, this.height / 2 - 90);
		int y    = top;

		// --- General ---
		// Evolved tools toggle
		evotoggleBtn = ButtonWidget.builder(currentLabel(), b -> {
			var cfg = ConfigManager.get();
			cfg.showEvolvedProgressBar = !cfg.showEvolvedProgressBar;
			b.setMessage(currentLabel());
			ConfigManager.save();
		}).dimensions(cx - w/2, y, w, h).build();
		addDrawableChild(evotoggleBtn);
		y += step;

		// Small Hand
		smallHandBtn = ButtonWidget.builder(smallHandLabel(), b -> {
			var cfg = ConfigManager.get();
			cfg.smallHand = !cfg.smallHand;
			b.setMessage(smallHandLabel());
			ConfigManager.save();
		}).dimensions(cx - w/2, y, w, h).build();
		addDrawableChild(smallHandBtn);
		y += step + 6;

		// --- Event Notifications
		eventsHeader = ButtonWidget.builder(Text.literal("— Event Notifications —"), btn -> {})
				.dimensions(cx - w/2, y, w, h).build();
		eventsHeader.active = false;
		addDrawableChild(eventsHeader);
		y += step;

		// Crab Rave
		crabBtn = ButtonWidget.builder(crabLabel(), b -> {
			var cfg = ConfigManager.get();
			cfg.notifyCrabRave = !cfg.notifyCrabRave;
			b.setMessage(crabLabel());
			ConfigManager.save();
		}).dimensions(cx - w/2, y, w, h).build();
		addDrawableChild(crabBtn);
		y += step;

		// Envoy
		envoyBtn = ButtonWidget.builder(envoyLabel(), b -> {
			var cfg = ConfigManager.get();
			cfg.notifyEnvoy = !cfg.notifyEnvoy;
			b.setMessage(envoyLabel());
			ConfigManager.save();
		}).dimensions(cx - w/2, y, w, h).build();
		addDrawableChild(envoyBtn);
		y += step;

		// Chat Events
		chatBtn = ButtonWidget.builder(chatLabel(), b -> {
			var cfg = ConfigManager.get();
			cfg.notifyChatEvents = !cfg.notifyChatEvents;
			b.setMessage(chatLabel());
			ConfigManager.save();
		}).dimensions(cx - w/2, y, w, h).build();
		addDrawableChild(chatBtn);

		// Done
		int pad = 12;
		int dx = (this.width - w) / 2;
		int dy = this.height - h - pad;
		doneBtn = ButtonWidget.builder(Text.literal("Done"), b -> close())
				.dimensions(dx, dy, w, h).build();
		addDrawableChild(doneBtn);
	}

	private Text smallHandLabel() {
		return Text.literal("Small Hand: " + (ConfigManager.get().smallHand ? "ON" : "OFF"));
	}

	private Text currentLabel() {
		return Text.literal("Evolved Tools Bar: "
				+ (ConfigManager.get().showEvolvedProgressBar ? "ON" : "OFF"));
	}
	private Text crabLabel() {
		return Text.literal("Crab Rave: "
				+ (ConfigManager.get().notifyCrabRave ? "ON" : "OFF"));
	}
	private Text envoyLabel() {
		return Text.literal("Envoy: "
				+ (ConfigManager.get().notifyEnvoy ? "ON" : "OFF"));
	}

	private Text chatLabel() {
		return Text.literal("Chat Events: " + (ConfigManager.get().notifyChatEvents ? "ON" : "OFF"));
	}

	@Override
	public void close() {
		ConfigManager.save();
		MinecraftClient.getInstance().setScreen(parent);
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		this.renderBackground(ctx, mouseX, mouseY, delta);
		super.render(ctx, mouseX, mouseY, delta);
		ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
	}
}