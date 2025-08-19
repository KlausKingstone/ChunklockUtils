package dev.combi.chunklockutils.client.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {
	private final Screen parent;
	private ButtonWidget evotoggleBtn;
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
		int cx = this.width / 2;
		int cy = this.height / 2;

		evotoggleBtn = ButtonWidget.builder(currentLabel(), b -> {
			ConfigManager cfg = ConfigManager.get();
			cfg.showEvolvedProgressBar = !cfg.showEvolvedProgressBar;
			b.setMessage(currentLabel());
			ConfigManager.save();
		}).dimensions(cx - 100, cy - 56, 200, 20).build();
		addDrawableChild(evotoggleBtn);

		eventsHeader = ButtonWidget.builder(Text.literal("— Event Notifications —"), btn -> {})
				.dimensions(cx - 100, cy - 28, 200, 20).build();
		eventsHeader.active = false;
		addDrawableChild(eventsHeader);

		crabBtn = ButtonWidget.builder(crabLabel(), b -> {
			ConfigManager cfg = ConfigManager.get();
			cfg.notifyCrabRave = !cfg.notifyCrabRave;
			b.setMessage(crabLabel());
			ConfigManager.save();
		}).dimensions(cx - 100, cy - 4, 200, 20).build();
		addDrawableChild(crabBtn);

		envoyBtn = ButtonWidget.builder(envoyLabel(), b -> {
			ConfigManager cfg = ConfigManager.get();
			cfg.notifyEnvoy = !cfg.notifyEnvoy;
			b.setMessage(envoyLabel());
			ConfigManager.save();
		}).dimensions(cx - 100, cy + 20, 200, 20).build();
		addDrawableChild(envoyBtn);

		chatBtn = ButtonWidget.builder(chatLabel(), b -> {
			ConfigManager cfg = ConfigManager.get();
			cfg.notifyChatEvents = !cfg.notifyChatEvents;
			b.setMessage(chatLabel());
			ConfigManager.save();
		}).dimensions(cx - 100, cy + 44, 200, 20).build();
		addDrawableChild(chatBtn);

		int pad = 12, w = 200, h = 20;
		int x = (this.width - w) / 2;
		int y = this.height - h - pad;

		doneBtn = ButtonWidget.builder(Text.literal("Done"), b -> close())
				.dimensions(x, y, w, h).build();
		addDrawableChild(doneBtn);
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
		ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 80, 0xFFFFFF);
	}
}