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
        if (!plugin.firstJoinEnabled()) return;
        Player p = e.getPlayer();
        if (p.hasPlayedBefore()) return;

        String warp = plugin.firstJoinWarp();
        if (warp != null && !warp.isEmpty()) {
            // EssentialsX /warp <name> <player>
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "warp " + warp + " " + p.getName());
            p.sendMessage(TutorialPlugin.color(plugin.prefix() + plugin.msg("first-join","&e튜토리얼 지역으로 이동합니다.")));
        }
        for (String raw : plugin.firstJoinCmds()) {
            execLine(p, raw);
        }
    }

    private void execLine(Player p, String line) {
        String lower = line.toLowerCase();
        String body = line.substring(line.indexOf(':')+1).trim();
        if (lower.startsWith("console:")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), body);
        } else if (lower.startsWith("player:")) {
            p.performCommand(body);
        } else if (lower.startsWith("op:")) {
            boolean was = p.isOp();
            try { p.setOp(true); p.performCommand(body); }
            finally { p.setOp(was); }
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), line);
        }
    }
}
