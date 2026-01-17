package com.diamssword.tombale;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class HologramSystem extends EntityTickingSystem<EntityStore> {

	@Nonnull
	@Override
	public Query<EntityStore> getQuery() {
		return Query.and(Tombale.holoComponentType, TransformComponent.getComponentType());
	}

	@Nullable
	public SystemGroup<EntityStore> getGroup() {
		return EntityTrackerSystems.QUEUE_UPDATE_GROUP;
	}

	public boolean isParallel(int archetypeChunkSize, int taskCount) {
		return EntityTickingSystem.maybeUseParallel(archetypeChunkSize, taskCount);
	}

	@Override
	public void tick(float dt, int index, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {
		TransformComponent transformComponent = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
		assert transformComponent != null;

		store.getExternalData().getWorld().execute(() -> {
			var vec = transformComponent.getPosition().toVector3i();
			var block = commandBuffer.getExternalData().getWorld().getBlock(vec);
			if(BlockType.getAssetMap().getIndex("Tombale_Tombstone") != block) {
				store.removeEntity(archetypeChunk.getReferenceTo(index), RemoveReason.REMOVE);
			}
		});

	}
}
