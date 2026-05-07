package com.diamssword.tombale;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.BlockRotation;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.Rotation;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig;
import com.hypixel.hytale.server.core.asset.type.gameplay.GameplayConfig;
import com.hypixel.hytale.server.core.asset.type.gameplay.WorldMapConfig;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.entity.entities.player.data.PlayerWorldData;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.worldmap.markers.user.UserMapMarker;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;

public class PlayerDeathTombSystem extends DeathSystems.OnDeathSystem {
	@Nonnull
	private static final Set<Dependency<EntityStore>> DEPENDENCIES = Set.of(new SystemDependency<>(Order.BEFORE, DeathSystems.DropPlayerDeathItems.class), new SystemDependency<>(Order.AFTER, DeathSystems.PlayerDropItemsConfig.class), new SystemDependency<>(Order.AFTER, DeathSystems.PlayerDeathMarker.class));

	@Nonnull
	@Override
	public Query<EntityStore> getQuery() {
		return Archetype.of(Player.getComponentType(), TransformComponent.getComponentType());
	}

	@Nonnull
	@Override
	public Set<Dependency<EntityStore>> getDependencies() {
		return DEPENDENCIES;
	}

	@Override
	public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent component, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
		Player playerComponent = store.getComponent(ref, Player.getComponentType());
		PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
		assert playerComponent != null;
		assert playerRefComponent != null;
		var config = Tombale.instance.getConfig().get();
		if(playerComponent.getGameMode() != GameMode.Creative && config.isWorldAllowed(playerComponent.getWorld().getName())) {

			CombinedItemContainer combinedInventoryComponent = InventoryComponent.getCombined(commandBuffer, ref, InventoryComponent.EVERYTHING);
			List<ItemStack> itemsToDrop = null;
			switch(component.getItemsLossMode()) {
				case ALL:
					itemsToDrop = combinedInventoryComponent.dropAllItemStacks();
					combinedInventoryComponent.clear();
					break;
				case CONFIGURED:
					double itemsAmountLossPercentage = component.getItemsAmountLossPercentage();
					if(itemsAmountLossPercentage > (double) 0.0F) {
						double itemAmountLossRatio = itemsAmountLossPercentage / (double) 100.0F;
						itemsToDrop = new ObjectArrayList<>();
						for(short i = 0; i < combinedInventoryComponent.getCapacity(); ++i) {
							ItemStack itemStack = combinedInventoryComponent.getItemStack(i);
							if(!ItemStack.isEmpty(itemStack) && itemStack.getItem().dropsOnDeath()) {
								int quantityToLose = Math.max(1, MathUtil.floor((double) itemStack.getQuantity() * itemAmountLossRatio));
								itemsToDrop.add(itemStack.withQuantity(quantityToLose));
								int newQuantity = itemStack.getQuantity() - quantityToLose;
								if(newQuantity > 0) {
									ItemStack updatedItemStack = itemStack.withQuantity(newQuantity);
									combinedInventoryComponent.replaceItemStackInSlot(i, itemStack, updatedItemStack);
								} else {
									combinedInventoryComponent.removeItemStackFromSlot(i);
								}
							}
						}
					}
				case NONE:
			}
			component.setItemsLossMode(DeathConfig.ItemsLossMode.NONE);
			var transf = store.getComponent(ref, TransformComponent.getComponentType());
			assert transf != null;
			var pos = transf.getTransform().getPosition();
			var world = playerComponent.getWorld();
			if(itemsToDrop != null) {
				List<ItemStack> finalItemsToDrop = itemsToDrop;
				component.setItemsLostOnDeath(itemsToDrop);
				world.execute(() -> {
					var emptyPos = findEmptyPlace(world, new Vector3i((int) transf.getPosition().x, (int) transf.getPosition().y, (int) transf.getPosition().z), 5);
					boolean stored = false;

					if(emptyPos != null && setBlockWithRotation(world, emptyPos.x, emptyPos.y, emptyPos.z, "Tombale_Tombstone", getRandomCardinalIndex())) {

						WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(emptyPos.x, emptyPos.z));
						if(chunk != null) {
							var container = chunk.getBlockComponentEntity(emptyPos.x, emptyPos.y, emptyPos.z);
							if(container != null) {
								var containerState = world.getChunkStore().getStore().getComponent(container, ItemContainerBlock.getComponentType());
								if(containerState != null) {
									if(!finalItemsToDrop.isEmpty()) {
										finalItemsToDrop.forEach(stack -> containerState.getItemContainer().addItemStack(stack));
										stored = true;
									}
								}
							}
						}
						if(config.isEnableChatMessageOnDeath())
							playerRefComponent.sendMessage(Message.translation("Tombale.message.death").param("pos", emptyPos.x + " " + emptyPos.y + " " + emptyPos.z));
						if(config.isEnableTombMarker()) {
							var markerC = store.ensureAndGetComponent(ref, Tombale.markerComponentType);
							GameplayConfig gameplayConfig = world.getGameplayConfig();
							WorldMapConfig worldMapConfigGameplayConfig = gameplayConfig.getWorldMapConfig();
							PlayerWorldData perWorldData = playerComponent.getPlayerConfigData().getPerWorldData(world.getName());
							if(worldMapConfigGameplayConfig.isDisplayDeathMarker()) {
								perWorldData.getDeathPositions().removeLast();
								//	playerComponent.getPlayerConfigData().markChanged();
							}
							WorldTimeResource worldTimeResource = commandBuffer.getResource(WorldTimeResource.getResourceType());
							Instant gameTime = worldTimeResource.getGameTime();
							int daysSinceWorldStart = (int) WorldTimeResource.ZERO_YEAR.until(gameTime, ChronoUnit.DAYS);
							markerC.addLastGrave(world.getName(), ref, emptyPos, daysSinceWorldStart);


						}
						Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

						ProjectileComponent projectileComponent = new ProjectileComponent("Projectile");
						holder.putComponent(ProjectileComponent.getComponentType(), projectileComponent);
						holder.putComponent(TransformComponent.getComponentType(), new TransformComponent(new Vector3d(emptyPos).add(0.5, 1.2, 0.5), transf.getRotation().clone()));
						holder.ensureComponent(UUIDComponent.getComponentType());
						if(projectileComponent.getProjectile() == null) {
							projectileComponent.initialize();
						}
						holder.ensureComponent(Tombale.holoComponentType);
						holder.addComponent(Nameplate.getComponentType(), new Nameplate(playerRefComponent.getUsername()));
						holder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));
						world.getEntityStore().getStore().addEntity(holder, AddReason.SPAWN);

					}
					if(!stored) //Welp, can't find a place to set a grave, back to spilling everything I guess
					{
						HeadRotation headRotationComponent = store.getComponent(ref, HeadRotation.getComponentType());
						assert headRotationComponent != null;
						Rotation3f headRotation = headRotationComponent.getRotation();
						Holder<EntityStore>[] drops = ItemComponent.generateItemDrops(store, finalItemsToDrop, new Vector3d(pos).add(0.0, 1.0, 0.0), headRotation);
						commandBuffer.addEntities(drops, AddReason.SPAWN);
					}
				});
			}


		}
	}

	public static Vector3i findEmptyPlace(World w, Vector3i position, int radius) {
		if(w.getBlock(position.x, position.y, position.z) == 0) {
			return new Vector3i(position.x, position.y, position.z);
		}

		// Recherche en cube croissant
		for(int r = 1; r <= radius; r++) {
			for(int dx = -r; dx <= r; dx++) {
				for(int dy = -r; dy <= r; dy++) {
					for(int dz = -r; dz <= r; dz++) {
						int x = position.x + dx;
						int y = position.y + dy;
						int z = position.z + dz;
						if(w.getBlock(x, y, z) == 0) {
							return new Vector3i(x, y, z); // Trouvé !
						}
					}
				}
			}
		}

		// Rien trouvé
		return null;
	}

	public static boolean setBlockWithRotation(World world, int x, int y, int z, String blockName, int rotationIndex) {
		final WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(x, z));
		int index = BlockType.getAssetMap().getIndex(blockName);
		if(index == Integer.MIN_VALUE) {
			throw new IllegalArgumentException("Unknown key! " + blockName);
		} else {

			return chunk.setBlock(x, y, z, index, BlockType.getAssetMap().getAsset(index), rotationIndex, 0, 0);
		}
	}

	public static int getRandomCardinalIndex() {
		var rotation = new BlockRotation(Rotation.values()[(int) (Math.random() * Rotation.values().length)], Rotation.None, Rotation.None);
		RotationTuple targetRotation = RotationTuple.of(
				com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation.valueOf(rotation.rotationYaw), com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation.valueOf(rotation.rotationPitch), com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation.valueOf(rotation.rotationRoll)
		);
		return targetRotation.index();
	}
}