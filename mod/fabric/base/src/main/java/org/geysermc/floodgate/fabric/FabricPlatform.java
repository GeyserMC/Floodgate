/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.fabric;

import io.micronaut.context.ApplicationContext;
import io.micronaut.inject.qualifiers.Qualifiers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.ModContainer;
import org.geysermc.floodgate.core.FloodgatePlatform;
import org.geysermc.floodgate.isolation.library.LibraryManager;

import java.nio.file.Path;

public final class FabricPlatform extends FloodgatePlatform {

    private ApplicationContext context;

    ModInitializer modContainer;

    private FabricPlatform(LibraryManager manager) {
        super(manager);
    }

    @Override
    public boolean isProxy() {
        return false;
    }

    /*
    TODO: What exactly do we need to do here?
    @Override
    protected void onContextCreated(ApplicationContext context) {
        context.registerSingleton(modContainer)
                .registerSingleton(
                        Path.class,
                        modContainer.getDataFolder().toPath(),
                        Qualifiers.byName("dataDirectory")
                );
        this.context = context;
    }
    */



}
