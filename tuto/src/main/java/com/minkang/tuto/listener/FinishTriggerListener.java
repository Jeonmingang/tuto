package com.minkang.tuto.listener;

import com.minkang.tuto.TutoPlugin;
import com.minkang.tuto.model.Trigger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

public class FinishTriggerListener implements Listener {
    private final TutoPlugin plugin;
    private final Set<java.util.UUID> cooldown = new HashSet<java.util.UUID>();
    private final Map<String, Boolean> blockTypeCache = new HashMap<String, Boolean>();
    public FinishTriggerListener(TutoPlugin plugin){ this.plugin = plugin; }

    private boolean isFinishBlock(Material mat){
        String key = mat.name();
        Boolean cached = blockTypeCache.get(key);
        if (cached != null) return cached.booleanValue();
        java.util.List<String> list = plugin.getConfig().getStringList("finish.block-types");
        boolean match = false;
        for (String s : list){
            try { if (mat == Material.valueOf(s.toUpperCase())) { match = true; break; } }
            catch (IllegalArgumentException ignored){}
        }
        blockTypeCache.put(key, Boolean.valueOf(match));
        return match;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
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

        boolean matched = false;
        java.util.List<String> runCmds = null;

        if (plugin.isLocationMode()){
            for (Trigger t : plugin.triggers()){
                if (t.matches(p.getLocation())){ matched = true; runCmds = t.commands; break; }
            }
        } else {
            Material feet = p.getLocation().getBlock().getType();
            if (isFinishBlock(feet)) matched = true;
        }

        if (!matched) return;

        int cd = plugin.getConfig().getInt("finish.cooldown-seconds", 2);
        if (cooldown.contains(u)) return;
        cooldown.add(u);
        new BukkitRunnable(){ public void run(){ cooldown.remove(u);} }.runTaskLater(plugin, cd*20L);

        final java.util.List<String> cmds = (runCmds!=null? runCmds : plugin.getConfig().getStringList("finish.commands"));
        int delay = plugin.getConfig().getInt("finish.execute-delay-ticks", 0);
        if (delay <= 0){
            for (String raw : cmds){ runCmd(raw, p); }
        } else {
            new BukkitRunnable(){ public void run(){ for (String raw : cmds){ runCmd(raw, p); } } }.runTaskLater(plugin, delay);
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
            case PLAYER:
                p.performCommand(cmd);
                break;
            case OP:
                boolean was = p.isOp();
                try{
                    if (!was) p.setOp(true);
                    p.performCommand(cmd);
                } finally {
                    if (!was) p.setOp(false);
                }
                break;
            default:
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }
    enum ExecMode { CONSOLE, PLAYER, OP }
}
