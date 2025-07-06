package io.github.andrew6rant.autoslabs;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SlabLockPayload(int slabLock) implements CustomPayload {
    public static final Identifier SLAB = Identifier.of("autoslabs", "slab_lock");
    public static final CustomPayload.Id<SlabLockPayload> ID = new CustomPayload.Id<>(SLAB);
    public static final PacketCodec<RegistryByteBuf, SlabLockPayload> CODEC = PacketCodec.tuple(PacketCodecs.INTEGER, SlabLockPayload::slabLock, SlabLockPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}