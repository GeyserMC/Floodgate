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

package org.geysermc.floodgate.isolation.loader;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.geysermc.floodgate.isolation.library.LibraryManager;

public class PlatformHolder {
    private final Class<?> platformClass;
    private final LibraryManager manager;

    private Object platformInstance;

    public PlatformHolder(Class<?> platformClass, LibraryManager manager) {
        this.platformClass = platformClass;
        this.manager = manager;
    }

    public void init(List<Class<?>> argumentTypes, List<Object> argumentValues)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        argumentTypes = new ArrayList<>(argumentTypes);
        argumentValues = new ArrayList<>(argumentValues);

        // LibraryManager is always the first argument
        argumentTypes.add(0, LibraryManager.class);
        argumentValues.add(0, manager);

        platformInstance = platformClass
                .getConstructor(argumentTypes.toArray(Class[]::new))
                .newInstance(argumentValues.toArray());
    }

    public void load() {
        LoaderUtil.invokeLoad(platformInstance);
    }

    public void enable() {
        LoaderUtil.invokeEnable(platformInstance);
    }

    public void disable() {
        LoaderUtil.invokeDisable(platformInstance);
        close();
    }

    public void close() {
        try {
            manager.classLoader().close();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to close classloader!", exception);
        }
    }

    public PlatformHolder platformInstance(Object platformInstance) {
        if (this.platformInstance == null) {
            this.platformInstance = platformInstance;
        }
        return this;
    }

    public LibraryManager manager() {
        return manager;
    }

    public Class<?> platformClass() {
        return platformClass;
    }
}
