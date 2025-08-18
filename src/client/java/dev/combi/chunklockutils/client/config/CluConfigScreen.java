package dev.combi.chunklockutils.client.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class CluConfigScreen extends Screen {
	private final Screen parent;
	private ButtonWidget toggleBtn;

	public CluConfigScreen(Screen parent) {
		super(Text.literal("ChunklockUtils Settings"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int cy = this.height / 2;

		toggleBtn = ButtonWidget.builder(currentLabel(), b -> {
			CluConfig cfg = CluConfig.get();
			cfg.showEvolvedProgressBar = !cfg.showEvolvedProgressBar;
			b.setMessage(currentLabel());
			CluConfig.save();
		}).dimensions(cx - 100, cy - 10, 200, 20).build();
		addDrawableChild(toggleBtn);

		addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
				.dimensions(cx - 100, cy + 20, 200, 20).build());
	}

	private Text currentLabel() {
		return Text.literal("Show Evolved Tools progress bar: " + (CluConfig.get().showEvolvedProgressBar ? "ON" : "OFF"));
	}

	@Override
	public void close() {
		CluConfig.save();
		MinecraftClient.getInstance().setScreen(parent);
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		this.renderBackground(ctx, mouseX, mouseY, delta);
		super.render(ctx, mouseX, mouseY, delta);
		ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);
	}
}
