package dev.combi.chunklockutils.client.commands;

import com.mojang.serialization.DataResult;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class PrintNbtCommand {
	private static boolean armed = false;
	private static long deadlineMs = 0L;

	private PrintNbtCommand() {}

	public static void init() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, ra) ->
				dispatcher.register(ClientCommandManager.literal("printnbt")
						.executes(ctx -> execute(ctx.getSource()))));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (!armed || client.player == null) return;

			if (client.currentScreen == null) {
				ItemEntity ie = pickLookedItem(client, 8.0);
				if (ie != null) {
					armed = false;
					dump(ie);
					return;
				}
			}

			if (System.currentTimeMillis() > deadlineMs) {
				armed = false;
				sendToChat("§cNo dropped item found in front of you. Try again closer (F3+B).");
			}
		});
	}

	private static int execute(FabricClientCommandSource src) {
		var mc = MinecraftClient.getInstance();
		if (mc.player == null) return 0;

		if (mc.currentScreen == null) {
			ItemEntity ie = pickLookedItem(mc, 8.0);
			if (ie != null) {
				dump(ie);
				return 1;
			}
		}

		armed = true;
		deadlineMs = System.currentTimeMillis() + 2_000L;
		src.sendFeedback(Text.literal("§eArmed: close chat and aim at the dropped item (2s)…"));
		return 1;
	}

	private static ItemEntity pickLookedItem(MinecraftClient mc, double maxDist) {
		if (mc.player == null || mc.world == null) return null;

		Vec3d start = mc.player.getCameraPosVec(1.0f);
		Vec3d dir   = mc.player.getRotationVec(1.0f);
		Vec3d end   = start.add(dir.multiply(maxDist));
		Box box     = mc.player.getBoundingBox().stretch(dir.multiply(maxDist)).expand(1.0);

		EntityHitResult ehr = ProjectileUtil.raycast(
				mc.player, start, end, box,
				(Entity e) -> e instanceof ItemEntity && e.isAlive(),
				maxDist
		);
		if (ehr != null && ehr.getEntity() instanceof ItemEntity ie1) return ie1;

		HitResult hr = mc.crosshairTarget;
		if (hr instanceof EntityHitResult ehr2 && ehr2.getEntity() instanceof ItemEntity ie2) return ie2;

		return null;
	}

	private static void dump(ItemEntity ie) {
		ItemStack stack = ie.getStack();
		String snbt = stackToSnbt(stack, true);
		System.out.println(snbt);
		sendToChat("§aPrinted NBT/components for §f" + stack.getName().getString() + " §7x" + stack.getCount() + " §ato console.");
	}

	private static void sendToChat(String msg) {
		var mc = MinecraftClient.getInstance();
		if (mc.player != null) {
			mc.player.sendMessage(Text.literal(msg), false);
		}
	}

	private static String stackToSnbt(ItemStack stack, boolean pretty) {
		var mc = MinecraftClient.getInstance();

		RegistryWrapper.WrapperLookup lookup = null;
		if (mc.world != null) {
			lookup = mc.world.getRegistryManager();
		} else if (mc.getNetworkHandler() != null) {
			try {
				lookup = mc.getNetworkHandler().getRegistryManager();
			} catch (Throwable ignored) {
				try {
					var m = mc.getNetworkHandler().getClass().getMethod("getCombinedRegistryManager");
					lookup = (RegistryWrapper.WrapperLookup) m.invoke(mc.getNetworkHandler());
				} catch (Throwable ignored2) {}
			}
		}

		var ops = (lookup != null) ? RegistryOps.of(NbtOps.INSTANCE, lookup) : NbtOps.INSTANCE;

		DataResult<NbtElement> res = ItemStack.CODEC.encodeStart(ops, stack);
		var tagOpt = res.result();
		if (tagOpt.isEmpty()) {
			return "<encode failed> " + res.error().map(Object::toString).orElse("");
		}

		NbtElement tag = tagOpt.get();
		return pretty ? pretty(tag) : tag.toString();
	}

	private static String pretty(NbtElement tag) {
		try {
			Class<?> cls = Class.forName("net.minecraft.nbt.NbtHelper");
			var m = cls.getMethod("toPrettyPrintedString", NbtElement.class);
			return (String) m.invoke(null, tag);
		} catch (Throwable ignore) {
			return tag.toString();
		}
	}
}