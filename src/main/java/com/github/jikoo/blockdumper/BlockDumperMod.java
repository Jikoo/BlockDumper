package com.github.jikoo.blockdumper;

import com.github.jikoo.blockdumper.render.SaveQueue;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class BlockDumperMod implements ClientModInitializer {

  public static final Logger LOGGER = LoggerFactory.getLogger("blockdumper");

  @Override
  public void onInitializeClient() {
    ClientLifecycleEvents.CLIENT_STOPPING.register(event -> SaveQueue.kill());
  }

}
