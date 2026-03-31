package com.mcodelogic.gravestone.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mcodelogic.gravestone.KMain;
import com.mcodelogic.gravestone.component.GravestoneOwnerData;
import com.mcodelogic.gravestone.util.DropGravestone;
import com.mcodelogic.gravestone.util.TinyMsg;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class UseGravestoneInteraction extends SimpleBlockInteraction {

    public static final BuilderCodec<UseGravestoneInteraction> CODEC = BuilderCodec.builder(
            UseGravestoneInteraction.class,
            UseGravestoneInteraction::new,
            SimpleBlockInteraction.CODEC
    ).build();

    @Override
    protected void interactWithBlock(@NonNullDecl World world,
                                     @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
                                     @NonNullDecl InteractionType interactionType,
                                     @NonNullDecl InteractionContext interactionContext,
                                     @NullableDecl ItemStack itemStack,
                                     @NonNullDecl Vector3i targetBlock,
                                     @NonNullDecl CooldownHandler cooldownHandler) {
        Ref<EntityStore> playerRef = interactionContext.getEntity();
        if (playerRef == null) {
            KMain.get().getLogger().atInfo().log("Player reference is null");
            return;
        }
        Store<EntityStore> store = playerRef.getStore();
        if (store == null) {
            KMain.get().getLogger().atInfo().log("Store is null");
            return;
        }
        Player player = commandBuffer.getComponent(playerRef, Player.getComponentType());
        if (player == null) {
            KMain.get().getLogger().atInfo().log("Player component is null");
            return;
        }
        UUIDComponent uuidComp = commandBuffer.getComponent(playerRef, UUIDComponent.getComponentType());
        if (uuidComp == null) {
            KMain.get().getLogger().atInfo().log("UUID component is null");
            return;
        }
        String playerUUID = uuidComp.getUuid().toString();
        Ref<ChunkStore> blockEntity = BlockModule.getBlockEntity(world, targetBlock.x, targetBlock.y, targetBlock.z);
        Store<ChunkStore> blockStore = blockEntity.getStore();
        try {
            GravestoneOwnerData ownerComponent = blockStore.getComponent(blockEntity, GravestoneOwnerData.getComponentType());
            if (ownerComponent == null) {
                KMain.get().getLogger().atInfo().log("No owner component found at: " + targetBlock.x + ", " + targetBlock.y + ", " + targetBlock.z);
                breakGravestone(world, targetBlock);
                return;
            }
            Long timeOfPlacement = ownerComponent.getTimeOfPlacement();
            long currentTime = System.currentTimeMillis();
            long timeSincePlacement = currentTime - timeOfPlacement;
            int maximumSecondsSincePlacement = KMain.get().getConfiguration().getTimeToCollectGrave();

            boolean isExpired = timeSincePlacement > (maximumSecondsSincePlacement * 1000L);
            String ignoreTimePermission = KMain.get().getConfiguration().getIgnoreTimePermission();
            boolean ignoreTime = KMain.get().getConfiguration().getIgnoreTime();

            if (isExpired && !player.hasPermission(ignoreTimePermission) && !ignoreTime) {
                player.sendMessage(TinyMsg.parse(KMain.get().getConfiguration().getMessageGravestoneItemsExpired()));
                if (ownerComponent.getItems() != null) {
                    ownerComponent.getItems().clear();
                }
                breakGravestone(world, targetBlock);
                return;
            }
            String ownerUUID = ownerComponent.getOwner();
            if (!ownerUUID.equals(playerUUID) && !player.hasPermission(KMain.get().getConfiguration().getBreakOthersPermission())) {
                player.sendMessage(TinyMsg.parse(KMain.get().getConfiguration().getMessageNotOwner()
                        .replace("{time}", String.valueOf(maximumSecondsSincePlacement - timeSincePlacement / 1000))));
                return;
            }

            CombinedItemContainer playerInv = InventoryComponent.getCombined(commandBuffer, playerRef, InventoryComponent.EVERYTHING);
            DropGravestone.dropGravestone(ownerComponent, new Vector3d(targetBlock.x, targetBlock.y, targetBlock.z), playerRef, playerInv, store, world);

            breakGravestone(world, targetBlock);
        } catch (Exception e) {
            KMain.get().getLogger().atInfo().log("Error Checking components for gravestone at :  " + targetBlock.x + ", " + targetBlock.y + ", " + targetBlock.z + " - " + e.getMessage());
            e.printStackTrace();
        }
    }




    private void breakGravestone(World world, Vector3i targetBlock) {
        world.execute(() -> world.breakBlock(targetBlock.x, targetBlock.y, targetBlock.z, 0));
    }


    @Override
    protected void simulateInteractWithBlock(@NonNullDecl InteractionType interactionType, @NonNullDecl InteractionContext interactionContext, @NullableDecl ItemStack itemStack, @NonNullDecl World world, @NonNullDecl Vector3i vector3i) {

    }
}
