package com.diamssword.tombale;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3iUtil;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.worldmap.markers.user.UserMapMarker;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.bson.BsonDocument;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TombaleMarkerComponent implements Component<EntityStore> {
	public static final BuilderCodec<TombaleMarkerComponent> CODEC = BuilderCodec.builder(TombaleMarkerComponent.class, TombaleMarkerComponent::new)
			.append(new KeyedCodec<>("Markers", new MapCodec<>(new ArrayCodec<>(GraveMarkerPos.CODEC, GraveMarkerPos[]::new), ConcurrentHashMap::new, false)),
					(config, perWorldData) -> config.markers = perWorldData,
					config -> config.markers).add()
			.build();

	private Map<String, GraveMarkerPos[]> markers = new ConcurrentHashMap<>();

	@Override
	public Component<EntityStore> clone() {
		BsonDocument document = CODEC.encode(this, ExtraInfo.THREAD_LOCAL.get()).asDocument();
		var t = new TombaleMarkerComponent();
		CODEC.decode(document, t, ExtraInfo.THREAD_LOCAL.get());
		return t;
	}

	@Nonnull
	public GraveMarkerPos[] getPerWorldData(@Nonnull String worldName) {
		return this.markers.computeIfAbsent(worldName, s -> new GraveMarkerPos[0]);
	}

	@Nonnull
	public List<GraveMarkerPos> getGravesPositions(String worldName) {
		return ObjectArrayList.wrap(getPerWorldData(worldName));
	}

	public void addLastGrave(@Nonnull String worldName, Ref<EntityStore> player, Vector3i pos, int deathDay) {
		var pref = player.getStore().getComponent(player, PlayerRef.getComponentType());
		var playerC = player.getStore().getComponent(player, Player.getComponentType());
		DisplayNameComponent displayNameComponent = player.getStore().getComponent(player, DisplayNameComponent.getComponentType());
		var deathPositions = getGravesPositions(worldName);
		var marker = new UserMapMarker();
		marker.setPosition(pos.x + 0.5f, pos.z + 0.5f);
		marker.setId("tombale-grave-marker-" + UUID.randomUUID());
		marker.setIcon("Death.png");
		marker.setColorTint(new Color((byte) 255, (byte) 100, (byte) 100));
		marker.withCreatedByUuid(pref.getUuid());
		marker.withCreatedByName(displayNameComponent.getDisplayName().getRawText());
		String key = "Tombale.marker.death";

		if(Tombale.instance.getConfig().get().isEnableTombMarkerCoordinate())
			key = "Tombale.marker.death.pos";
		var translated = I18nModule.get().getMessage(pref.getLanguage(), key);
		translated = translated.replaceAll("\\{day}", deathDay + "");
		if(Tombale.instance.getConfig().get().isEnableTombMarkerCoordinate())
			translated = translated.replaceAll("\\{pos}", pos.x + "," + pos.y + "," + pos.z);
		marker.setName(translated);
		var worldConf = playerC.getPlayerConfigData().getPerWorldData(worldName);
		worldConf.addUserMapMarker(marker);
		deathPositions.add(new GraveMarkerPos(marker.getId(), pos));

		while(deathPositions.size() > 5) {
			var rem = deathPositions.removeFirst();
			worldConf.removeUserMapMarker(rem.markerID);
		}
		this.markers.put(worldName, deathPositions.toArray(new GraveMarkerPos[0]));
	}

	public boolean removeGraveAt(String worldName, Ref<EntityStore> player, Vector3i blockpos) {
		var deathPositions = getGravesPositions(worldName);
		var found = deathPositions.stream().filter(deathPosition -> blockpos.equals(deathPosition.position));
		return found.findAny().map(removed -> {
			deathPositions.remove(removed);
			var playerC = player.getStore().getComponent(player, Player.getComponentType());
			playerC.getPlayerConfigData().getPerWorldData(worldName).removeUserMapMarker(removed.markerID);
			this.markers.put(worldName, deathPositions.toArray(new GraveMarkerPos[0]));
			return true;
		}).orElse(false);
	}

	public static class GraveMarkerPos {
		@Nonnull
		public static final BuilderCodec<GraveMarkerPos> CODEC = BuilderCodec.builder(GraveMarkerPos.class, GraveMarkerPos::new)
				.append(new KeyedCodec<>("MarkerId", Codec.STRING), (data, value) -> data.markerID = value, data -> data.markerID)
				.documentation("The unique ID of the associated map marker.")
				.add()
				.append(new KeyedCodec<>("Position", Vector3iUtil.CODEC), (data, value) -> data.position = value, data -> data.position)
				.documentation("The transform of this death position.")
				.add()
				.build();
		public String markerID;
		public Vector3i position;

		public GraveMarkerPos() {

		}

		public GraveMarkerPos(String id, Vector3i pos) {
			this.markerID = id;
			this.position = pos;
		}
	}
}
