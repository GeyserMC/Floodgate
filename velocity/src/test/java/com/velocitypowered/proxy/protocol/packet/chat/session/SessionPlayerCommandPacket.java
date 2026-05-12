package com.velocitypowered.proxy.protocol.packet.chat.session;

import java.time.Instant;

public class SessionPlayerCommandPacket {
    protected String command;
    protected Instant timeStamp;

    public SessionPlayerCommandPacket() {
    }

    public SessionPlayerCommandPacket(String command, Instant timeStamp) {
        this.command = command;
        this.timeStamp = timeStamp;
    }

    public String getCommand() {
        return command;
    }

    public Instant getTimeStamp() {
        return timeStamp;
    }
}
