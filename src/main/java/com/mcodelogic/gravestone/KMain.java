package com.mcodelogic.gravestone;

import com.hypixel.hytale.component.ComponentRegistry;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.util.Config;
import com.mcodelogic.gravestone.component.GravestoneOwnerData;
import com.mcodelogic.gravestone.config.KConfig;
import com.mcodelogic.gravestone.interaction.BreakGravestoneInteraction;
import com.mcodelogic.gravestone.interaction.UseGravestoneInteraction;
import com.mcodelogic.gravestone.listener.DropItemsOnDeathListener;
import lombok.Getter;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.io.File;
import java.lang.reflect.Field;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;


public class KMain extends JavaPlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.getLogger();
    private final Config<KConfig> pluginConfiguration;

    private DropItemsOnDeathListener deathEventListener;
    private static KMain instance;
    @Getter
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public static KMain get() {
        return instance;
    }

    @Getter
    private ComponentType<ChunkStore, GravestoneOwnerData> gravestoneOwnerDataComponentType;

    public KMain(@NonNullDecl JavaPluginInit init) {
        super(init);
        instance = this;
        pluginConfiguration = this.withConfig(Constants.PLUGIN_NAME_FULL, KConfig.CODEC);
    }

    @Override
    protected void setup() {
        super.setup();
        LOGGER.at(Level.INFO).log("Initializing " + Constants.PLUGIN_NAME_FULL + " Plugin...");
        try {
            if (!new File(getDataDirectory().resolve(Constants.PLUGIN_NAME_FULL + ".json").toUri()).exists()) {
                LOGGER.at(Level.INFO).log("Creating default configuration");
                pluginConfiguration.save();
            }
            pluginConfiguration.load();

            if (pluginConfiguration.get().getConfigVersion() == null) {
                pluginConfiguration.get().setConfigVersion("1.0");
                pluginConfiguration.get().setIgnoreTime(false);
                pluginConfiguration.get().setIgnoreTimePermission("gravestone.ignore.time");
                pluginConfiguration.save();
            }

            LOGGER.at(Level.INFO).log("Configuration loaded");
        } catch (Exception e) {
            Logger.getLogger(KMain.class.getName()).log(Level.SEVERE, "Error while loading configuration", e);
        }
        this.getCodecRegistry(Interaction.CODEC).register("BreakGravestoneInteraction", BreakGravestoneInteraction.class, BreakGravestoneInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("UseGravestoneInteraction", UseGravestoneInteraction.class, UseGravestoneInteraction.CODEC);
        this.gravestoneOwnerDataComponentType = this.getChunkStoreRegistry().registerComponent(GravestoneOwnerData.class, "GravestoneOwnerData", GravestoneOwnerData.CODEC);
        this.deathEventListener = new DropItemsOnDeathListener(this);
        getEntityStoreRegistry().registerSystem(deathEventListener);
        disableDefaultDrops();
    }

    private void disableDefaultDrops() {
        try {
            ComponentRegistryProxy proxy = getEntityStoreRegistry();
            Field field = proxy.getClass().getDeclaredField("registry");
            field.setAccessible(true);
            ((ComponentRegistry) field.get(proxy)).unregisterSystem(DeathSystems.DropPlayerDeathItems.class);
        } catch (Exception e) {
            getLogger().atWarning().log("Unable to disable default drop system! Error: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @Override
    protected void start() {
        LOGGER.at(Level.INFO).log("Starting " + Constants.PLUGIN_NAME_FULL + " Plugin...");
    }

    @Override
    protected void shutdown() {
        LOGGER.at(Level.INFO).log("Stopping " + Constants.PLUGIN_NAME_FULL + " Plugin...");
    }

    public KConfig getConfiguration() {
        return pluginConfiguration.get();
    }
}
