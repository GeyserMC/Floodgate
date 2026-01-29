/**
 * Copyright (c) 2008, http://www.snakeyaml.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.geysermc.floodgate.database.config;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;

/**
 * Construct instances with a custom Class Loader.
 */
public class DatabaseConfigConstructor extends Constructor {

    private final ClassLoader loader;

    public DatabaseConfigConstructor(ClassLoader cLoader) {
        this(Object.class, cLoader, new LoaderOptions());
    }

    public DatabaseConfigConstructor(ClassLoader loader, LoaderOptions loadingConfig) {
        this(Object.class, loader, loadingConfig);
    }

    public DatabaseConfigConstructor(Class<? extends Object> theRoot, ClassLoader theLoader, LoaderOptions loadingConfig) {
        super(theRoot, loadingConfig);
        if (theLoader == null) {
            throw new NullPointerException("Loader must be provided.");
        }
        this.loader = theLoader;
    }

    @Override
    protected Class<?> getClassForName(String name) throws ClassNotFoundException {
        return Class.forName(name, true, loader);
    }
}
