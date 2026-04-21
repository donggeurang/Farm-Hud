package com.donghan.farmhud.ui;

import com.donghan.farmhud.FarmHudClient;
import com.donghan.farmhud.config.ConfigManager;
import com.donghan.farmhud.config.FarmHudConfig;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {
    private final Screen parent;

    private CyclingButtonWidget<String> cropButton;
    private TextFieldWidget priceField;
    private ValueSliderWidget scaleSlider;
    private ValueSliderWidget alphaSlider;
    private ValueSliderWidget pauseSlider;

    public ConfigScreen(Screen parent) {
        super(Text.literal("FARM HUD 설정"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        FarmHudConfig c = FarmHudClient.CONFIG;
        if (!c.trackedCrops.contains(c.selectedCrop)) {
            c.selectedCrop = "minecraft:melon";
        }

        int cx = this.width / 2;
        int w = 250;
        int h = 20;
        int gap = 24;
        int y = 42;

        cropButton = addDrawableChild(CyclingButtonWidget.<String>builder(this::cropLabel)
                .values(c.trackedCrops)
                .initially(c.selectedCrop)
                .build(cx - w / 2, y, w, h, Text.literal("작물"),
                        (btn, v) -> {
                            c.selectedCrop = v;
                            priceField.setText(String.valueOf(c.getPriceFor(v)));
                        }));
        y += gap;

        priceField = addField(cx - w / 2, y, w, h, String.valueOf(c.getPriceFor(c.selectedCrop)), "개당 가격 (G)");
        y += gap;

        scaleSlider = addDrawableChild(new ValueSliderWidget(cx - w / 2, y, w, h,
                "HUD 크기", 0.5, 1.5, c.hudScale, false,
                value -> c.hudScale = (float) value));
        y += gap;

        alphaSlider = addDrawableChild(new ValueSliderWidget(cx - w / 2, y, w, h,
                "배경 투명도", 0.0, 1.0, c.hudAlpha, false,
                value -> c.hudAlpha = (float) value));
        y += gap;

        pauseSlider = addDrawableChild(new ValueSliderWidget(cx - w / 2, y, w, h,
                "자동 일시정지 (초)", 1.0, 30.0, c.pauseSeconds, true,
                value -> c.pauseSeconds = (int) Math.round(value)));
        y += gap;

        addDrawableChild(CyclingButtonWidget.onOffBuilder(c.enabled)
                .build(cx - w / 2, y, w, h, Text.literal("HUD 표시"),
                        (btn, v) -> c.enabled = v));
        y += gap;

        addDrawableChild(ButtonWidget.builder(Text.literal("HUD 위치 변경 (드래그)"),
                        b -> {
                            applyAndSave();
                            if (client != null) client.setScreen(new HudPositionScreen(this));
                        })
                .dimensions(cx - w / 2, y, w, h).build());
        y += gap;

        addDrawableChild(ButtonWidget.builder(Text.literal("세션 초기화"),
                        b -> FarmHudClient.TRACKER.resetSession())
                .dimensions(cx - w / 2, y, w, h).build());
        y += gap + 4;

        addDrawableChild(ButtonWidget.builder(Text.literal("저장 & 닫기"),
                        b -> close())
                .dimensions(cx - w / 2, y, w, h).build());
    }

    private TextFieldWidget addField(int x, int y, int w, int h, String value, String placeholder) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, w, h, Text.literal(placeholder));
        f.setMaxLength(128);
        f.setText(value);
        addDrawableChild(f);
        return f;
    }

    private void applyAndSave() {
        FarmHudConfig c = FarmHudClient.CONFIG;
        try { c.cropPrices.put(c.selectedCrop, Double.parseDouble(priceField.getText().trim())); } catch (Exception ignored) {}
        ConfigManager.save(c);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx, mx, my, delta);
        super.render(ctx, mx, my, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width/2, 16, 0xFFEDEFF3);
    }

    @Override
    public void close() {
        applyAndSave();
        if (client != null) client.setScreen(parent);
    }

    private Text cropLabel(String cropId) {
        return Text.literal(cropName(cropId) + " (클릭해서 변경)");
    }

    private static String cropName(String id) {
        if (id == null) return "-";
        return switch (id) {
            case "minecraft:melon" -> "수박";
            case "minecraft:pumpkin" -> "호박";
            default -> id.replace("minecraft:", "");
        };
    }

    @FunctionalInterface
    private interface ValueConsumer {
        void accept(double value);
    }

    private final class ValueSliderWidget extends SliderWidget {
        private final String label;
        private final double min;
        private final double max;
        private final boolean integer;
        private final ValueConsumer onChange;

        private ValueSliderWidget(int x, int y, int width, int height, String label,
                                  double min, double max, double current, boolean integer,
                                  ValueConsumer onChange) {
            super(x, y, width, height, Text.empty(), normalize(current, min, max));
            this.label = label;
            this.min = min;
            this.max = max;
            this.integer = integer;
            this.onChange = onChange;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            double current = currentValue();
            String valueText = this.integer
                    ? Integer.toString((int) Math.round(current))
                    : String.format("%.2f", current);
            setMessage(Text.literal(this.label + ": " + valueText));
        }

        @Override
        protected void applyValue() {
            this.onChange.accept(currentValue());
            updateMessage();
        }

        private double currentValue() {
            return this.min + (this.max - this.min) * this.value;
        }

        private static double normalize(double current, double min, double max) {
            if (max <= min) return 0.0;
            return Math.max(0.0, Math.min(1.0, (current - min) / (max - min)));
        }
    }
}
