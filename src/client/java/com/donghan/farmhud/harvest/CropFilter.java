package com.donghan.farmhud.harvest;

import com.donghan.farmhud.config.FarmHudConfig;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class CropFilter {
    private final FarmHudConfig config;

    public CropFilter(FarmHudConfig config) {
        this.config = config;
    }

    public String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "minecraft:air";
        Identifier id = Registries.ITEM.getId(stack.getItem());
        return id == null ? "minecraft:air" : id.toString();
    }

    public boolean isSelectedCrop(ItemStack stack) {
        return itemId(stack).equals(config.selectedCrop);
    }

    public boolean isPlain(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!config.excludeCustomNameAndLore) return true;

        if (stack.contains(DataComponentTypes.CUSTOM_NAME)) return false;

        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null && !lore.lines().isEmpty()) return false;

        return true;
    }
}
