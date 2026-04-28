package com.diamssword.tombale;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.joml.Vector3d;

public class Hologram implements Component<EntityStore> {
	public static final BuilderCodec<Hologram> CODEC = BuilderCodec.builder(Hologram.class, Hologram::new).build();

	public Hologram(String messsage, Vector3d position, int rotation) {
	}

	public Hologram() {

	}

	@NullableDecl
	@Override
	public Component<EntityStore> clone() {
		Hologram holo = new Hologram();
		return holo;
	}
}
