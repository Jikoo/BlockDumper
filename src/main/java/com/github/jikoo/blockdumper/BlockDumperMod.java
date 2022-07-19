package com.github.jikoo.blockdumper;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class BlockDumperMod implements ClientModInitializer {

  public static final Logger LOGGER = LoggerFactory.getLogger("blockdumper");

  @Override
  public void onInitializeClient() {
    // TODO is this needed? Entry via title screen mixin
  }

}
