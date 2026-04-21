package com.donghan.farmhud.mixin;

import com.donghan.farmhud.FarmHudClient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.SetPlayerInventoryS2CPacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("HEAD"))
    private void farmhud$onScreenHandlerSlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!client.isOnThread()) return;
        try {
            FarmHudClient.TRACKER.handleSlotUpdate(packet, client);
        } catch (Throwable t) {
            FarmHudClient.LOGGER.error("[FarmHud] onScreenHandlerSlotUpdate hook 실패", t);
        }
    }

    @Inject(method = "onInventory", at = @At("HEAD"))
    private void farmhud$onInventory(InventoryS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!client.isOnThread()) return;
        try {
            FarmHudClient.TRACKER.handleFullInventory(packet, client);
        } catch (Throwable t) {
            FarmHudClient.LOGGER.error("[FarmHud] onInventory hook 실패", t);
        }
    }

    @Inject(method = "onSetPlayerInventory", at = @At("HEAD"))
    private void farmhud$onSetPlayerInventory(SetPlayerInventoryS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!client.isOnThread()) return;
        try {
            FarmHudClient.TRACKER.handlePlayerInventoryUpdate(packet, client);
        } catch (Throwable t) {
            FarmHudClient.LOGGER.error("[FarmHud] onSetPlayerInventory hook 실패", t);
        }
    }
}
