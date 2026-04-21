package com.donghan.farmhud.harvest;

import com.donghan.farmhud.FarmHudClient;
import com.donghan.farmhud.config.FarmHudConfig;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.SetPlayerInventoryS2CPacket;
import net.minecraft.screen.PlayerScreenHandler;

import java.util.List;

public class HarvestTracker {
    private final FarmHudConfig    config;
    private final DebugInfo        debug;
    private final CropFilter       filter;
    private final InventoryMirror  mirror = new InventoryMirror();

    private SessionState state          = SessionState.IDLE;
    private long         sessionCount   = 0;
    private long         accumActiveMs  = 0;
    private long         activeStartMs  = 0;
    private long         lastHarvestMs  = 0;

    private boolean      mirrorInitialized = false;

    public HarvestTracker(FarmHudConfig config, DebugInfo debug) {
        this.config = config;
        this.debug  = debug;
        this.filter = new CropFilter(config);
    }

    public void onWorldJoin() {
        mirror.clear();
        mirrorInitialized = false;
    }

    public void onWorldLeave() {
        if (state == SessionState.ACTIVE) {
            long now = System.currentTimeMillis();
            accumActiveMs += Math.max(0, now - activeStartMs);
            activeStartMs  = 0;
            state = SessionState.PAUSED;
        }
        mirror.clear();
        mirrorInitialized = false;
    }

    public void tick(MinecraftClient client) {
        if (!mirrorInitialized && client.player != null) {
            initMirrorFromPlayer(client.player.getInventory());
            mirrorInitialized = true;
        }

        if (state == SessionState.ACTIVE) {
            long now = System.currentTimeMillis();
            if ((now - lastHarvestMs) > config.pauseSeconds * 1000L) {
                accumActiveMs += Math.max(0, now - activeStartMs);
                activeStartMs  = 0;
                state = SessionState.PAUSED;
            }
        }
    }

    private void initMirrorFromPlayer(PlayerInventory inv) {
        for (int i = 0; i <= 35; i++) {
            ItemStack s = inv.getStack(i);
            mirror.put(i, InventoryMirror.fromStack(s, filter));
        }
    }

    public synchronized void handleSlotUpdate(ScreenHandlerSlotUpdateS2CPacket p, MinecraftClient client) {
        if (!config.enabled || client.player == null) return;

        int syncId = p.getSyncId();
        if (syncId != 0) {
            return;
        }

        if (config.onlyCountWhenNoScreen && isBlockingScreenOpen(client.currentScreen)) {
            return;
        }

        Integer playerSlot = mapPlayerScreenHandlerToInvSlot(p.getSlot());
        if (playerSlot == null) return;

        processSlotChange("SlotUpdate", playerSlot, p.getStack());
    }

    public synchronized void handlePlayerInventoryUpdate(SetPlayerInventoryS2CPacket p, MinecraftClient client) {
        if (!config.enabled || client.player == null) return;

        int slot = p.slot();
        if (config.onlyCountWhenNoScreen && isBlockingScreenOpen(client.currentScreen)) {
            return;
        }

        if (slot < 0 || slot > 35) return;

        processSlotChange("SetPlayerInv", slot, p.contents());
    }

    public synchronized void handleFullInventory(InventoryS2CPacket p, MinecraftClient client) {
        if (!config.enabled || client.player == null) return;
        if (p.getSyncId() != 0) return;

        List<ItemStack> contents = p.getContents();

        if (!mirrorInitialized) {
            for (int shSlot = 0; shSlot < contents.size(); shSlot++) {
                Integer invSlot = mapPlayerScreenHandlerToInvSlot(shSlot);
                if (invSlot == null) continue;
                mirror.put(invSlot, InventoryMirror.fromStack(contents.get(shSlot), filter));
            }
            mirrorInitialized = true;
            debug.lastSource  = "FullInv(init)";
            debug.lastReason  = "initialized";
            return;
        }

        if (config.onlyCountWhenNoScreen && isBlockingScreenOpen(client.currentScreen)) {
            for (int shSlot = 0; shSlot < contents.size(); shSlot++) {
                Integer invSlot = mapPlayerScreenHandlerToInvSlot(shSlot);
                if (invSlot == null) continue;
                mirror.put(invSlot, InventoryMirror.fromStack(contents.get(shSlot), filter));
            }
            return;
        }

        for (int shSlot = 0; shSlot < contents.size(); shSlot++) {
            Integer invSlot = mapPlayerScreenHandlerToInvSlot(shSlot);
            if (invSlot == null) continue;
            processSlotChange("FullInv", invSlot, contents.get(shSlot));
        }
    }

    private int processSlotChange(String source, int invSlot, ItemStack newStack) {
        InventoryMirror.Entry oldE = mirror.get(invSlot);
        InventoryMirror.Entry newE = InventoryMirror.fromStack(newStack, filter);

        int     delta     = 0;
        boolean countable = false;
        String  reason    = "none";

        String  selected      = FarmHudClient.CONFIG.selectedCrop;
        boolean newIsSelected = newE.itemId.equals(selected);

        if (oldE == null) {
            reason = "first-observe";
        } else if (newE.itemId.equals(oldE.itemId)) {
            if (newIsSelected && newE.plain && newE.count > oldE.count) {
                delta     = newE.count - oldE.count;
                countable = true;
                reason    = "same-item-grew";
            } else {
                reason = "same-item-nochange";
            }
        } else {
            if (newIsSelected && newE.plain && newE.count > 0) {
                delta     = newE.count;
                countable = true;
                reason    = "replaced-slot";
            } else {
                reason = "replaced-not-target";
            }
        }

        mirror.put(invSlot, newE);

        debug.lastSource  = source;
        debug.lastSlot    = invSlot;
        debug.lastItemId  = newE.itemId;
        debug.lastDelta   = countable ? delta : 0;
        debug.lastPlain   = newE.plain;
        debug.lastReason  = reason;

        if (countable && delta > 0) {
            registerHarvest(delta);
            return delta;
        }
        return 0;
    }

    private void registerHarvest(int amount) {
        long now = System.currentTimeMillis();
        sessionCount += amount;

        if (state == SessionState.IDLE || state == SessionState.PAUSED) {
            state         = SessionState.ACTIVE;
            activeStartMs = now;
        }
        lastHarvestMs = now;
    }

    private Integer mapPlayerScreenHandlerToInvSlot(int shSlot) {
        if (shSlot >= 9 && shSlot <= 35) return shSlot;
        if (shSlot >= PlayerScreenHandler.HOTBAR_START && shSlot <= 44) {
            return shSlot - PlayerScreenHandler.HOTBAR_START;
        }
        return null;
    }

    private boolean isBlockingScreenOpen(Screen screen) {
        if (screen == null) return false;
        return screen instanceof HandledScreen<?>;
    }

    public SessionState getState() { return state; }
    public long         getCount() { return sessionCount; }

    public long getActiveMillis() {
        long base = accumActiveMs;
        if (state == SessionState.ACTIVE && activeStartMs > 0) {
            base += Math.max(0, System.currentTimeMillis() - activeStartMs);
        }
        return base;
    }

    public double getRevenue() {
        return sessionCount * config.getPriceFor(config.selectedCrop);
    }

    public double getRevenuePerHour() {
        long ms = getActiveMillis();
        if (ms <= 0) return 0.0;
        return getRevenue() / (ms / 3_600_000.0);
    }

    public synchronized void resetSession() {
        state         = SessionState.IDLE;
        sessionCount  = 0;
        accumActiveMs = 0;
        activeStartMs = 0;
        lastHarvestMs = 0;
    }
}
