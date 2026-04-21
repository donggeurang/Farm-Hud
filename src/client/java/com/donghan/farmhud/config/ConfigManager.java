package com.donghan.farmhud.config;

import com.donghan.farmhud.FarmHudClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH =
            FabricLoader.getInstance().getConfigDir().resolve("farmhud.json");
    private static final String MELON = "minecraft:melon";
    private static final String PUMPKIN = "minecraft:pumpkin";

    public static FarmHudConfig load() {
        try {
            if (!Files.exists(PATH)) {
                FarmHudConfig c = new FarmHudConfig();
                save(c);
                return c;
            }
            try (Reader r = Files.newBufferedReader(PATH)) {
                FarmHudConfig c = GSON.fromJson(r, FarmHudConfig.class);
                return c == null ? new FarmHudConfig() : normalize(c);
            }
        } catch (Exception e) {
            FarmHudClient.LOGGER.error("[FarmHud] 설정 로드 실패, 기본값 사용", e);
            return new FarmHudConfig();
        }
    }

    public static void save(FarmHudConfig c) {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer w = Files.newBufferedWriter(PATH)) {
                GSON.toJson(c, w);
            }
        } catch (IOException e) {
            FarmHudClient.LOGGER.error("[FarmHud] 설정 저장 실패", e);
        }
    }

    private static FarmHudConfig normalize(FarmHudConfig c) {
        c.trackedCrops = new ArrayList<>();
        c.trackedCrops.add(MELON);
        c.trackedCrops.add(PUMPKIN);

        if (!MELON.equals(c.selectedCrop) && !PUMPKIN.equals(c.selectedCrop)) {
            c.selectedCrop = MELON;
        }

        c.cropPrices = new HashMap<>(c.cropPrices);
        c.cropPrices.entrySet().removeIf(entry -> !MELON.equals(entry.getKey()) && !PUMPKIN.equals(entry.getKey()));
        c.cropPrices.putIfAbsent(MELON, 2.0);
        c.cropPrices.putIfAbsent(PUMPKIN, 3.0);
        return c;
    }
}
