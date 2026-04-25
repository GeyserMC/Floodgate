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

package org.geysermc.floodgate.api.inject;

/**
 * @deprecated Injector addons will be removed with the launch of Floodgate 3.0. Please look at
 * <a href="https://github.com/GeyserMC/Floodgate/issues/536">#536</a> for additional context.
 */
@Deprecated
public interface PlatformInjector {
    /**
     * Injects the server connection. This will allow various addons (like getting the Floodgate
     * data and debug mode) to work.
     *
     * @throws Exception if the platform couldn't be injected
     */
    void inject() throws Exception;

    /**
     * Some platforms may not be able to remove their injection process. If so, this method will
     * return false.
     *
     * @return true if it is safe to attempt to remove our injection performed in {@link #inject()}.
     */
    default boolean canRemoveInjection() {
        return true;
    }

    /**
     * Removes the injection from the server. Please note that this function should only be used
     * internally (on plugin shutdown). This method will also remove every added addon.
     *
     * @throws Exception if the platform injection could not be removed
     */
    void removeInjection() throws Exception;

    /**
     * If the server connection is currently injected.
     *
     * @return true if the server connection is currently injected, returns false otherwise
     */
    boolean isInjected();

    /**
     * Adds an addon to the addon list of the Floodgate Injector (the addon is called when Floodgate
     * injects a channel). See {@link InjectorAddon} for more info.
     *
     * @param addon the addon to add to the addon list
     * @return true if the addon has been added, false if the addon is already present
     */
    boolean addAddon(InjectorAddon addon);

    /**
     * Removes an addon from the addon list of the Floodgate Injector (the addon is called when
     * Floodgate injects a channel). See {@link InjectorAddon} for more info.
     *
     * @param addon the class of the addon to remove from the addon list
     * @param <T>   the addon type
     * @return the removed addon instance
     */
    <T extends InjectorAddon> T removeAddon(Class<T> addon);
}
