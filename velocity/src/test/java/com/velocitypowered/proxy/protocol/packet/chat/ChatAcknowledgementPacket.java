package com.velocitypowered.proxy.protocol.packet.chat;

public final class ChatAcknowledgementPacket {
    private final int offset;

    public ChatAcknowledgementPacket(int offset) {
        this.offset = offset;
    }

    public int offset() {
        return offset;
    }
}
