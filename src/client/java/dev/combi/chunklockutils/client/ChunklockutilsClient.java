package dev.combi.chunklockutils.client;

import dev.combi.chunklockutils.client.commands.LogCustomEntitiesCommand;
import dev.combi.chunklockutils.client.commands.PrintNbtCommand;
import dev.combi.chunklockutils.client.config.ConfigManager;
import dev.combi.chunklockutils.client.config.ConfigScreen;
import dev.combi.chunklockutils.client.features.EventsReminder;
import dev.combi.chunklockutils.client.features.EvolvingProgressBar;
import dev.combi.chunklockutils.client.features.EvolvingTooltip;
import dev.combi.chunklockutils.client.features.MissingRankItems;
import dev.combi.chunklockutils.client.misc.UpdateChecker;
import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChunklockutilsClient implements ClientModInitializer {
	public static final String MODID = "chunklockutils";
	public static final String GITHUB_OWNER = "KlausKingstone";
	public static final String GITHUB_REPO  = "ChunklockUtils";
	private static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	private static KeyBinding openConfig;
	private static KeyBinding toggleHideItems;

	private Screen lastScreen = null;

	@Override
	public void onInitializeClient() {
		// Update checker
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
				UpdateChecker.checkAsync(client, MODID, GITHUB_OWNER, GITHUB_REPO));


		// Config handling
		ConfigManager.load();
		openConfig = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.chunklockutils.config",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_O,
				"key.categories.chunklockutils"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openConfig.wasPressed()) {
				MinecraftClient.getInstance().setScreen(new ConfigScreen(MinecraftClient.getInstance().currentScreen));
			}
		});

		// Fix evolving tools
		ItemTooltipCallback.EVENT.register((stack, ctx, type, lines) -> {
			EvolvingTooltip.rewriteTooltip(stack, lines);
		});

		// Add event notifications
		EventsReminder.init();

		// Register commands
		PrintNbtCommand.init();
		LogCustomEntitiesCommand.init();

		// Register rank progress feature
		MissingRankItems.register();

		// Add progress bar to evolving tools
		HudRenderCallback.EVENT.register(EvolvingProgressBar::renderHotbar);

		// Hide dropped items keybind
		toggleHideItems = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.chunklockutils.toggle_hide_items",
				GLFW.GLFW_KEY_I, // default: I
				"key.categories.chunklockutils"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) return;
			while (toggleHideItems.wasPressed()) {
				var cfg = ConfigManager.get();
				cfg.hideDroppedItems = !cfg.hideDroppedItems;
				ConfigManager.save();

				client.player.sendMessage(
						Text.empty()
								.append(Text.literal("["))
								.append(Text.literal("ChunklockUtils")
										.styled(s -> s.withBold(true).withColor(0x00FFC8)))
								.append(Text.literal("] "))
								.append(Text.literal("Dropped items are now ").styled(s -> s.withColor(0xDADADA))
								.append(Text.literal(cfg.hideDroppedItems ? "HIDDEN" : "SHOWN").styled(s -> s.withBold(true).withColor(cfg.hideDroppedItems ? 0xFF5555 : 0x55FF55)))),
						false
				);

				client.player.playSound(SoundEvents.BLOCK_LEVER_CLICK, 0.7f, 1.2f);
			}
		});

		LOGGER.info("[{}] Initialized", MODID);
	}
}
