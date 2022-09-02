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

package org.geysermc.floodgate.universal.loader;

import java.lang.reflect.InvocationTargetException;

public class LoaderUtils {
  public static void invokeLoad(Object floodgatePlatform) {
    try {
      floodgatePlatform.getClass().getMethod("load").invoke(floodgatePlatform);
    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
      throw new RuntimeException(exception);
    }
  }

  public static void invokeEnable(Object floodgatePlatform) {
    try {
      floodgatePlatform.getClass().getMethod("enable").invoke(floodgatePlatform);
    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
      throw new RuntimeException(exception);
    }
  }

  public static void invokeDisable(Object floodgatePlatform) {
    try {
      floodgatePlatform.getClass().getMethod("disable").invoke(floodgatePlatform);
    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
      throw new RuntimeException(exception);
    }
  }
}
