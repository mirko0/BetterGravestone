package com.mcodelogic.gravestone.util;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mcodelogic.gravestone.component.GravestoneOwnerData;

import java.util.List;

public class DropGravestone {

    public static void dropGravestone(GravestoneOwnerData ownerComponent, Vector3d targetBlock, Ref<EntityStore> playerRef, CombinedItemContainer playerInv, Store<EntityStore> store, World world) {
        if (ownerComponent.getItems() == null) {
            return;
        }
        for (short i = 0; i < ownerComponent.getCapacity(); i++) {
            ItemStack item = ownerComponent.getItems().get(i);
            ownerComponent.getItems().remove(i);
            if (item != null && !item.isEmpty()) {
                // Give user items from the slot and if there is any left, put it in the player inventory
                ItemStack remainder = playerInv.addItemStack(item).getRemainder();
                ownerComponent.getItems().put(i, remainder);
            }
        }

        if (!ownerComponent.getItems().isEmpty()) {
            world.execute(() -> {
                HeadRotation rot = store.getComponent(playerRef, HeadRotation.getComponentType());
                Vector3f facing = rot != null ? rot.getRotation() : new Vector3f();
                List<ItemStack> remaining = ownerComponent.getItems().values().stream().toList();
                Vector3d dropPos = targetBlock.clone().add(0, 1, 0);

                Holder[] drops = ItemComponent.generateItemDrops(store, remaining, dropPos, facing);
                for (Holder drop : drops) {
                    world.getEntityStore().getStore().addEntity(drop, AddReason.SPAWN);
                }
            });
        }
    }
}
