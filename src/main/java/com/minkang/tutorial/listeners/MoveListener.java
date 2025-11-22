package com.minkang.tutorial.listeners;

import com.minkang.tutorial.TutorialWarpPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 플레이어 이동을 감지하여 튜토리얼 종료 블럭 / 반경 진입 시
 * TutorialWarpPlugin#handleTrigger 를 호출한다.
 *
 * 플레이어별로 짧은 쿨타임을 두어 중복 호출을 방지한다.
 */
public class MoveListener implements Listener {

    private final TutorialWarpPlugin plugin;
    private final Set<UUID> cooldown = new HashSet<>();

    public MoveListener(TutorialWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) {
            return;
        }

        Player p = e.getPlayer();
        boolean sneaking = p.isSneaking();

        if (plugin.isTriggered(e.getTo(), sneaking)) {
            if (cooldown.add(p.getUniqueId())) {
                try {
                    plugin.handleTrigger(p);
                } finally {
                    // 2초 후 쿨타임 제거
                    p.getServer().getScheduler().runTaskLater(plugin, () -> cooldown.remove(p.getUniqueId()), 40L);
                }
            }
        }
    }
}
