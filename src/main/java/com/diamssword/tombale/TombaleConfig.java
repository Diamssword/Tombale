package com.diamssword.tombale;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class TombaleConfig {
	public static final BuilderCodec<TombaleConfig> CODEC = BuilderCodec.builder(TombaleConfig.class, TombaleConfig::new)
			.append(new KeyedCodec<>("WorldsList", new ArrayCodec<>(Codec.STRING, String[]::new)),
					(config, value) -> config.worldlist = value, // Setter
					(config) -> config.worldlist).documentation("A list of world to whitelist or blacklist the tombs from spawning, support '*' as a wildcard").add()
			.append(new KeyedCodec<>("IsWorldListWhitelist", Codec.BOOLEAN),
					(config, value) -> config.isWhiteList = value,
					(config) -> config.isWhiteList).documentation("Is the list above a whitelist or a blacklist").add()
			.append(new KeyedCodec<>("EnableTombMarker", Codec.BOOLEAN),
					(config, value) -> config.enableTombMarker = value,
					(config) -> config.enableTombMarker).add()
			.append(new KeyedCodec<>("EnableTombMarkerCoordinate", Codec.BOOLEAN),
					(config, value) -> config.enableTombMarkerCoordinate = value,
					(config) -> config.enableTombMarkerCoordinate).add()
			.append(new KeyedCodec<>("EnableChatMessageOnDeath", Codec.BOOLEAN),
					(config, value) -> config.enableChatMessageOnDeath = value,
					(config) -> config.enableChatMessageOnDeath).add()
			.afterDecode(TombaleConfig::encodeRegex)
			.build();


	private String[] worldlist = new String[0];
	private boolean isWhiteList = false;
	private boolean enableTombMarker = true;
	private boolean enableTombMarkerCoordinate = true;
	private boolean enableChatMessageOnDeath = false;
	private List<Pattern> patterns;

	public TombaleConfig() {
	}

	public boolean isEnableTombMarker() {
		return enableTombMarker;
	}

	public boolean isEnableTombMarkerCoordinate() {
		return enableTombMarkerCoordinate;
	}

	public boolean isEnableChatMessageOnDeath() {
		return enableChatMessageOnDeath;
	}

	public boolean isWorldAllowed(String worldName) {
		var match = patterns.stream().anyMatch(p -> p.matcher(worldName).matches());
		return isWhiteList == match;
	}

	private static void encodeRegex(TombaleConfig cfg) {
		cfg.patterns = Arrays.stream(cfg.worldlist)
				.map(TombaleConfig::wildcardToRegex)
				.map(Pattern::compile)
				.toList();
	}

	private static String wildcardToRegex(String wildcard) {
		return "^" +
				wildcard
						.replace(".", "\\.")
						.replace("*", ".*")
						.replace("?", ".")
				+ "$";
	}
}