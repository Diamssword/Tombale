package com.diamssword.tombale;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class Hologram implements Component<EntityStore> {
	public static final BuilderCodec<Hologram> CODEC = BuilderCodec.builder(Hologram.class, Hologram::new).build();
	public String messsage;
	public Vector3d position;
	public int rotation;

	public Hologram(String messsage, Vector3d position, int rotation) {
		this.messsage = messsage;
		this.rotation = rotation;
		this.position = position;
	}

	public Hologram() {

	}

	@NullableDecl
	@Override
	public Component<EntityStore> clone() {
		Hologram holo = new Hologram();
		holo.messsage = this.messsage;
		holo.position = this.position;
		holo.rotation = this.rotation;
		return holo;
	}
}
