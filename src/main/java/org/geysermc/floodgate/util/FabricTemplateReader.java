package org.geysermc.floodgate.util;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.geysermc.configutils.file.template.TemplateReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

public class FabricTemplateReader implements TemplateReader {

    private final ModContainer container;

    public FabricTemplateReader() {
        container = FabricLoader.getInstance().getModContainer("floodgate").orElseThrow();
    }

    @Override
    public BufferedReader read(String configName) {
        Optional<Path> optional = container.findPath(configName);
        if (optional.isPresent()) {
            try {
                InputStream stream = optional.get().getFileSystem()
                        .provider()
                        .newInputStream(optional.get());
                return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return null;
    }
}
