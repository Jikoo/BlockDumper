package com.github.jikoo.blockdumper.mixin;

import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.texture.TextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderer.class)
public interface MixinItemRenderer {

  @Accessor(value = "textureManager")
  TextureManager blockdumper$accessor$textureManager();

}
