package com.donghan.farmhud.ui;

import com.donghan.farmhud.FarmHudClient;
import com.donghan.farmhud.config.ConfigManager;
import com.donghan.farmhud.config.FarmHudConfig;
import com.donghan.farmhud.hud.HudRenderer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class HudPositionScreen extends Screen {
    private final Screen parent;
    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;

    public HudPositionScreen(Screen parent) {
        super(Text.literal("FARM HUD 위치 변경"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonY = this.height - 32;
        addDrawableChild(ButtonWidget.builder(Text.literal("초기화"),
                        b -> resetPosition())
                .dimensions(this.width / 2 - 104, buttonY, 100, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("완료"),
                        b -> close())
                .dimensions(this.width / 2 + 4, buttonY, 100, 20)
                .build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 16, 0xFFEDEFF3);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("HUD 카드를 드래그해서 위치를 옮기세요"),
                this.width / 2, 34, 0xFFB7C0D0);

        HudRenderer.HudPosition position = HudRenderer.getHudPosition(this.client, FarmHudClient.CONFIG);
        HudRenderer.renderPreview(ctx, position.x(), position.y());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0) return false;

        HudRenderer.HudLayout layout = HudRenderer.getLayout(this.client);
        FarmHudConfig cfg = FarmHudClient.CONFIG;
        HudRenderer.HudPosition position = HudRenderer.getHudPosition(this.client, cfg);

        int scaledWidth = Math.round(layout.width() * cfg.hudScale);
        int scaledHeight = Math.round(layout.height() * cfg.hudScale);
        int hudX = position.x();
        int hudY = position.y();

        if (mouseX >= hudX && mouseX <= hudX + scaledWidth && mouseY >= hudY && mouseY <= hudY + scaledHeight) {
            this.dragging = true;
            this.dragOffsetX = (int) mouseX - hudX;
            this.dragOffsetY = (int) mouseY - hudY;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!this.dragging || button != 0) return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);

        FarmHudConfig cfg = FarmHudClient.CONFIG;
        HudRenderer.HudLayout layout = HudRenderer.getLayout(this.client);
        int scaledWidth = Math.round(layout.width() * cfg.hudScale);
        int scaledHeight = Math.round(layout.height() * cfg.hudScale);

        int newX = clamp((int) mouseX - this.dragOffsetX, 0, Math.max(0, this.width - scaledWidth));
        int newY = clamp((int) mouseY - this.dragOffsetY, 0, Math.max(0, this.height - scaledHeight));
        HudRenderer.setHudPosition(this.client, cfg, newX, newY);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.dragging) {
            this.dragging = false;
            ConfigManager.save(FarmHudClient.CONFIG);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        ConfigManager.save(FarmHudClient.CONFIG);
        if (this.client != null) this.client.setScreen(this.parent);
    }

    private void resetPosition() {
        HudRenderer.setHudPosition(this.client, FarmHudClient.CONFIG, 8, 8);
        ConfigManager.save(FarmHudClient.CONFIG);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
