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
        final Player p = e.getPlayer();
        if (!plugin.isTutorialEnabled()) return;
        if (p.hasPlayedBefore()) return; // only first join
        final String warp = plugin.tutorialWarp();
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable(){
            @Override public void run(){
                if (!p.isOnline()) return;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "warp " + warp + " " + p.getName());
            }
        }, 20L);
    }
}
