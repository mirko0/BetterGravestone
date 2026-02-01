package com.mcodelogic.gravestone.listener;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;
import com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mcodelogic.gravestone.KMain;
import com.mcodelogic.gravestone.component.GravestoneOwnerData;
import com.mcodelogic.gravestone.util.TinyMsg;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class DropItemsOnDeathListener extends DeathSystems.OnDeathSystem {

    private final KMain plugin;

    public DropItemsOnDeathListener(KMain plugin) {
        this.plugin = plugin;
    }

    @Nonnull
    private static final Query<EntityStore> QUERY = Archetype.of(Player.getComponentType(), TransformComponent.getComponentType(), HeadRotation.getComponentType());

    @Nonnull
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public void onComponentAdded(@NonNullDecl Ref<EntityStore> playerRef, @NonNullDecl DeathComponent component,
                                 @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {
        World world = commandBuffer.getExternalData().getWorld();
        Player player = store.getComponent(playerRef, Player.getComponentType());
        UUIDComponent uuidComp = store.getComponent(playerRef, UUIDComponent.getComponentType());
        if (player == null || uuidComp == null) return;
        if (player.getGameMode() == GameMode.Creative) return;
        DeathConfig config = world.getDeathConfig();

        component.setDisplayDataOnDeathScreen(true);

        CombinedItemContainer combinedItemContainer = player.getInventory().getCombinedEverything();
        double durabilityLossPercentage = config.getItemsDurabilityLossPercentage();
        if (durabilityLossPercentage > (double) 0.0F) {
            double durabilityLossRatio = durabilityLossPercentage / (double) 100.0F;
            boolean hasArmorBroken = false;

            for (short i = 0; i < combinedItemContainer.getCapacity(); ++i) {
                ItemStack itemStack = combinedItemContainer.getItemStack(i);
                if (!ItemStack.isEmpty(itemStack) && !itemStack.isBroken()) {
                    double durabilityLoss = itemStack.getMaxDurability() * durabilityLossRatio;
                    ItemStack updatedItemStack = itemStack.withIncreasedDurability(-durabilityLoss);
                    ItemStackSlotTransaction transaction = combinedItemContainer.replaceItemStackInSlot(i, itemStack, updatedItemStack);
                    if (transaction.getSlotAfter().isBroken() && itemStack.getItem().getArmor() != null) {
                        hasArmorBroken = true;
                    }
                }
            }

            if (hasArmorBroken) {
                player.getStatModifiersManager().setRecalculate(true);
            }
        }

        if (config.getItemsLossMode() == DeathConfig.ItemsLossMode.NONE) return;
        double lossPercentage = config.getItemsAmountLossPercentage();


        plugin.getLogger().atInfo().log("Death config item loss mode: " + config.getItemsLossMode());
        List<ItemStack> itemsToDrop = null;
        if (config.getItemsLossMode() == DeathConfig.ItemsLossMode.ALL) {
            itemsToDrop = player.getInventory().dropAllItemStacks();
        }else {
            double itemsAmountLossPercentage = lossPercentage;
            if (itemsAmountLossPercentage > (double) 0.0F) {
                double itemAmountLossRatio = itemsAmountLossPercentage / (double) 100.0F;
                itemsToDrop = new ObjectArrayList();

                for (short i = 0; i < combinedItemContainer.getCapacity(); ++i) {
                    ItemStack itemStack = combinedItemContainer.getItemStack(i);
                    if (!ItemStack.isEmpty(itemStack) && itemStack.getItem().dropsOnDeath()) {
                        int quantityToLose = Math.max(1, MathUtil.floor((double) itemStack.getQuantity() * itemAmountLossRatio));
                        itemsToDrop.add(itemStack.withQuantity(quantityToLose));
                        int newQuantity = itemStack.getQuantity() - quantityToLose;
                        if (newQuantity > 0) {
                            ItemStack updatedItemStack = itemStack.withQuantity(newQuantity);
                            combinedItemContainer.replaceItemStackInSlot(i, itemStack, updatedItemStack);
                        }else {
                            combinedItemContainer.removeItemStackFromSlot(i);
                        }
                    }
                }
            }
        }

        if (itemsToDrop != null && !itemsToDrop.isEmpty()) {
            TransformComponent transformComponent = store.getComponent(playerRef, TransformComponent.getComponentType());
            assert transformComponent != null;
            Vector3i pos = transformComponent.getPosition().toVector3i();
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();

            List<ItemStack> finalItemsToDrop = itemsToDrop;
            plugin.getExecutor().schedule(() -> world.execute(() -> prepareChunk(world, x, y, z, finalItemsToDrop, player, uuidComp.getUuid())), 100L, TimeUnit.MILLISECONDS);
            component.setItemsLostOnDeath(itemsToDrop);

            if (plugin.getConfiguration().isSendLocation()) {
                player.sendMessage(TinyMsg.parse(plugin.getConfiguration().getSendLocationMessage()
                        .replace("{x}", String.valueOf(x))
                        .replace("{y}", String.valueOf(y))
                        .replace("{z}", String.valueOf(z))));
            }
        }
    }


    private void prepareChunk(World world, int x, int y, int z, List<ItemStack> items, Player player, UUID playerUUID) {
        try {
            long chunkKey = ChunkUtil.indexChunk(x, z);
            WorldChunk chunk = world.getChunkIfLoaded(chunkKey);
            if (chunk == null) {
                world.getChunkAsync(chunkKey).thenAccept(c -> {
                    if (c != null) {
                        world.execute(() -> {
                            plugin.getLogger().atInfo().log("Created a gravestone for player: " + player.getDisplayName() + " at: " + x + ", " + y + ", " + z);
                            placeBlock(world, playerUUID, c, x, y, z, items);
                        });
                    }
                });
            }else {
                plugin.getLogger().atInfo().log("Created a gravestone for player: " + player.getDisplayName() + " at: " + x + ", " + y + ", " + z);
                placeBlock(world, playerUUID, chunk, x, y, z, items);
            }
        } catch (Exception e) {
            plugin.getLogger().atSevere().log("Failed to place gravestone at (" + x + ", " + y + ", " + z + ")");
            e.printStackTrace();
        }
    }

    private void placeBlock(World world, UUID ownerUuid, WorldChunk chunk, int x, int y, int z, List<ItemStack> items) {
        try {
            chunk.setBlock(x, y, z, 0);
            world.setBlock(x, y, z, "Furniture_Gravestone");
            fillContainer(world, x, y, z, items, ownerUuid);
        } catch (Exception e) {
            plugin.getLogger().atInfo().log("Error placing tombstone at : " + x + ", " + y + ", " + z + ". Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SuppressWarnings("removal")
    private void fillContainer(World world, int x, int y, int z, List<ItemStack> items, UUID ownerUuid) {
        try {
            Ref<ChunkStore> blockEntity = BlockModule.getBlockEntity(world, x, y, z);
            Store<ChunkStore> store = blockEntity.getStore();
            try {
                GravestoneOwnerData ownerComponent = store.getComponent(blockEntity, GravestoneOwnerData.getComponentType());
                if (ownerComponent == null) {
                    plugin.getLogger().atInfo().log("No owner component found at " + x + ", " + y + ", " + z);
                }else {
                    ownerComponent.setOwner(ownerUuid.toString());
                    ownerComponent.setTimeOfPlacement(System.currentTimeMillis());
                }
            } catch (Exception e) {
                plugin.getLogger().atInfo().log("Error setting owner at " + x + ", " + y + ", " + z + " - " + e.getMessage());
                e.printStackTrace();
            }

            BlockState blockState = BlockState.getBlockState(blockEntity, store);
            if (blockState instanceof ItemContainerState container) {
                SimpleItemContainer inv = new SimpleItemContainer((short) Math.max(items.size(), 1));
                container.setItemContainer(inv);
                inv.addItemStacks(items);
            }else {
                plugin.getLogger().atInfo().log("Block state not ItemContainerState at " + x + ", " + y + ", " + z + " - Block Type: " + blockState.getClass().getSimpleName());
            }
        } catch (Exception e) {
            plugin.getLogger().atInfo().log("Error filling container: " + e.getMessage());
        }
    }

}
