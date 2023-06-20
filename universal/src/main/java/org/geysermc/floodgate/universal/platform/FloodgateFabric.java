/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Floodgate
 */

package org.geysermc.floodgate.universal.platform;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import java.nio.file.Path;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.universal.UniversalLoader;
import org.geysermc.floodgate.universal.holder.FloodgateHolder;
import org.geysermc.floodgate.universal.logger.JavaUtilLogger;
import org.geysermc.floodgate.universal.logger.Slf4jLogger;
import org.geysermc.floodgate.universal.util.UniversalLogger;

public final class FloodgateFabric implements FabricModInitializer {
  private FloodgateHolder holder;

  @Override
  public void onLoad() {
    UniversalLogger logger = new JavaUtilLogger(getLogger());
    try {
      holder = new UniversalLoader("spigot", getDataFolder().toPath(), logger).start();
      holder.init(new Class[]{JavaPlugin.class}, this);
      holder.load();
    } catch (Exception exception) {
      throw new RuntimeException("Failed to load Floodgate", exception);
    }
  }

  @Override
  public void onEnable() {
    holder.enable();
  }

  @Override
  public void onDisable() {
    holder.disable();
  }

}
