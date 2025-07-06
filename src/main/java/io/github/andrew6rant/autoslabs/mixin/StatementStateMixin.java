package io.github.andrew6rant.autoslabs.mixin;

import io.github.andrew6rant.autoslabs.statement.StatementStateExtensions;
import net.minecraft.block.SlabBlock;
import net.minecraft.state.State;
import net.minecraft.state.property.Property;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(State.class)
public class StatementStateMixin<O, S> implements StatementStateExtensions<S> {
    @Shadow @Final protected O owner;
    
    @Inject(method = "get", cancellable = true, at = @At(value = "INVOKE", target = "Ljava/lang/IllegalArgumentException;<init>(Ljava/lang/String;)V", shift = At.Shift.BEFORE))
    private <T extends Comparable<T>> void statement$onGet(Property<T> property, CallbackInfoReturnable<T> info) {
        // Only handle slab-related properties on slab blocks
        if (this.owner instanceof SlabBlock && isSlabProperty(property)) {
            // Provide fallback value for slab properties
            info.setReturnValue(property.getValues().iterator().next());
        }
        // For all other properties (including sculk_sensor_phase), let the exception be thrown
        // This preserves the vanilla sculk sensor bug
    }
    
    @Unique
    private boolean isSlabProperty(Property<?> property) {
        return property.getName().equals("vertical_type") || property.getName().equals("type");
    }
    
    @Override
    public void statement_initShapeCache() {
        // No-op for our minimal implementation
    }
} 