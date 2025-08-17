package dev.combi.chunklockutils.client;

import dev.combi.chunklockutils.client.features.EvolvingTooltip;
import dev.combi.chunklockutils.client.misc.UpdateChecker;
import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChunklockutilsClient implements ClientModInitializer {
	public static final String MODID = "chunklockutils";
	public static final String GITHUB_OWNER = "KlausKingstone";
	public static final String GITHUB_REPO  = "ChunklockUtils";
	private static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	@Override
	public void onInitializeClient() {

		// Check if there are updates available
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
				UpdateChecker.checkAsync(client, MODID, GITHUB_OWNER, GITHUB_REPO));

		// Fix the Evolving tools
		ItemTooltipCallback.EVENT.register((stack, ctx, type, lines) -> {
			EvolvingTooltip.rewriteTooltip(stack, lines);
		});

		LOGGER.info("[{}] Initialized", MODID);
	}
}
