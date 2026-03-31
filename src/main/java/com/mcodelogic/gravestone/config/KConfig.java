package com.mcodelogic.gravestone.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import lombok.Getter;
import lombok.Setter;

@Getter
public class KConfig {

    public static final BuilderCodec<KConfig> CODEC = BuilderCodec.builder(KConfig.class, KConfig::new)

            .append(new KeyedCodec<Integer>("TimeToCollectGraveInSeconds", Codec.INTEGER),
                    (config, value, extra) -> config.timeToCollectGrave = value,
                    (config, extra) -> config.timeToCollectGrave)
            .add()

            .append(new KeyedCodec<Boolean>("SendDeathLocation", Codec.BOOLEAN),
                    (config, value, extra) -> config.sendLocation = value,
                    (config, extra) -> config.sendLocation)
            .add()


            .append(new KeyedCodec<String>("MessageDeathLocation", Codec.STRING),
                    (config, value, extra) -> config.sendLocationMessage = value,
                    (config, extra) -> config.sendLocationMessage)
            .add()

            .append(new KeyedCodec<String>("MessageGraveExpired", Codec.STRING),
                    (config, value, extra) -> config.messageGravestoneItemsExpired = value,
                    (config, extra) -> config.messageGravestoneItemsExpired)
            .add()

            .append(new KeyedCodec<String>("MessageGraveNotOwner", Codec.STRING),
                    (config, value, extra) -> config.messageNotOwner = value,
                    (config, extra) -> config.messageNotOwner)
            .add()

            .append(new KeyedCodec<String>("BreakOthersPermission", Codec.STRING),
                    (config, value, extra) -> config.breakOthersPermission = value,
                    (config, extra) -> config.breakOthersPermission)
            .add()


            .append(new KeyedCodec<Boolean>("IgnoreTimeToCollect", Codec.BOOLEAN),
                    (config, value, extra) -> config.ignoreTime = value,
                    (config, extra) -> config.ignoreTime)
            .add()

            .append(new KeyedCodec<String>("IgnoreTimePermission", Codec.STRING),
                    (config, value, extra) -> config.ignoreTimePermission = value,
                    (config, extra) -> config.ignoreTimePermission)
            .add()

            .append(new KeyedCodec<String>("ConfigVersion", Codec.STRING),
                    (config, value, extra) -> config.configVersion = value,
                    (config, extra) -> config.configVersion)
            .add()

            .build();


    private String breakOthersPermission = "gravestone.break.others";
    private boolean sendLocation = true;
    private Integer timeToCollectGrave = 1800;
    private String sendLocationMessage = "<color:red>You have died at</color><color:white>:</color> <color:gray>{x}, {y}, {z}</color><color:white>.</color>";

    private String messageGravestoneItemsExpired = "<color:red>This gravestone has been placed for a long time</color><color:white>.</color> <color:red>Your items have rotted away</color><color:white>.</color>";
    private String messageNotOwner = "<color:red>You are not the owner of this gravestone</color><color:white>.</color> <color:gray> You can break this gravestone in </color><color:white>{time}</color> <color:gray>seconds</color><color:white>.</color>";

    @Setter
    private Boolean ignoreTime = false;

    @Setter
    private String ignoreTimePermission = "gravestone.ignore.time";

    @Setter
    private String configVersion;

    public KConfig() {
    }


}