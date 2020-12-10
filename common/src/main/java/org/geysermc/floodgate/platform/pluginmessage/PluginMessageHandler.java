/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.platform.pluginmessage;

import com.google.common.base.Charsets;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.geysermc.cumulus.Form;
import org.geysermc.floodgate.config.FloodgateConfigHolder;
import org.geysermc.floodgate.util.RawSkin;

public abstract class PluginMessageHandler {
    protected final Short2ObjectMap<Form> storedForms = new Short2ObjectOpenHashMap<>();
    private final AtomicInteger nextFormId = new AtomicInteger(0);
    private final FloodgateConfigHolder configHolder;

    protected PluginMessageHandler(FloodgateConfigHolder configHolder) {
        this.configHolder = configHolder;
    }

    public abstract boolean sendForm(UUID player, Form form);

    public abstract boolean sendSkinRequest(UUID player, RawSkin skin);

    public abstract void sendSkinResponse(UUID player, String response);

    protected byte[] createFormData(Form form) {
        short formId = getNextFormId();
        if (configHolder.isProxy()) {
            formId |= 0x8000;
        }
        storedForms.put(formId, form);

        byte[] jsonData = form.getJsonData().getBytes(Charsets.UTF_8);

        byte[] data = new byte[jsonData.length + 3];
        data[0] = (byte) form.getType().ordinal();
        data[1] = (byte) (formId >> 8 & 0xFF);
        data[2] = (byte) (formId & 0xFF);
        System.arraycopy(jsonData, 0, data, 3, jsonData.length);
        return data;
    }

    protected boolean callResponseConsumer(byte[] data) {
        Form storedForm = storedForms.remove(getFormId(data));
        if (storedForm != null) {
            storedForm.getResponseHandler().accept(
                    new String(data, 2, data.length - 2, Charsets.UTF_8));
            return true;
        }
        return false;
    }

    protected short getFormId(byte[] data) {
        return (short) ((data[0] & 0xFF) << 8 | data[1] & 0xFF);
    }

    protected short getNextFormId() {
        // signed bit is used to check if the form is from a proxy or a server
        return (short) nextFormId.getAndUpdate(
                (number) -> number == Short.MAX_VALUE ? 0 : number + 1);
    }
}
