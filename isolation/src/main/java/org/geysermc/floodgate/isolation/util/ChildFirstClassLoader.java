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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.geysermc.floodgate.isolation.library.classloader.LibraryClassLoader;

// Based of a Medium article https://medium.com/@isuru89/java-a-child-first-class-loader-cbd9c3d0305
public class ChildFirstClassLoader extends LibraryClassLoader {
    private final ClassLoader systemClassLoader;

    public ChildFirstClassLoader(ClassLoader parent) {
        super(Objects.requireNonNull(parent));
        systemClassLoader = getSystemClassLoader();
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass == null) {
            try {
                if (systemClassLoader != null) {
                    loadedClass = systemClassLoader.loadClass(name);
                }
            } catch (ClassNotFoundException ignored) {}

            try {
                if (loadedClass == null) {
                    loadedClass = findClass(name);
                }
            } catch (ClassNotFoundException e) {
                loadedClass = super.loadClass(name, resolve);
            }
        }

        if (resolve) {
            resolveClass(loadedClass);
        }
        return loadedClass;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<URL> allResources = new LinkedList<>();

        Enumeration<URL> systemResources = systemClassLoader.getResources(name);
        if (systemResources != null) {
            while (systemResources.hasMoreElements()) {
                allResources.add(systemResources.nextElement());
            }
        }

        Enumeration<URL> thisResources = findResources(name);
        if (thisResources != null) {
            while (thisResources.hasMoreElements()) {
                allResources.add(thisResources.nextElement());
            }
        }

        Enumeration<URL> parentResources = getParent().getResources(name);
        if (parentResources != null) {
            while (parentResources.hasMoreElements()) {
                allResources.add(parentResources.nextElement());
            }
        }

        return new Enumeration<>() {
            final Iterator<URL> it = allResources.iterator();

            @Override
            public boolean hasMoreElements() {
                return it.hasNext();
            }

            @Override
            public URL nextElement() {
                return it.next();
            }
        };
    }

    @Override
    public URL getResource(String name) {
        URL resource = null;
        if (systemClassLoader != null) {
            resource = systemClassLoader.getResource(name);
        }
        if (resource == null) {
            resource = findResource(name);
        }
        if (resource == null) {
            resource = getParent().getResource(name);
        }
        return resource;
    }
}
