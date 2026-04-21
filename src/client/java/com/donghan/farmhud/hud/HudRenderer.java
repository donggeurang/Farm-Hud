package com.donghan.farmhud.hud;

import com.donghan.farmhud.FarmHudClient;
import com.donghan.farmhud.config.FarmHudConfig;
import com.donghan.farmhud.harvest.HarvestTracker;
import com.donghan.farmhud.harvest.SessionState;
import com.donghan.farmhud.ui.HudPositionScreen;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class HudRenderer {
    public record HudLayout(int width, int height) {}
    public record HudPosition(int x, int y) {}

    private static final int BG_FILL       = 0x0E1116;
    private static final int BG_FILL_SOFT  = 0x141820;
    private static final int BORDER        = 0x2A3240;
    private static final int C_LABEL       = 0xFF8B93A7;
    private static final int C_VALUE       = 0xFFE8ECF4;
    private static final int C_ACCENT      = 0xFF7EE8C4;
    private static final int C_GOLD        = 0xFFF5C56B;
    private static final int C_TITLE       = 0xFFEDEFF3;
    private static final int C_STATE_ACTIVE = 0xFF7EE8C4;
    private static final int C_STATE_PAUSED = 0xFFF5C56B;
    private static final int C_STATE_IDLE   = 0xFF6B7380;
    private static final int PAD_X      = 10;
    private static final int PAD_Y      = 9;
    private static final int LINE_GAP   = 2;
    private static final int SECTION_GAP = 6;
    private static final int BAR_W      = 3;
    private static final int CARD_MIN_W = 170;

    public static void register() {
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> render(ctx));
    }

    public static void render(DrawContext ctx) {
        FarmHudConfig cfg = FarmHudClient.CONFIG;
        if (cfg == null || !cfg.enabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden) return;
        if (client.currentScreen instanceof HudPositionScreen) return;

        HarvestTracker t = FarmHudClient.TRACKER;
        TextRenderer tr = client.textRenderer;
        HudPosition position = getHudPosition(client, cfg);
        renderCard(ctx, tr, cfg, t, position.x(), position.y());
    }

    public static HudLayout getLayout(MinecraftClient client) {
        FarmHudConfig cfg = FarmHudClient.CONFIG;
        if (cfg == null || client == null) return new HudLayout(0, 0);
        return computeLayout(client.textRenderer, cfg);
    }

    public static void renderPreview(DrawContext ctx, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        FarmHudConfig cfg = FarmHudClient.CONFIG;
        HarvestTracker tracker = FarmHudClient.TRACKER;
        if (client == null || cfg == null || tracker == null) return;
        renderCard(ctx, client.textRenderer, cfg, tracker, x, y);
    }

    public static HudPosition getHudPosition(MinecraftClient client, FarmHudConfig cfg) {
        HudLayout layout = getLayout(client);
        int scaledWidth = Math.round(layout.width() * cfg.hudScale);
        int scaledHeight = Math.round(layout.height() * cfg.hudScale);
        int maxX = Math.max(0, client.getWindow().getScaledWidth() - scaledWidth);
        int maxY = Math.max(0, client.getWindow().getScaledHeight() - scaledHeight);

        if (cfg.hudRelX < 0.0f || cfg.hudRelY < 0.0f) {
            cfg.hudRelX = normalizePosition(cfg.hudX, maxX);
            cfg.hudRelY = normalizePosition(cfg.hudY, maxY);
        }

        int x = Math.round(cfg.hudRelX * maxX);
        int y = Math.round(cfg.hudRelY * maxY);
        cfg.hudX = x;
        cfg.hudY = y;
        return new HudPosition(x, y);
    }

    public static void setHudPosition(MinecraftClient client, FarmHudConfig cfg, int x, int y) {
        HudLayout layout = getLayout(client);
        int scaledWidth = Math.round(layout.width() * cfg.hudScale);
        int scaledHeight = Math.round(layout.height() * cfg.hudScale);
        int maxX = Math.max(0, client.getWindow().getScaledWidth() - scaledWidth);
        int maxY = Math.max(0, client.getWindow().getScaledHeight() - scaledHeight);

        cfg.hudX = clamp(x, 0, maxX);
        cfg.hudY = clamp(y, 0, maxY);
        cfg.hudRelX = normalizePosition(cfg.hudX, maxX);
        cfg.hudRelY = normalizePosition(cfg.hudY, maxY);
    }

    private static int drawRow(DrawContext ctx, TextRenderer tr, String label, String value,
                               int x, int rx, int y, int alpha, int labelColor, int valueColor) {
        drawText(ctx, tr, label, x, y, withAlpha(labelColor, alpha));
        int vw = tr.getWidth(value);
        drawText(ctx, tr, value, rx - vw, y, withAlpha(valueColor, alpha));
        return y + tr.fontHeight + LINE_GAP;
    }

    private static void drawText(DrawContext ctx, TextRenderer tr, String s, int x, int y, int argb) {
        ctx.drawTextWithShadow(tr, s, x, y, argb);
    }

    private static void fill(DrawContext ctx, int x1, int y1, int x2, int y2, int argb) {
        ctx.fill(x1, y1, x2, y2, argb);
    }

    private static void drawHLine(DrawContext ctx, int x1, int x2, int y, int argb) {
        ctx.fill(x1, y, x2, y + 1, argb);
    }

    private static void drawVLine(DrawContext ctx, int x, int y1, int y2, int argb) {
        ctx.fill(x, y1, x + 1, y2, argb);
    }

    private static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static float normalizePosition(int value, int max) {
        if (max <= 0) return 0.0f;
        return (float) clamp(value, 0, max) / (float) max;
    }

    private static void renderCard(DrawContext ctx, TextRenderer tr, FarmHudConfig cfg, HarvestTracker t, int baseX, int baseY) {
        int backgroundAlpha = clamp((int) (cfg.hudAlpha * 255f), 0, 255);
        HudLayout layout = computeLayout(tr, cfg);

        MatrixStack ms = ctx.getMatrices();
        ms.push();
        ms.translate(baseX, baseY, 0);
        ms.scale(cfg.hudScale, cfg.hudScale, 1.0f);

        int stateColor = stateColor(t.getState());
        String stateText = stateText(t.getState());
        String cropName = cropKorean(cfg.selectedCrop);
        String vCount = formatNumber(t.getCount()) + " 개";
        String vRevenue = formatMoney(t.getRevenue()) + " G";
        String vTime = formatDuration(t.getActiveMillis());

        int cardW = layout.width();
        int cardH = layout.height();
        int lineH = tr.fontHeight;

        fill(ctx, 0, 0, cardW, cardH, withAlpha(BG_FILL, backgroundAlpha));
        int borderA = Math.min(255, backgroundAlpha + 20);
        drawHLine(ctx, 0, cardW, 0, withAlpha(BORDER, borderA));
        drawHLine(ctx, 0, cardW, cardH - 1, withAlpha(BORDER, borderA));
        drawVLine(ctx, 0, 0, cardH, withAlpha(BORDER, borderA));
        drawVLine(ctx, cardW - 1, 0, cardH, withAlpha(BORDER, borderA));

        int barA = Math.min(255, backgroundAlpha + 40);
        fill(ctx, 1, 1, 1 + BAR_W, cardH - 1, withAlpha(stateColor & 0xFFFFFF, barA));

        int x = BAR_W + PAD_X;
        int rx = cardW - PAD_X;
        int y = PAD_Y;

        drawText(ctx, tr, "§lFARM HUD", x, y, C_TITLE);
        String stateLabel = "● " + stateText;
        int stW = tr.getWidth(stateLabel);
        drawText(ctx, tr, stateLabel, rx - stW, y, stateColor);
        y += lineH + SECTION_GAP;

        drawHLine(ctx, BAR_W + PAD_X - 4, cardW - PAD_X + 4, y, withAlpha(BORDER, borderA));
        y += 1 + SECTION_GAP - 2;

        y = drawRow(ctx, tr, "작물", cropName, x, rx, y, 255, C_LABEL, C_VALUE);
        y += SECTION_GAP - LINE_GAP;

        y = drawRow(ctx, tr, "수확량", vCount, x, rx, y, 255, C_LABEL, C_ACCENT);
        y = drawRow(ctx, tr, "수익", vRevenue, x, rx, y, 255, C_LABEL, C_GOLD);
        y += SECTION_GAP - LINE_GAP;

        y = drawRow(ctx, tr, "경과", vTime, x, rx, y, 255, C_LABEL, C_VALUE);

        ms.pop();
    }

    private static HudLayout computeLayout(TextRenderer tr, FarmHudConfig cfg) {
        HarvestTracker t = FarmHudClient.TRACKER;
        int contentW = 0;

        String stateText = stateText(t.getState());
        String cropName = cropKorean(cfg.selectedCrop);
        String vCount = formatNumber(t.getCount()) + " 개";
        String vRevenue = formatMoney(t.getRevenue()) + " G";
        String vTime = formatDuration(t.getActiveMillis());

        contentW = Math.max(contentW, tr.getWidth("§lFARM HUD"));
        contentW = Math.max(contentW, tr.getWidth("● " + stateText));
        contentW = Math.max(contentW, tr.getWidth("작물") + 6 + tr.getWidth(cropName));
        contentW = Math.max(contentW, tr.getWidth("수확량") + 6 + tr.getWidth(vCount));
        contentW = Math.max(contentW, tr.getWidth("수익") + 6 + tr.getWidth(vRevenue));
        contentW = Math.max(contentW, tr.getWidth("경과") + 6 + tr.getWidth(vTime));

        int cardW = Math.max(CARD_MIN_W, contentW + PAD_X * 2 + BAR_W);
        int lineH = tr.fontHeight;
        int h = PAD_Y;
        h += lineH;
        h += SECTION_GAP + 1;
        h += lineH + LINE_GAP;
        h += SECTION_GAP;
        h += lineH + LINE_GAP;
        h += lineH + LINE_GAP;
        h += SECTION_GAP;
        h += lineH;
        h += PAD_Y;
        return new HudLayout(cardW, h);
    }

    private static String formatNumber(long n) {
        return String.format("%,d", n);
    }

    private static String formatMoney(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "0";
        return String.format("%,.1f", v);
    }

    private static String formatDuration(long ms) {
        long s = ms / 1000L;
        long h = s / 3600L;
        long m = (s % 3600L) / 60L;
        long sec = s % 60L;
        if (h > 0) return String.format("%d시간 %02d분", h, m);
        if (m > 0) return String.format("%d분 %02d초", m, sec);
        return String.format("%d초", sec);
    }

    private static String stateText(SessionState s) {
        return switch (s) {
            case ACTIVE -> "수확 중";
            case PAUSED -> "일시정지";
            case IDLE   -> "대기";
        };
    }

    private static int stateColor(SessionState s) {
        return switch (s) {
            case ACTIVE -> C_STATE_ACTIVE;
            case PAUSED -> C_STATE_PAUSED;
            case IDLE   -> C_STATE_IDLE;
        };
    }

    private static String cropKorean(String id) {
        if (id == null) return "-";
        return switch (id) {
            case "minecraft:melon"   -> "수박";
            case "minecraft:pumpkin" -> "호박";
            default -> id.replace("minecraft:", "");
        };
    }
}
