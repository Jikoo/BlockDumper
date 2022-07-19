package com.github.jikoo.blockdumper.mixin;

import com.github.jikoo.blockdumper.BlockDumperMod;
import com.github.jikoo.blockdumper.data.DumperSettings;
import com.github.jikoo.blockdumper.screen.DumperSettingsScreen;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinTitleScreen extends Screen {

  protected MixinTitleScreen(Text title) {
    super(title);
  }

  @Inject(at = @At("TAIL"), method = "init()V")
  private void init(@NotNull CallbackInfo info) {
    addDrawableChild(
        new TexturedButtonWidget(
            width / 2 + 104,
            height / 4 + 48,
            20,
            20,
            0,
            0,
            20,
            new Identifier("blockdumper", "textures/widget/blockdumper_button.png"),
            20,
            40,
            (button) ->
                Objects.requireNonNull(client)
                    .setScreen(new DumperSettingsScreen(this, client.options, new DumperSettings())),
            new ButtonWidget.TooltipSupplier() {
              private final Text title = Text.translatable("blockdumper.title");

              public void onTooltip(
                  ButtonWidget buttonWidget, MatrixStack matrixStack, int i, int j) {
                renderOrderedTooltip(
                    matrixStack,
                    Objects.requireNonNull(client)
                        .textRenderer
                        .wrapLines(this.title, Math.max(width / 2 - 43, 170)),
                    i,
                    j);
              }

              public void supply(Consumer<Text> consumer) {
                consumer.accept(this.title);
              }
            },
            Text.translatable("blockdumper.narrator.button.blockdumper")));
  }

}
