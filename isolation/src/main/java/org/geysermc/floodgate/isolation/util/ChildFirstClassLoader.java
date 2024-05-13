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

package org.geysermc.floodgate.isolation.util;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Objects;
import org.geysermc.floodgate.isolation.library.classloader.LibraryClassLoader;

public class ChildFirstClassLoader extends LibraryClassLoader {
    public ChildFirstClassLoader(ClassLoader parent) {
        super(Objects.requireNonNull(parent));
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loadedClass = findLoadedClass(name);
            if (loadedClass == null) {
                // actual Java system classes (like java.lang.Integer) should be loaded by the parent classloader,
                // which does use the system class loader (unlike us). findClass only returns its own classes.
                // We don't call the system class loader ourselves since e.g. on Velocity, Velocity is the system.
                // This resulted in Velocity overriding our own Configurate version for example
                try {
                    loadedClass = findClass(name);
                } catch (ClassNotFoundException ignored) {
                    loadedClass = super.loadClass(name, resolve);
                }
            }

            if (resolve) {
                resolveClass(loadedClass);
            }
            return loadedClass;
        }
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return new Enumeration<>() {
            final Enumeration<URL> thisResources = findResources(name);
            final Enumeration<URL> parentResources = getParent().getResources(name);

            @Override
            public boolean hasMoreElements() {
                return thisResources.hasMoreElements() || parentResources.hasMoreElements();
            }

            @Override
            public URL nextElement() {
                return thisResources.hasMoreElements() ? thisResources.nextElement() : parentResources.nextElement();
            }
        };
    }

    @Override
    public URL getResource(String name) {
        URL resource = findResource(name);
        if (resource == null) {
            resource = getParent().getResource(name);
        }
        return resource;
    }

    static {
        ClassLoader.registerAsParallelCapable();
    }
}
