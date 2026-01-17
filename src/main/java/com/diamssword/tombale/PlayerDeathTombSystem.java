package com.diamssword.tombale;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockRotation;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.Rotation;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig;
import com.hypixel.hytale.server.core.command.commands.player.inventory.InventoryClearCommand;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.interaction.BlockPlaceUtils;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;

public class PlayerDeathTombSystem extends DeathSystems.OnDeathSystem {
	@Nonnull
	private static final Set<Dependency<EntityStore>> DEPENDENCIES = Set.of(new SystemDependency<>(Order.BEFORE, DeathSystems.DropPlayerDeathItems.class), new SystemDependency<>(Order.AFTER, DeathSystems.PlayerDropItemsConfig.class));

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
		assert playerComponent != null;
		if(playerComponent.getGameMode() != GameMode.Creative) {
			CombinedItemContainer combinedItemContainer = playerComponent.getInventory().getCombinedEverything();
			List<ItemStack> itemsToDrop = null;
			switch(component.getItemsLossMode()) {
				case ALL:
					itemsToDrop = playerComponent.getInventory().dropAllItemStacks();
					playerComponent.getInventory().clear();
					break;
				case CONFIGURED:
					double itemsAmountLossPercentage = component.getItemsAmountLossPercentage();
					if(itemsAmountLossPercentage > (double) 0.0F) {
						double itemAmountLossRatio = itemsAmountLossPercentage / (double) 100.0F;
						itemsToDrop = new ObjectArrayList();
						for(short i = 0; i < combinedItemContainer.getCapacity(); ++i) {
							ItemStack itemStack = combinedItemContainer.getItemStack(i);
							if(!ItemStack.isEmpty(itemStack) && itemStack.getItem().dropsOnDeath()) {
								int quantityToLose = Math.max(1, MathUtil.floor((double) itemStack.getQuantity() * itemAmountLossRatio));
								itemsToDrop.add(itemStack.withQuantity(quantityToLose));
								int newQuantity = itemStack.getQuantity() - quantityToLose;
								if(newQuantity > 0) {
									ItemStack updatedItemStack = itemStack.withQuantity(newQuantity);
									combinedItemContainer.replaceItemStackInSlot(i, itemStack, updatedItemStack);
								} else {
									combinedItemContainer.removeItemStackFromSlot(i);
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
						if(chunk != null && chunk.getState(emptyPos.x, emptyPos.y, emptyPos.z) instanceof ItemContainerState containerState) {
							if(!finalItemsToDrop.isEmpty()) {
								finalItemsToDrop.forEach(stack -> {
									containerState.getItemContainer().addItemStack(stack);
								});
								stored = true;
							}
						}
						playerComponent.sendMessage(Message.translation("server.tombale.gravePos").param("pos", emptyPos.x + " " + emptyPos.y + " " + emptyPos.z));
						Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

						ProjectileComponent projectileComponent = new ProjectileComponent("Projectile");
						holder.putComponent(ProjectileComponent.getComponentType(), projectileComponent);
						holder.putComponent(TransformComponent.getComponentType(), new TransformComponent(emptyPos.toVector3d().add(0.5, 1.2, 0.5), transf.getRotation().clone()));
						holder.ensureComponent(UUIDComponent.getComponentType());
						if(projectileComponent.getProjectile() == null) {
							projectileComponent.initialize();
						}
						holder.ensureComponent(Tombale.holoComponentType);
						holder.addComponent(Nameplate.getComponentType(), new Nameplate(playerComponent.getDisplayName()));
						holder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));
						world.getEntityStore().getStore().addEntity(holder, AddReason.SPAWN);

					}
					if(!stored) //Welp, can't find a place to set a grave, back to spilling everything I guess
					{
						HeadRotation headRotationComponent = store.getComponent(ref, HeadRotation.getComponentType());
						assert headRotationComponent != null;
						Vector3f headRotation = headRotationComponent.getRotation();
						Holder<EntityStore>[] drops = ItemComponent.generateItemDrops(store, finalItemsToDrop, pos.clone().add((double) 0.0F, (double) 1.0F, (double) 0.0F), headRotation);
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