package com.minkang.tutorial.listeners;

import com.minkang.tutorial.TutorialWarpPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MoveListener implements Listener {
    private final TutorialWarpPlugin plugin;
    private final Set<UUID> cooldown = new HashSet<>();

    public MoveListener(TutorialWarpPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;

        Player p = e.getPlayer();
        if (p.hasPermission("tutorial.bypass")) return;

        if (plugin.isOncePerPlayer() && plugin.isCompleted(p)) {
            // 이미 완료자는 스킵
            return;
        }

        boolean sneaking = p.isSneaking();
        if (plugin.isTriggered(e.getTo(), sneaking)) {
            if (cooldown.add(p.getUniqueId())) {
                plugin.runOnTriggerActions(p);
                // 중복발동 방지 2초
                p.getServer().getScheduler().runTaskLater(plugin, () -> cooldown.remove(p.getUniqueId()), 40L);
            }
        }
    }
}
