package org.geysermc.floodgate.isolation;

public interface IsolatedPlatform {
    void load();

    void enable();

    void disable();

    void shutdown();
}
