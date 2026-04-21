package com.donghan.farmhud.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FarmHudConfig {
    public boolean enabled = true;
    public int   hudX      = 8;
    public int   hudY      = 8;
    public float hudRelX   = -1.0f;
    public float hudRelY   = -1.0f;
    public float hudScale  = 1.0f;
    public float hudAlpha  = 0.75f;
    public int pauseSeconds = 30;
    public String  selectedCrop             = "minecraft:melon";
    public boolean onlyCountWhenNoScreen    = true;
    public boolean excludeCustomNameAndLore = true;
    public Map<String, Double> cropPrices = new HashMap<>();
    public List<String> trackedCrops = new ArrayList<>();
    public boolean debugMode = false;

    public FarmHudConfig() {
        trackedCrops.add("minecraft:melon");
        trackedCrops.add("minecraft:pumpkin");

        cropPrices.put("minecraft:melon",   2.0);
        cropPrices.put("minecraft:pumpkin", 3.0);
    }

    public double getPriceFor(String cropId) {
        return cropPrices.getOrDefault(cropId, 0.0);
    }
}
