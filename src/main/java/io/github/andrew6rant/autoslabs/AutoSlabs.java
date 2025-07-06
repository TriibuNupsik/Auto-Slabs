package io.github.andrew6rant.autoslabs;

import io.github.thepoultryman.arrp_but_different.api.RuntimeResourcePack;
import io.github.thepoultryman.arrp_but_different.fabric.ARRPEvent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import io.github.andrew6rant.autoslabs.statement.StatementStateRefresher;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

import static io.github.andrew6rant.autoslabs.config.CommonConfig.dumpResources;

public class AutoSlabs implements ModInitializer {
	public static final RuntimeResourcePack AUTO_SLABS_RESOURCES = RuntimeResourcePack.create(Identifier.of("autoslabs", "resources"));

	public static final Map<PlayerEntity, SlabLockEnum> slabLockPosition = new HashMap<>();

	@Override
	public void onInitialize() {
		// Config is initialized in a static block in StateMixin. I need it to run earlier than State$get, and AutoSlabs$onInitialize is too late.
		for (Block block : Registries.BLOCK) {
			Util.registerSlab(block);
		}

		RegistryEntryAddedCallback.event(Registries.BLOCK).register((raw, id, block) -> Util.registerSlab(block));

		StatementStateRefresher.INSTANCE.reorderBlockStates();

		// custom ARRP entrypoint that is only available in my fork of ARRP
		ARRPEvent.BETWEEN_MODS_AND_USER.register(a -> a.add(AUTO_SLABS_RESOURCES));
		// if (dumpResources) {
		// 	AUTO_SLABS_RESOURCES.dump();
		// }

		ServerPlayNetworking.registerGlobalReceiver(SlabLockPayload.ID, (payload, context) -> {
			SlabLockEnum slabLockBuf = SlabLockEnum.POSITION_VALUES[payload.slabLock()];
			slabLockPosition.put(context.player(), slabLockBuf);
		});
	}
}