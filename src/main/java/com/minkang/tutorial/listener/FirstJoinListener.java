package com.minkang.tutorial.listener;

import com.minkang.tutorial.TutorialPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class FirstJoinListener implements Listener {

    private final TutorialPlugin plugin;

    public FirstJoinListener(TutorialPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!plugin.tutorialEnabled()) return;
        if (p.hasPlayedBefore()) return;

        // 안내
        p.sendMessage(plugin.msg("prefix","&6[Tutorial]&r ")
                + plugin.msg("first-join", "&e튜토리얼 지역으로 이동합니다."));

        // warp
        String warp = plugin.firstWarp();
        if (warp != null && !warp.trim().isEmpty()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "warp " + warp + " " + p.getName());
        }

        // extra commands
        for (String raw : plugin.firstCommands()) {
            String line = plugin.ph(raw, p.getUniqueId(), p.getLocation());
            execLine(p, line);
        }
    }

    private void execLine(Player p, String line) {
        String lower = line.toLowerCase();
        if (lower.startsWith("console:")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), line.substring("console:".length()).trim());
        } else if (lower.startsWith("player:")) {
            p.performCommand(line.substring("player:".length()).trim());
        } else if (lower.startsWith("op:")) {
            boolean was = p.isOp();
            try { p.setOp(true); p.performCommand(line.substring("op:".length()).trim()); }
            finally { p.setOp(was); }
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), line);
        }
    }
}