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

package org.geysermc.floodgate.module;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.ProvidesIntoSet;
import org.geysermc.floodgate.addon.AddonManagerAddon;
import org.geysermc.floodgate.addon.DebugAddon;
import org.geysermc.floodgate.addon.PacketHandlerAddon;
import org.geysermc.floodgate.addon.data.BungeeDataAddon;
import org.geysermc.floodgate.api.inject.InjectorAddon;
import org.geysermc.floodgate.register.AddonRegister;

public final class BungeeAddonModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(AddonRegister.class).asEagerSingleton();
    }

    @Singleton
    @ProvidesIntoSet
    public InjectorAddon managerAddon() {
        return new AddonManagerAddon();
    }

    @Singleton
    @ProvidesIntoSet
    public InjectorAddon dataAddon() {
        return new BungeeDataAddon();
    }

    @Singleton
    @ProvidesIntoSet
    public InjectorAddon debugAddon() {
        return new DebugAddon();
    }

    @Singleton
    @ProvidesIntoSet
    public InjectorAddon packetHandlerAddon() {
        return new PacketHandlerAddon();
    }
}
