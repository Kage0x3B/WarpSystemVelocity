package de.syscy.warpsystem.transfer.packets.utils;

public interface PacketHandler {
    void handle(Packet packet, String... extra);
}
