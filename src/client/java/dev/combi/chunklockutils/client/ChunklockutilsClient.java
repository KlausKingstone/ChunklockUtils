package dev.combi.chunklockutils.client;

import dev.combi.chunklockutils.client.config.CluConfig;
import dev.combi.chunklockutils.client.config.CluConfigScreen;
import dev.combi.chunklockutils.client.features.EvolvingProgressBar;
import dev.combi.chunklockutils.client.features.EvolvingTooltip;
import dev.combi.chunklockutils.client.misc.UpdateChecker;
import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChunklockutilsClient implements ClientModInitializer {
	public static final String MODID = "chunklockutils";
	public static final String GITHUB_OWNER = "KlausKingstone";
	public static final String GITHUB_REPO  = "ChunklockUtils";
	private static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	private static KeyBinding openConfig;

	@Override
	public void onInitializeClient() {
		// Check if there are updates available
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
				UpdateChecker.checkAsync(client, MODID, GITHUB_OWNER, GITHUB_REPO));


		// Config handling
		CluConfig.load();
		openConfig = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.chunklockutils.config",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_O,
				"key.categories.chunklockutils"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openConfig.wasPressed()) {
				MinecraftClient.getInstance().setScreen(new CluConfigScreen(MinecraftClient.getInstance().currentScreen));
			}
		});

		// Fix the Evolving tools
		ItemTooltipCallback.EVENT.register((stack, ctx, type, lines) -> {
			EvolvingTooltip.rewriteTooltip(stack, lines);
		});

		// Add progress bar to Evolving tools
		HudRenderCallback.EVENT.register(EvolvingProgressBar::renderHotbar);

		LOGGER.info("[{}] Initialized", MODID);
	}
}
