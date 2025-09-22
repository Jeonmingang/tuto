package com.minkang.tuto.listener;

import com.minkang.tuto.TutoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

public class FinishTriggerListener implements Listener {
    private final TutoPlugin plugin;
    private final Set<java.util.UUID> cooldown = new HashSet<>();
    private final Map<String, Boolean> blockTypeCache = new HashMap<>();
    public FinishTriggerListener(TutoPlugin plugin){ this.plugin = plugin; }

    private boolean isFinishBlock(Material mat){
        String key = mat.name();
        return blockTypeCache.computeIfAbsent(key, k -> {
            java.util.List<String> list = plugin.getConfig().getStringList("finish.block-types");
            for (String s : list){
                try { if (mat == Material.valueOf(s.toUpperCase())) return true; }
                catch (IllegalArgumentException ignored){}
            }
            return false;
        });
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e){
        if (!plugin.getConfig().getBoolean("finish.enabled", true)) return;
        if (e.getTo()==null) return;
        if (e.getTo().getBlockX()==e.getFrom().getBlockX()
         && e.getTo().getBlockY()==e.getFrom().getBlockY()
         && e.getTo().getBlockZ()==e.getFrom().getBlockZ()) return;
        Player p = e.getPlayer();
        java.util.UUID u = p.getUniqueId();

        if (plugin.getConfig().getBoolean("finish.one-time-per-player", true)
                && plugin.data().isCompleted(u)) return;
        if (plugin.getConfig().getBoolean("finish.require-sneak", false) && !p.isSneaking()) return;

        Material feet = p.getLocation().getBlock().getType();
        if (!isFinishBlock(feet)) return;

        int cd = plugin.getConfig().getInt("finish.cooldown-seconds", 2);
        if (cooldown.contains(u)) return;
        cooldown.add(u);
        new BukkitRunnable(){ public void run(){ cooldown.remove(u);} }.runTaskLater(plugin, cd*20L);

        for (String raw : plugin.getConfig().getStringList("finish.commands")){
            runCmd(raw, p);
        }
        if (plugin.getConfig().getBoolean("finish.one-time-per-player", true)){
            plugin.data().setCompleted(u, true);
        }
        String msg = plugin.getConfig().getString("messages.finished", "&a튜토리얼 완료!");
        p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', msg));
    }

    private void runCmd(String raw, Player p){
        String line = raw.trim();
        String cmd = line;
        ExecMode mode = ExecMode.CONSOLE;
        if (line.startsWith("console:")) { mode=ExecMode.CONSOLE; cmd=line.substring(8).trim(); }
        else if (line.startsWith("player:")) { mode=ExecMode.PLAYER; cmd=line.substring(7).trim(); }
        else if (line.startsWith("op:")) { mode=ExecMode.OP; cmd=line.substring(3).trim(); }
        cmd = cmd.replace("{player}", p.getName()).replace("{uuid}", p.getUniqueId().toString());
        switch (mode){
            case PLAYER -> p.performCommand(cmd);
            case OP -> {
                boolean was = p.isOp();
                try{ if (!was) p.setOp(true); p.performCommand(cmd);} finally { if (!was) p.setOp(false); }
            }
            default -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }
    enum ExecMode { CONSOLE, PLAYER, OP }
}
