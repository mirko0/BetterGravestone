package com.mcodelogic.gravestone.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.Short2ObjectMapCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.mcodelogic.gravestone.KMain;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import lombok.*;

import javax.annotation.Nullable;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@ToString
public class GravestoneOwnerData implements Component<ChunkStore>{
    public static final BuilderCodec<GravestoneOwnerData> CODEC = BuilderCodec
            .builder(GravestoneOwnerData.class, GravestoneOwnerData::new)
            .append(new KeyedCodec<>("Owner", Codec.STRING),
                    (GravestoneOwnerData, value) -> GravestoneOwnerData.owner = value,
                    (GravestoneOwnerData) -> GravestoneOwnerData.owner)
            .add()
            .append(new KeyedCodec<>("TimeOfPlacement", Codec.LONG),
                    (GravestoneOwnerData, value) -> GravestoneOwnerData.timeOfPlacement = value,
                    (GravestoneOwnerData) -> GravestoneOwnerData.timeOfPlacement)
            .add()
           .append(new KeyedCodec<>("Capacity", Codec.SHORT),
                    (o, i) -> o.capacity = i, (o) -> o.capacity)
            .addValidator(Validators.greaterThanOrEqual((short)0))
            .add()
            .append(
                    new KeyedCodec<>("Items", new Short2ObjectMapCodec<>(ItemStack.CODEC, Short2ObjectOpenHashMap::new, false)),
                    (o, i) -> o.items = i, (o) -> o.items)
            .add()
            .build()
            ;

    private String owner;
    private Long timeOfPlacement;

    private short capacity;
    private Short2ObjectMap<ItemStack> items;

    public static ComponentType<ChunkStore, GravestoneOwnerData> getComponentType() {
        return KMain.get().getGravestoneOwnerDataComponentType();
    }


    @Nullable
    public Component<ChunkStore> clone() {
        return new GravestoneOwnerData(owner, timeOfPlacement, capacity, items);
    }

}
