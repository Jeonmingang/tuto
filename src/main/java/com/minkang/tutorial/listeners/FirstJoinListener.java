package com.minkang.tutorial.listeners;

import com.minkang.tutorial.TutorialWarpPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class FirstJoinListener implements Listener {
    private final TutorialWarpPlugin plugin;
    public FirstJoinListener(TutorialWarpPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        final Player p = e.getPlayer();
        if (!plugin.getConfig().getBoolean("tutorial.first-join-enabled", true)) return;
        if (p.hasPlayedBefore()) return;

        final String warp = plugin.getConfig().getString("tutorial.warp-name", "tutorial");
        // Run a bit later to let other plugins (Essentials) initialize
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            // Prefer Essentials warp command; fallback to generic spawn
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "warp " + warp + " " + p.getName());
            for (String line : plugin.getConfig().getStringList("tutorial.first-join-commands")) {
                dispatchWithPrefix(line, p);
            }
        }, 20L);
    }

    private void dispatchWithPrefix(String line, Player p) {
        if (line == null || line.trim().isEmpty()) return;
        String repl = line.replace("{player}", p.getName());
        if (repl.regionMatches(true, 0, "console:", 0, 8)) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), repl.substring(8).trim());
        } else if (repl.regionMatches(true, 0, "player:", 0, 7)) {
            p.performCommand(repl.substring(7).trim());
        } else if (repl.regionMatches(true, 0, "op:", 0, 3)) {
            boolean was = p.isOp();
            try {
                p.setOp(true);
                p.performCommand(repl.substring(3).trim());
            } finally {
                p.setOp(was);
            }
        } else {
            // default: console
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), repl.trim());
        }
    }
}
