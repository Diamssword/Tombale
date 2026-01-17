package com.diamssword.tombale;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.builtin.crafting.interaction.OpenBenchPageInteraction;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.events.AllWorldsLoadedEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * This class serves as the entrypoint for your plugin. Use the setup method to register into game registries or add
 * event listeners.
 */
public class Tombale extends JavaPlugin {

	public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	public static ComponentType<EntityStore, Hologram> holoComponentType;

	public Tombale(@Nonnull JavaPluginInit init) {
		super(init);
	}

	@Override
	protected void setup() {
		LOGGER.atInfo().log("The dead deserve a proper Burial");

		AssetRegistry.getAssetStore(Interaction.class)
				.loadAssets(
						this.getName(),
						List.of(CollectGraveInteraction.GRAVE_INTERACT)
				);
		AssetRegistry.getAssetStore(RootInteraction.class)
				.loadAssets(
						getName(), List.of(CollectGraveInteraction.GRAVE_ROOT)
				);
		this.getCodecRegistry(Interaction.CODEC).register("CollectGrave", CollectGraveInteraction.class, CollectGraveInteraction.CODEC);
		this.getEntityStoreRegistry().registerSystem(new PlayerDeathTombSystem());
		holoComponentType = this.getEntityStoreRegistry().registerComponent(Hologram.class, Hologram::new);
		this.getEntityStoreRegistry().registerSystem(new HologramSystem());
	}
}