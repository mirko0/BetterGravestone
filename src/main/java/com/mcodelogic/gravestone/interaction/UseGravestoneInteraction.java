package com.mcodelogic.gravestone.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mcodelogic.gravestone.KMain;
import com.mcodelogic.gravestone.component.GravestoneOwnerData;
import com.mcodelogic.gravestone.util.TinyMsg;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.List;

public class UseGravestoneInteraction extends SimpleBlockInteraction {

    public static final BuilderCodec<UseGravestoneInteraction> CODEC = BuilderCodec.builder(
            UseGravestoneInteraction.class,
            UseGravestoneInteraction::new,
            SimpleBlockInteraction.CODEC
    ).build();

/*    public static final UseGravestoneInteraction INSTANCE = new UseGravestoneInteraction("UseGravestoneInteraction");

    public static final RootInteraction ROOT = new RootInteraction(INSTANCE.getId(), INSTANCE.getId());

    public UseGravestoneInteraction() {
        super();
    }

    public UseGravestoneInteraction(String id) {
        super();
        try {
            java.lang.reflect.Field idField = Interaction.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(this, id);
        } catch (Exception ignored) {}
    }*/

    @SuppressWarnings("removal")
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
        CombinedItemContainer playerInv = player.getInventory().getCombinedEverything();

        Ref<ChunkStore> blockEntity = BlockModule.getBlockEntity(world, targetBlock.x, targetBlock.y, targetBlock.z);
        Store<ChunkStore> blockStore = blockEntity.getStore();
        try {
            BlockState blockState = BlockState.getBlockState(blockEntity, blockStore);
            if (!(blockState instanceof ItemContainerState)) {
                KMain.get().getLogger().atInfo().log("Block state is not ItemContainerState");
                return;
            }
            SimpleItemContainer graveInv = (SimpleItemContainer) ((ItemContainerState) blockState).getItemContainer();

            GravestoneOwnerData ownerComponent = blockStore.getComponent(blockEntity, GravestoneOwnerData.getComponentType());
            if (ownerComponent == null) {
                KMain.get().getLogger().atInfo().log("No owner component found at: " + targetBlock.x + ", " + targetBlock.y + ", " + targetBlock.z);
                graveInv.clear();
                breakGravestone(world, targetBlock);
                return;
            }
            Long timeOfPlacement = ownerComponent.getTimeOfPlacement();
            long currentTime = System.currentTimeMillis();
            long timeSincePlacement = currentTime - timeOfPlacement;
            int maximumSecondsSincePlacement = KMain.get().getConfiguration().getTimeToCollectGrave();

            if (timeSincePlacement > (maximumSecondsSincePlacement * 1000)) {
                player.sendMessage(TinyMsg.parse(KMain.get().getConfiguration().getMessageGravestoneItemsExpired()));
                if (graveInv != null) {
                    graveInv.clear();
                }
                graveInv.clear();
                breakGravestone(world, targetBlock);
                return;
            }
            String ownerUUID = ownerComponent.getOwner();
            if (!ownerUUID.equals(playerUUID) && !player.hasPermission(KMain.get().getConfiguration().getBreakOthersPermission())) {
                player.sendMessage(TinyMsg.parse(KMain.get().getConfiguration().getMessageNotOwner()
                        .replace("{time}", String.valueOf(maximumSecondsSincePlacement - timeSincePlacement / 1000))));
                return;
            }

            for (short i = 0; i < graveInv.getCapacity(); i++) {
                ItemStack item = graveInv.getItemStack(i);
                if (item != null && !item.isEmpty()) {
                    ItemStack remainder = playerInv.addItemStack(item).getRemainder();
                    graveInv.setItemStackForSlot(i, remainder);
                }
            }

            if (!graveInv.isEmpty()) {
                world.execute(() -> {
                    HeadRotation rot = (HeadRotation) store.getComponent(playerRef, HeadRotation.getComponentType());
                    Vector3f facing = rot != null ? rot.getRotation() : new Vector3f();
                    List<ItemStack> remaining = graveInv.dropAllItemStacks();
                    Vector3d dropPos = targetBlock.clone().add(0, 1, 0).toVector3d();

                    Holder[] drops = ItemComponent.generateItemDrops(store, remaining, dropPos, facing);
                    for (Holder drop : drops) {
                        world.getEntityStore().getStore().addEntity(drop, AddReason.SPAWN);
                    }
                });
            }

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
