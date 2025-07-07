package io.github.andrew6rant.autoslabs;

import io.github.andrew6rant.autoslabs.util.PlacementUtil;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public class PlacementHandler {
    
    public static void init() {
        UseBlockCallback.EVENT.register(PlacementHandler::onUseBlock);
    }
    
    private static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        ItemStack stack = player.getStackInHand(hand);
        
        // Check if this is a slab item
        if (!(stack.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof SlabBlock)) {
            return ActionResult.PASS; // Let vanilla handle non-slab items
        }
        
        // Check if player is using autoslabs mode
        if (AutoSlabs.slabLockPosition.getOrDefault(player, SlabLockEnum.DEFAULT_AUTOSLABS).equals(SlabLockEnum.VANILLA_PLACEMENT)) {
            return ActionResult.PASS; // Let vanilla handle vanilla placement mode
        }
        
        // Create placement context to check if this is actually a valid slab placement
        ItemPlacementContext context = new ItemPlacementContext(player, hand, stack, hitResult);
        BlockPos placementPos = context.getBlockPos();
        BlockState currentState = world.getBlockState(placementPos);
        
        // Only handle if we're placing on a slab or empty space (not on other blocks)
        boolean shouldHandle = currentState.getBlock() instanceof SlabBlock || currentState.isAir();
        
        // If we shouldn't handle this placement, let vanilla handle it
        if (!shouldHandle) {
            return ActionResult.PASS;
        }
        
        // Client-side: Return SUCCESS to allow animation but prevent vanilla placement
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        
        // Server-side: Handle actual placement
        if (player instanceof ServerPlayerEntity serverPlayer && world instanceof ServerWorld serverWorld) {
            return handleServerPlacement(serverPlayer, serverWorld, hand, hitResult, stack, blockItem);
        }
        
        return ActionResult.PASS;
    }
    
    private static ActionResult handleServerPlacement(ServerPlayerEntity player, ServerWorld world, Hand hand, 
                                                     BlockHitResult hitResult, ItemStack stack, BlockItem blockItem) {
        // Create placement context
        ItemPlacementContext context = new ItemPlacementContext(player, hand, stack, hitResult);
        
        // Get the actual placement position (context handles offset logic)
        BlockPos placementPos = context.getBlockPos();
        BlockState currentState = world.getBlockState(placementPos);
        
        // Check if player can place at this position
        if (!player.canPlaceOn(placementPos, hitResult.getSide(), stack)) {
            return ActionResult.FAIL;
        }
        
        // Check if there's an entity in the way (simplified check)
        if (!world.getOtherEntities(null, new Box(placementPos)).isEmpty()) {
            return ActionResult.FAIL;
        }
        
        // Calculate placement state
        BlockState placementState = PlacementUtil.calcPlacementState(context, blockItem.getBlock().getDefaultState());
        
        if (placementState == null) {
            // Placement not valid
            return ActionResult.FAIL;
        }
        
        // Check if we can replace the existing block (only if it's a slab)
        if (currentState.getBlock() instanceof SlabBlock && !PlacementUtil.canReplace(currentState, context)) {
            return ActionResult.FAIL;
        }
        
        // Check if the placement state can be placed at this position
        if (!placementState.canPlaceAt(world, placementPos)) {
            return ActionResult.FAIL;
        }
        
        // Place the block at the correct position
        boolean placed = world.setBlockState(placementPos, placementState);
        
        if (placed) {
            // Play placement sound
            world.playSound(null, placementPos, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            
            // Decrement item stack (only on server)
            if (!player.getAbilities().creativeMode) {
                stack.decrement(1);
            }
            
            return ActionResult.SUCCESS;
        }
        
        return ActionResult.FAIL;
    }
} 