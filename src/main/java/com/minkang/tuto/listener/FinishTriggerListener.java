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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FinishTriggerListener implements Listener {
    private final TutoPlugin plugin;
    private final Set<UUID> cooldown = new HashSet<UUID>();
    private final Map<String, Boolean> blockTypeCache = new HashMap<String, Boolean>();

    public FinishTriggerListener(TutoPlugin plugin){ this.plugin = plugin; }

    private boolean isFinishBlock(Material mat){
        String key = mat.name();
        Boolean cached = blockTypeCache.get(key);
        if (cached != null) return cached.booleanValue();
        List<String> list = plugin.getConfig().getStringList("finish.block-types");
        boolean match = false;
        for (String s : list){
            try { if (mat == Material.valueOf(s.toUpperCase())) { match = true; break; } } catch (Exception ignore){}
        }
        blockTypeCache.put(key, match);
        return match;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e){
        if (!plugin.isFinishEnabled()) return;
        Player p = e.getPlayer();

        // Only check when block position changes
        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
         && e.getFrom().getBlockY() == e.getTo().getBlockY()
         && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) {
            return;
        }

        if (plugin.getConfig().getBoolean("finish.require-sneak-to-complete", false)) {
            if (!p.isSneaking()) return;
        }

        UUID u = p.getUniqueId();
        boolean oneTime = plugin.getConfig().getBoolean("finish.one-time-per-player", true);
        if (oneTime && plugin.data().isCompleted(u)) return;

        int cdSec = plugin.getConfig().getInt("finish.cooldown-seconds", 2);
        if (cdSec > 0) {
            if (cooldown.contains(u)) return;
        }

        boolean matched = false;
        List<Trigger> list = plugin.getTriggers();
        if (plugin.isLocationMode()) {
            for (Trigger t : list) {
                if (t.matches(p.getLocation())) { matched = true; break; }
            }
        } else {
            // block mode — step on configured block type
            Material under = p.getLocation().clone().add(0, -1, 0).getBlock().getType();
            matched = isFinishBlock(under);
        }

        if (!matched) return;

        // Mark complete and cooldown
        plugin.data().setCompleted(u, true);
        if (cdSec > 0) {
            cooldown.add(u);
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable(){
                @Override public void run(){ cooldown.remove(u); }
            }, cdSec * 20L);
        }

        // Feedback + commands
        final String msg = plugin.getConfig().getString("messages.finished", "&a튜토리얼 완료!");
        p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', msg));

        int delay = plugin.getConfig().getInt("finish.execute-delay-ticks", 0);
        new BukkitRunnable(){
            @Override public void run(){
                runFinishCommands(p);
            }
        }.runTaskLater(plugin, Math.max(0, delay));
    }

    private void runFinishCommands(Player p){
        // Pick per-trigger commands if matching trigger has its own, else global default list
        List<String> commands = null;
        if (plugin.isLocationMode()) {
            for (Trigger t : plugin.getTriggers()) {
                if (t.matches(p.getLocation()) && t.commands != null && !t.commands.isEmpty()) {
                    commands = t.commands;
                    break;
                }
            }
        }
        if (commands == null || commands.isEmpty()) {
            commands = plugin.getConfig().getStringList("finish.commands");
        }
        for (String raw : commands) {
            String line = raw == null ? "" : raw.trim();
            if (line.length() == 0) continue;
            String replaced = line.replace("{player}", p.getName());
            ExecMode mode = ExecMode.CONSOLE;
            String cmd = replaced;
            int colon = replaced.indexOf(':');
            if (colon > 0) {
                String prefix = replaced.substring(0, colon).trim().toLowerCase();
                cmd = replaced.substring(colon+1).trim();
                if (prefix.equals("player")) mode = ExecMode.PLAYER;
                else if (prefix.equals("op")) mode = ExecMode.OP;
                else mode = ExecMode.CONSOLE;
            }
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
    }

    enum ExecMode { CONSOLE, PLAYER, OP }
}
