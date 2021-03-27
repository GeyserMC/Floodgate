package org.geysermc.floodgate.mixin_interface;

public interface HandshakeS2CPacketAddressGetter {
    String getAddress();

    //TODO packet fields will become final in 1.17
    void setAddress(String address);
}
