package com.github.jikoo.blockdumper.render;

import com.github.jikoo.blockdumper.mixin.MixinItemRenderer;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public final class RenderHelper {

  public static final int ITEM_RENDER_START = 60;

  public static void renderItem(@NotNull Item item, int renderSize) {
    MinecraftClient client = MinecraftClient.getInstance();
    ItemRenderer renderer = client.getItemRenderer();
    ItemStack stack = new ItemStack(item);
    BakedModel model = renderer.getModel(stack, null, null, 0);

    // See ItemRenderer#renderGuiItemIcon(ItemStack, x, y)
    ((MixinItemRenderer) renderer)
        .blockdumper$accessor$textureManager()
        .getTexture(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE)
        .setFilter(false, false);
    RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
    RenderSystem.enableBlend();
    RenderSystem.blendFunc(
        GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    MatrixStack matrixStack = RenderSystem.getModelViewStack();
    matrixStack.push();
    // BR start: Scale to desired size instead of default 16.
    // Render is centered with default settings. We want it to be shifted to center only in X.
    Window window = client.getWindow();
    // Don't inline these - IDE warns about int division in floating point context, but we want to
    // grab our screenshot from a consistent location relative to the render.
    float scaledSize = (float) (renderSize / window.getScaleFactor());
    int scaledXCenter = window.getScaledWidth() / 2;
    int scaledYStart = (int) (ITEM_RENDER_START + scaledSize / 2);
    matrixStack.translate(scaledXCenter, scaledYStart, 100.0F + renderer.zOffset);
    matrixStack.scale(1.0F, -1.0F, 1.0F);
    matrixStack.scale(scaledSize, scaledSize, scaledSize);
    // BR end
    RenderSystem.applyModelViewMatrix();
    MatrixStack matrixStack2 = new MatrixStack();
    VertexConsumerProvider.Immediate immediate =
        client.getBufferBuilders().getEntityVertexConsumers();
    boolean isNotSideLit = !model.isSideLit();
    if (isNotSideLit) {
      DiffuseLighting.disableGuiDepthLighting();
    }

    // Display item.
    renderer.renderItem(
        stack,
        ModelTransformation.Mode.GUI,
        false,
        matrixStack2,
        immediate,
        15728880,
        OverlayTexture.DEFAULT_UV,
        model);
    immediate.draw();

    RenderSystem.enableDepthTest();
    if (isNotSideLit) {
      DiffuseLighting.enableGuiDepthLighting();
    }

    matrixStack.pop();
    RenderSystem.applyModelViewMatrix();
  }

  public static int scaleInt(@NotNull Window window, int value) {
    return (int) (window.getScaleFactor() * value);
  }

  public static boolean willItemRenderFitWindow(@NotNull Window window, int renderSize) {
    return scaleInt(window, ITEM_RENDER_START) + renderSize <= window.getHeight()
        && renderSize <= window.getWidth();
  }

  public static void saveScreenshot(
      @NotNull Window window,
      @NotNull File output,
      int scaledX,
      int unscaledY,
      int renderSize)
      throws IOException {
    int startY = window.getHeight() - (int) (unscaledY * window.getScaleFactor()) - renderSize;

    BufferedImage image = new BufferedImage(renderSize, renderSize, BufferedImage.TYPE_INT_ARGB);
    Graphics graphics = image.getGraphics();
    ByteBuffer buffer = BufferUtils.createByteBuffer(renderSize * renderSize * 4);

    GL11.glReadPixels(scaledX, startY, renderSize, renderSize, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
    buffer.rewind();

    for (int localHeight = 0; localHeight < renderSize; ++localHeight) {
      for (int localWidth = 0; localWidth < renderSize; ++localWidth) {
        graphics.setColor(new Color(buffer.get() & 0xff, buffer.get() & 0xff, buffer.get() & 0xff, buffer.get() & 0xff));
        graphics.drawRect(localWidth, renderSize - localHeight, 1, 1);
      }
    }

    Files.createDirectories(output.getParentFile().toPath());
    ImageIO.write(image, "png", output);
  }

  private RenderHelper() {
    throw new IllegalStateException("Cannot instantiate static helper class");
  }

}
