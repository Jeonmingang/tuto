package com.minkang.tuto.listener;

import com.minkang.tuto.TutoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class FirstJoinListener implements Listener {

    private final TutoPlugin plugin;

    public FirstJoinListener(TutoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!plugin.getConfig().getBoolean("tutorial.enabled", true)) return;
        if (p.hasPlayedBefore()) return;
        String warp = plugin.getConfig().getString("tutorial.warp", "tutorial");
        // essentials: /warp <name>
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "warp " + warp + " " + p.getName());
    }
}