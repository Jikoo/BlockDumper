package com.github.jikoo.blockdumper.screen;

import com.github.jikoo.blockdumper.BlockDumperMod;
import com.github.jikoo.blockdumper.data.DumperSettings;
import com.github.jikoo.blockdumper.render.RenderHelper;
import com.github.jikoo.blockdumper.render.SaveQueue;
import com.github.jikoo.blockdumper.render.WindowToPng;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

public class RenderProgressScreen extends Screen {

  // TODO
  //  - extract all item stuff to an external renderer, make this just queue things
  //  - separate save queue that dumps the images - image creation isn't too heavy, saving is
  private static final int TEXT_COLOR = 0xffffff;
  private final Screen parent;
  private final List<Item> items;
  private final int size;
  private final int renderSize;
  private ButtonWidget completeButton;
  private int index = 0;
  private int lastRenderStartX = -1; // TODO make this less messy, boolean or something

  public RenderProgressScreen(@NotNull Screen parent, @NotNull DumperSettings settings) {
    super(Text.translatable("blockdumper.render.title"));
    this.parent = parent;
    // skip index 0, air.
    items = Registry.ITEM.stream().skip(1).toList();
    size = items.size();
    renderSize = settings.renderSize.getValue();
  }

  @Override
  public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
    if (hasAvailableMemory()) {
      saveScreenshot();
      clearBackground();
      renderCurrent(matrices);
    } else {
      drawCenteredText(matrices, textRenderer, Text.translatable("blockdumper.render.low_memory"), width / 2, 50, 0xff0000);
    }
    completeButton.render(matrices, mouseX, mouseY, delta);
  }

  private boolean hasAvailableMemory() {
    Runtime runtime = Runtime.getRuntime();
    long freeMemory = runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory();
    // Expect to consume more memory than we explicitly need - other objects are also created,
    // some not on the current thread.
    long expectedConsumption = 256L * renderSize * renderSize * 4;

    return freeMemory > expectedConsumption;
  }

  protected void saveScreenshot() {
    if (index <= 0 || index > size || lastRenderStartX < 0) {
      return;
    }

    Window window = Objects.requireNonNull(client).getWindow();
    File output = client.runDirectory.toPath()
        .resolve(
            Path.of("blockdumper", "out", items.get(index - 1).getTranslationKey() + ".png"))
        .toFile();

    if (hasAvailableMemory()) {
      // Double check available memory, crashing is bad.
      int startY = window.getHeight() - RenderHelper.scaleInt(window, RenderHelper.ITEM_RENDER_START) - renderSize;
      ByteBuffer data = WindowToPng.readWindowPixels(lastRenderStartX, startY, renderSize, renderSize);

      SaveQueue.add(new WindowToPng(data, renderSize, renderSize, output));
      ++postedRenders;
    } else {
      --index;
    }
  }

  protected void clearBackground() {
    GL11.glClearColor(0f, 0f, 0f, 0f);
    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
  }

  protected void renderCurrent(MatrixStack matrices) {
    int centerX = this.width / 2;

    drawCenteredText(matrices, this.textRenderer, this.title, centerX, 30, TEXT_COLOR);

    if (index >= size) {
      if (index == size) {
        completeButton.setMessage(ScreenTexts.DONE);
        ++index;
      }
      drawCenteredText(
          matrices,
          textRenderer,
          Text.translatable("blockdumper.render.progress", size, size),
          centerX,
          40,
          TEXT_COLOR);
      drawCenteredText(matrices, textRenderer, ScreenTexts.DONE, centerX, 50, TEXT_COLOR);
      return;
    }

    // Center X/Y count
    drawCenteredText(
        matrices,
        textRenderer,
        Text.translatable("blockdumper.render.progress", index, size),
        centerX,
        40,
        TEXT_COLOR);

    Item currentItem = items.get(index);

    RenderHelper.renderItem(currentItem, renderSize);

    Window window = Objects.requireNonNull(client).getWindow();
    if (!RenderHelper.willItemRenderFitWindow(window, renderSize)) {
      lastRenderStartX = -1;
      drawCenteredText(matrices, this.textRenderer, Text.translatable("blockdumper.render.too_small"), centerX, 50,  0xff0000);
      return;
    }

    // Right align "Current item: " to center.
    Text active = Text.translatable("blockdumper.render.active");
    textRenderer.drawWithShadow(
        matrices, active, centerX - textRenderer.getWidth(active), 50, TEXT_COLOR);
    // Left align item name to center.
    textRenderer.drawWithShadow(matrices, currentItem.getName(), centerX, 50, TEXT_COLOR);

    lastRenderStartX = window.getWidth() / 2 - renderSize / 2;

    ++index;
  }

  @Override
  public void close() {
    Objects.requireNonNull(client).setScreen(parent);
  }

  @Override
  protected void init() {
    // TODO add an "open folder" button
    completeButton =
        new ButtonWidget(
            this.width / 2 - 100,
            5,
            200,
            20,
            ScreenTexts.CANCEL,
            (button) -> {
              SaveQueue.kill();
              Objects.requireNonNull(client).setScreen(this.parent);
            });
    this.addDrawableChild(completeButton);
  }

}
