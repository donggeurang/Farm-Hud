package com.donghan.farmhud;

import com.donghan.farmhud.config.ConfigManager;
import com.donghan.farmhud.config.FarmHudConfig;
import com.donghan.farmhud.harvest.DebugInfo;
import com.donghan.farmhud.harvest.HarvestTracker;
import com.donghan.farmhud.hud.HudRenderer;
import com.donghan.farmhud.keybinds.KeyBindings;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FarmHudClient implements ClientModInitializer {
	public static final String MOD_ID = "farmhud";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static FarmHudConfig  CONFIG;
	public static HarvestTracker TRACKER;
	public static DebugInfo      DEBUG;

	@Override
	public void onInitializeClient() {
		CONFIG  = ConfigManager.load();
		DEBUG   = new DebugInfo();
		TRACKER = new HarvestTracker(CONFIG, DEBUG);

		KeyBindings.register();
		HudRenderer.register();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			TRACKER.tick(client);
			KeyBindings.handleInput(client);
		});

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			TRACKER.onWorldJoin();
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			TRACKER.onWorldLeave();
		});

		Runtime.getRuntime().addShutdownHook(new Thread(() -> ConfigManager.save(CONFIG)));

		LOGGER.info("[FarmHud] 초기화 완료");
	}
}