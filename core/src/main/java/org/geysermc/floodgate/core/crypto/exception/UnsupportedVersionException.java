package org.geysermc.floodgate.core.crypto.exception;

public final class UnsupportedVersionException extends Exception {
    public UnsupportedVersionException(int expected, int received) {
        super("Expected Floodgate data version " + expected + ", received version " + received);
    }
}
