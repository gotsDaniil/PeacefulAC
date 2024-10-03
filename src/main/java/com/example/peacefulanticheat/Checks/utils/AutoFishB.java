package com.example.peacefulanticheat.Checks.utils;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AutoFishB extends PacketListenerAbstract {

    static ConcurrentMap<Player, Long> idleTimes = new ConcurrentHashMap<>();

    public AutoFishB() {
        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    public void updatePlayerActivity(Player player) {
        idleTimes.put(player, System.currentTimeMillis());
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        if (event.getPacketType() == PacketType.Play.Client.CHAT_MESSAGE) {
            updatePlayerActivity(player);
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_FLYING ||
                event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION ||
                event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            updatePlayerActivity(player);
        }
    }
}