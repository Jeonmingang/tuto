package com.minkang.tuto.listener;

import com.minkang.tuto.TutoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class FirstJoinListener implements Listener {
    private final TutoPlugin plugin;
    public FirstJoinListener(TutoPlugin plugin){ this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e){
        Player p = e.getPlayer();
        if (!plugin.isTutorialEnabled()) return;
        if (p.hasPlayedBefore()) return; // only first join
        String warp = plugin.tutorialWarp();
        // run a tick later to ensure player is fully spawned
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "warp " + warp + " " + p.getName());
        }, 20L);
    }
}
