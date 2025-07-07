package io.github.andrew6rant.autoslabs;

import io.github.andrew6rant.autoslabs.util.RenderUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.block.SlabBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import static io.github.andrew6rant.autoslabs.config.CommonConfig.showCrosshairIcon;

public class AutoSlabsClient implements ClientModInitializer {
	final ModContainer container = FabricLoader.getInstance().getModContainer("autoslabs").get();

	public static KeyBinding SLAB_LOCK_KEYBIND = KeyBindingHelper.registerKeyBinding(
			new KeyBinding("key.autoslabs.place_mode", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.autoslabs.keybinds")
	);

	private static Boolean validKeyPress = true;
	public static SlabLockEnum clientSlabLockPosition = SlabLockEnum.DEFAULT_AUTOSLABS;
	private static long slabIconVisibleUntil = 0;

	private void sendKeybind(SlabLockEnum lockedPosition) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeEnumConstant(lockedPosition);
		ClientPlayNetworking.send(new Identifier("autoslabs", "slab_lock"), buf);
	}

	private void setKeybind(MinecraftClient client) {
		clientSlabLockPosition = clientSlabLockPosition.loop(client.options.sneakKey.isPressed());
		sendKeybind(clientSlabLockPosition);
		client.player.sendMessage(Text.translatable("text.autoslabs.slab_lock."+ clientSlabLockPosition.toString()), true);
		// Show icon for 3 seconds when mode changes
		slabIconVisibleUntil = System.currentTimeMillis() + 3000;
	}

	@Override
	public void onInitializeClient() {
		ResourceManagerHelper.registerBuiltinResourcePack(Identifier.of("autoslabs", "distinct_slabs"), container, Text.literal("Distinct Slabs (Built-In)"), ResourcePackActivationType.DEFAULT_ENABLED);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player != null) {
				if (SLAB_LOCK_KEYBIND.isPressed() && validKeyPress) {
					ItemStack heldItem = client.player.getStackInHand(client.player.getActiveHand());
					if (heldItem != null && !heldItem.isEmpty() && heldItem.getItem() instanceof BlockItem && ((BlockItem) heldItem.getItem()).getBlock() instanceof SlabBlock) {
						validKeyPress = false;
						setKeybind(client);
					}
				}
				if (!SLAB_LOCK_KEYBIND.isPressed() && !validKeyPress) {
					validKeyPress = true;
				}
			}
		});

		HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.player == null) return;

			// Check if player is holding a slab
			ItemStack heldItem = client.player.getStackInHand(client.player.getActiveHand());
			boolean holdingSlab = heldItem != null && !heldItem.isEmpty() &&
				heldItem.getItem() instanceof BlockItem &&
				((BlockItem) heldItem.getItem()).getBlock() instanceof SlabBlock;

			// Don't show icon if not holding slab
			if (!holdingSlab) return;
			
			// Handle different visibility modes
			switch (showCrosshairIcon) {
				case NEVER -> { return; }
				case ON_CHANGE -> {
					if (System.currentTimeMillis() > slabIconVisibleUntil) return;
				}
				case ALWAYS -> {
					// Always show, no additional checks needed
				}
			}

			switch (clientSlabLockPosition) {
				case DEFAULT_AUTOSLABS -> RenderUtil.drawSlabIcon(drawContext, 0, 48);
				case BOTTOM_SLAB -> RenderUtil.drawSlabIcon(drawContext, 0, 0);
				case TOP_SLAB -> RenderUtil.drawSlabIcon(drawContext, 16, 0);
				case NORTH_SLAB_VERTICAL -> RenderUtil.drawSlabIcon(drawContext, 0, 16);
				case SOUTH_SLAB_VERTICAL -> RenderUtil.drawSlabIcon(drawContext, 16, 16);
				case EAST_SLAB_VERTICAL -> RenderUtil.drawSlabIcon(drawContext, 0, 32);
				case WEST_SLAB_VERTICAL -> RenderUtil.drawSlabIcon(drawContext, 16, 32);
				case VANILLA_PLACEMENT -> RenderUtil.drawSlabIcon(drawContext, 32, 0);
			}
		});
	}
}