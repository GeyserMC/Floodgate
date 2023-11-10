package org.geysermc.floodgate.core.crypto.exception;

import org.geysermc.floodgate.core.util.Constants;

public final class UnsupportedVersionException extends Exception {
    public UnsupportedVersionException(int expected, int received) {
        super(Constants.UNSUPPORTED_DATA_VERSION.formatted(expected, received));
    }
}
