package com.diamssword.tombale;

import com.hypixel.hytale.builtin.crafting.interaction.OpenBenchPageInteraction;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CollectGraveInteraction extends SimpleBlockInteraction {
	public static final BuilderCodec<CollectGraveInteraction> CODEC = BuilderCodec.builder(
					CollectGraveInteraction.class, CollectGraveInteraction::new, SimpleBlockInteraction.CODEC
			)
			.build();
	public static final CollectGraveInteraction GRAVE_INTERACT = new CollectGraveInteraction("CollectGrave");
	public static final RootInteraction GRAVE_ROOT = new RootInteraction(GRAVE_INTERACT.getId(), GRAVE_INTERACT.getId());

	public CollectGraveInteraction() {
		super();
	}

	public CollectGraveInteraction(String collectGrave) {
		super(collectGrave);
	}

	@Override
	protected void interactWithBlock(
			@Nonnull World world,
			@Nonnull CommandBuffer<EntityStore> commandBuffer,
			@Nonnull InteractionType type,
			@Nonnull InteractionContext context,
			@Nullable ItemStack itemInHand,
			@Nonnull Vector3i pos,
			@Nonnull CooldownHandler cooldownHandler
	) {
		Ref<EntityStore> ref = context.getEntity();
		Store<EntityStore> store = ref.getStore();
		Player playerComponent = commandBuffer.getComponent(ref, Player.getComponentType());
		if(playerComponent != null) {

			var container = world.getChunk(ChunkUtil.indexChunkFromBlock(pos.x, pos.z)).getBlockComponentEntity(pos.x, pos.y, pos.z);
			if(container != null) {
				var itemContainerState = world.getChunkStore().getStore().getComponent(container, ItemContainerBlock.getComponentType());
				if(itemContainerState != null) {

					world.execute(() -> {
						CombinedItemContainer combinedItemContainer = playerComponent.getInventory().getCombinedEverything();
						var containerS = itemContainerState.getItemContainer();
						for(short i = 0; i < containerS.getCapacity(); i++) {
							var stack = containerS.getItemStack(i);
							if(stack != null) {
								containerS.setItemStackForSlot(i, combinedItemContainer.addItemStack(stack).getRemainder());
							}
						}
						if(!containerS.isEmpty()) {
							world.execute(() -> {
								HeadRotation headRotationComponent = store.getComponent(ref, HeadRotation.getComponentType());
								assert headRotationComponent != null;
								Vector3f headRotation = headRotationComponent.getRotation();
								Holder<EntityStore>[] drops = ItemComponent.generateItemDrops(store, containerS.dropAllItemStacks(), pos.clone().add(0, 1, 0).toVector3d(), headRotation);
								for(Holder<EntityStore> drop : drops) {
									world.getEntityStore().getStore().addEntity(drop, AddReason.SPAWN);
								}
							});

						}
						world.breakBlock(pos.x, pos.y, pos.z, 0);
					});


				} else {
					playerComponent.sendMessage(
							Message.translation("server.interactions.invalidBlockState")
									.param("interaction", this.getClass().getSimpleName())
									.param("blockState", container != null ? container.getClass().getSimpleName() : "null")
					);
				}
			}

		}
	}

	@Override
	protected void simulateInteractWithBlock(
			@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nullable ItemStack itemInHand, @Nonnull World world, @Nonnull Vector3i targetBlock
	) {
	}
}
