package com.github.jikoo.blockdumper.data;

import com.mojang.serialization.Codec;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;

public class DumperSettings {

  private static final int MIN_RENDER_SIZE = 12;
  private static final int MAX_RENDER_SIZE = 1024;

  public final SimpleOption<Integer> renderSize =
      new SimpleOption<>(
          "blockdumper.options.rendersize",
          SimpleOption.emptyTooltip(),
          (optionText, value) -> {
            if (value == MAX_RENDER_SIZE) {
              return GameOptions.getGenericValueText(
                  optionText, Text.translatable("blockdumper.options.rendersize.max", value));
            }
            if (value == MIN_RENDER_SIZE) {
              return GameOptions.getGenericValueText(
                  optionText, Text.translatable("blockdumper.options.rendersize.min", value));
            }
            return GameOptions.getGenericValueText(
                optionText, Text.translatable("blockdumper.options.rendersize.value", value));
          },
          new SimpleOption.ValidatingIntSliderCallbacks(MIN_RENDER_SIZE, MAX_RENDER_SIZE),
          Codec.intRange(MIN_RENDER_SIZE, MAX_RENDER_SIZE),
          16,
          (value) -> {});
  public final SimpleOption<Boolean> enablePreview =
      SimpleOption.ofBoolean(
          "blockdumper.options.rendersize.preview",
          SimpleOption.constantTooltip(Text.translatable("blockdumper.options.rendersize.fit")),
          true);

  public DumperSettings() {}

  public SimpleOption<?>[] settings() {
    return new SimpleOption<?>[] {renderSize, enablePreview};
  }

}
