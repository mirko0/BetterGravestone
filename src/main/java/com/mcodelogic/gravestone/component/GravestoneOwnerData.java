package com.mcodelogic.gravestone.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.mcodelogic.gravestone.KMain;
import lombok.*;

import javax.annotation.Nullable;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@ToString
public class GravestoneOwnerData implements Component<ChunkStore> {
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
            .build();

    private String owner;
    private Long timeOfPlacement;

    public static ComponentType<ChunkStore, GravestoneOwnerData> getComponentType() {
        return KMain.get().getGravestoneOwnerDataComponentType();
    }

    @Nullable
    public Component<ChunkStore> clone() {
        return new GravestoneOwnerData(owner, timeOfPlacement);
    }

}
