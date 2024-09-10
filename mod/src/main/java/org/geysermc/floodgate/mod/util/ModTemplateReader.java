package org.geysermc.floodgate.mod.util;

import org.geysermc.configutils.file.template.TemplateReader;
import org.geysermc.floodgate.mod.FloodgateMod;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModTemplateReader implements TemplateReader {

    @Override
    public BufferedReader read(String configName) {
        Path path = FloodgateMod.INSTANCE.resourcePath(configName);
        if (path != null) {
            try {
                return Files.newBufferedReader(path);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return null;
    }
}
