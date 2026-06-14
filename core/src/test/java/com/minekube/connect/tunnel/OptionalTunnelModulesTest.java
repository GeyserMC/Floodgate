package com.minekube.connect.tunnel;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.junit.jupiter.api.Test;

class OptionalTunnelModulesTest {

    @Test
    void keepsModulesWhenLibp2pModuleIsNotOnClasspath() {
        Module module = new AbstractModule() {
        };

        assertArrayEquals(new Module[] {module}, OptionalTunnelModules.append(module));
    }
}
