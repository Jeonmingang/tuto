package com.minkang.tutorial.listeners;

import com.minkang.tutorial.TutorialWarpPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class FirstJoinListener implements Listener {
    private final TutorialWarpPlugin plugin;
    public FirstJoinListener(TutorialWarpPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (p.hasPlayedBefore()) return;
        if (!plugin.getConfig().getBoolean("tutorial.first-join-enabled", true)) return;
        List<String> cmds = plugin.getConfig().getStringList("tutorial.first-join-commands");
        for (String raw : cmds) {
            String line = plugin.color(raw.trim());
            if (line.isEmpty()) continue;
            String replaced = line.replace("{player}", p.getName());
            if (line.regionMatches(true, 0, "player:", 0, 7)) {
                p.performCommand(replaced.substring(7).trim());
            } else if (line.regionMatches(true, 0, "op:", 0, 3)) {
                boolean was = p.isOp();
                try {
                    p.setOp(true);
                    p.performCommand(replaced.substring(3).trim());
                } finally {
                    p.setOp(was);
                }
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replaced.replaceFirst("(?i)^console:\\s*", ""));
            }
        }
    }
}
