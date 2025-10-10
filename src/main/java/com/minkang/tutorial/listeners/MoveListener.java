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
 * Fires tutorial completion when a player enters any configured trigger.
 * Cooldowns per player prevent rapid re-firing (40 ticks).
 */
public class MoveListener implements Listener {
    private final TutorialWarpPlugin plugin;
    private final Set<UUID> cooldown = new HashSet<>();

    public MoveListener(TutorialWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent e) {
        if (e.getTo() == null) return;
        Player p = e.getPlayer();

        // bypass permission skips the tutorial trigger entirely
        if (p.hasPermission("tutorial.bypass")) return;

        // If tutorial is once-per-player and already completed, skip
        if (plugin.isOncePerPlayer() && plugin.isCompleted(p)) {
            return;
        }

        boolean sneaking = p.isSneaking();
        if (plugin.isTriggered(e.getTo(), sneaking)) {
            if (cooldown.add(p.getUniqueId())) {
                try {
                    plugin.runOnTriggerActions(p);
                } finally {
                    // remove cooldown after 2 seconds
                    p.getServer().getScheduler().runTaskLater(plugin, () -> cooldown.remove(p.getUniqueId()), 40L);
                }
            }
        }
    }
}
