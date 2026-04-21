package com.donghan.farmhud.keybinds;

import com.donghan.farmhud.FarmHudClient;
import com.donghan.farmhud.config.ConfigManager;
import com.donghan.farmhud.ui.ConfigScreen;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    public static KeyBinding TOGGLE_HUD;
    public static KeyBinding RESET_SESSION;
    public static KeyBinding OPEN_CONFIG;

    public static void register() {
        TOGGLE_HUD = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.farmhud.toggle_hud", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H, "category.farmhud"));

        RESET_SESSION = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.farmhud.reset_session", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, "category.farmhud"));

        OPEN_CONFIG = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.farmhud.open_config", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K, "category.farmhud"));
    }

    public static void handleInput(MinecraftClient client) {
        while (TOGGLE_HUD.wasPressed()) {
            FarmHudClient.CONFIG.enabled = !FarmHudClient.CONFIG.enabled;
            ConfigManager.save(FarmHudClient.CONFIG);
        }
        while (RESET_SESSION.wasPressed()) {
            FarmHudClient.TRACKER.resetSession();
        }
        while (OPEN_CONFIG.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new ConfigScreen(null));
            }
        }
    }
}
