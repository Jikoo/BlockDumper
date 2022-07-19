package com.github.jikoo.blockdumper.screen;

import com.github.jikoo.blockdumper.data.DumperSettings;
import com.github.jikoo.blockdumper.render.RenderHelper;
import java.util.Objects;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.SimpleOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public class DumperSettingsScreen extends SimpleOptionsScreen {

  private final DumperSettings settings;

  public DumperSettingsScreen(
      @NotNull Screen parent, @NotNull GameOptions gameOptions, @NotNull DumperSettings settings) {
    // TODO narrator
    super(parent, gameOptions, Text.translatable("blockdumper.title"), settings.settings());
    this.settings = settings;
  }

  @Override
  protected void initFooter() {
    this.addDrawableChild(
        new ButtonWidget(
            this.width / 2 - 155,
            this.height - 27,
            150,
            20,
            ScreenTexts.CANCEL,
            (button) -> Objects.requireNonNull(this.client).setScreen(this.parent)));
    // TODO confirm screen with epilepsy warning
    this.addDrawableChild(
        new ButtonWidget(
            this.width / 2 + 5,
            this.height - 27,
            150,
            20,
            ScreenTexts.DONE,
            (button) ->
                Objects.requireNonNull(this.client)
                    .setScreen(
                        new ConfirmScreen(
                            callback -> {
                              if (callback) {
                                client.setScreen(
                                    new RenderProgressScreen(this.parent, this.settings));
                              } else {
                                client.setScreen(parent);
                              }
                            },
                            Text.translatable("blockdumper.title"),
                            Text.translatable("blockdumper.warning.epilepsy"),
                            ScreenTexts.PROCEED,
                            ScreenTexts.BACK))));
  }

  @Override
  public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
    super.render(matrices, mouseX, mouseY, delta);
    if (settings.enablePreview.getValue()) {
      RenderHelper.renderItem(Items.DIRT, settings.renderSize.getValue());
    }
  }

}
