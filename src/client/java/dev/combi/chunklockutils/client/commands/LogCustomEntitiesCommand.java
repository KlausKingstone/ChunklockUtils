package dev.combi.chunklockutils.client.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class LogCustomEntitiesCommand {
	private LogCustomEntitiesCommand() {}

	public static void init() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, ra) ->
				dispatcher.register(
						ClientCommandManager.literal("logentities")
								.executes(ctx -> run(3))
								.then(ClientCommandManager.argument("radius", IntegerArgumentType.integer(1, 10))
										.executes(ctx -> run(IntegerArgumentType.getInteger(ctx, "radius"))))
				)
		);
	}

	private static int run(int radius) {
		var mc = MinecraftClient.getInstance();
		if (mc.player == null || mc.world == null) return 0;

		var p = mc.player;
		Box box = p.getBoundingBox().expand(radius);

		List<Entity> displays = mc.world.getOtherEntities(p, box, LogCustomEntitiesCommand::isItemDisplay);
		displays.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(p)));

		System.out.println("=== EntityParts within " + radius + " blocks (count=" + displays.size() + ") ===");
		for (Entity e : displays) {
			String type = String.valueOf(EntityType.getId(e.getType()));
			double dist = Math.sqrt(e.squaredDistanceTo(p));
			String pos  = String.format(Locale.ROOT, "(%.2f, %.2f, %.2f)", e.getX(), e.getY(), e.getZ());

			ItemStack stack = getItemStackFromDisplay(e);
			String stackSnbt = stackToSnbt(stack);

			System.out.println(type + "  id=" + e.getId()
					+ "  dist=" + String.format(Locale.ROOT, "%.2f", dist)
					+ "  pos=" + pos);
			System.out.println("  ItemStack: " + stackSnbt);
		}
		System.out.println("=== End (" + displays.size() + ") ===");
		System.out.flush();

		p.sendMessage(Text.literal("§aLogged " + displays.size() + " entity parts to console."), false);
		return 1;
	}

	private static boolean isItemDisplay(Entity e) {
		if (e instanceof DisplayEntity.ItemDisplayEntity) return true;
		return "ItemDisplayEntity".equals(e.getClass().getSimpleName())
				|| (e instanceof DisplayEntity && hasMethod(e, "getItemStack"));
	}

	private static ItemStack getItemStackFromDisplay(Entity e) {
		try {
			if (e instanceof DisplayEntity.ItemDisplayEntity id) return id.getItemStack();
		} catch (Throwable ignored) {}
		try {
			Method m = e.getClass().getMethod("getItemStack");
			Object obj = m.invoke(e);
			if (obj instanceof ItemStack s) return s;
		} catch (Throwable ignored) {}
		return ItemStack.EMPTY;
	}

	private static String stackToSnbt(ItemStack stack) {
		try {
			var mc = MinecraftClient.getInstance();
			RegistryWrapper.WrapperLookup lookup =
					mc.world != null ? mc.world.getRegistryManager()
							: (mc.getNetworkHandler() != null ? mc.getNetworkHandler().getRegistryManager()
							: DynamicRegistryManager.EMPTY);
			var ops = RegistryOps.of(NbtOps.INSTANCE, lookup);
			var res = ItemStack.CODEC.encodeStart(ops, stack);
			return res.result().map(NbtElement::toString)
					.orElseGet(() -> Registries.ITEM.getId(stack.getItem()) + " x" + stack.getCount());
		} catch (Throwable t) {
			return Registries.ITEM.getId(stack.getItem()) + " x" + stack.getCount();
		}
	}

	private static boolean hasMethod(Object o, String name) {
		for (Method m : o.getClass().getMethods()) if (m.getName().equals(name)) return true;
		return false;
	}
}
