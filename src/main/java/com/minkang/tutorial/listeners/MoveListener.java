package com.minkang.tutorial.listeners;

import com.minkang.tutorial.TutorialWarpPlugin;
import com.minkang.tutorial.TutorialWarpPlugin.BlockPoint;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class MoveListener implements Listener {
    private final TutorialWarpPlugin plugin;
    public MoveListener(TutorialWarpPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getTo() == null) return;
        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
            && e.getFrom().getBlockY() == e.getTo().getBlockY()
            && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) {
            return; // same block -> ignore spam
        }
        Player p = e.getPlayer();
        if (p.hasPermission("tutorial.bypass")) return;

        FileConfiguration c = plugin.getConfig();
        if (!c.getBoolean("trigger.require-sneak", false) || p.isSneaking()) {
            String mode = c.getString("trigger.mode", "block");
            if ("block".equalsIgnoreCase(mode)) {
                // check block under feet
                Location under = e.getTo().clone();
                under.setY(Math.floor(under.getY()) - 1);
                BlockPoint bp = BlockPoint.of(under);
                if (plugin.getTriggerBlocks().contains(bp)) {
                    runActions(p, e.getTo());
                }
            } else {
                // radius mode
                double radius = c.getDouble("trigger.radius", 1.0);
                for (BlockPoint bp : plugin.getTriggerBlocks()) {
                    Location center = bp.toLocation();
                    if (center != null && center.getWorld().equals(e.getTo().getWorld())) {
                        if (center.distanceSquared(e.getTo()) <= radius * radius) {
                            runActions(p, e.getTo());
                            break;
                        }
                    }
                }
            }
        }
    }

    private void runActions(Player p, Location loc) {
        for (String raw : plugin.getConfig().getStringList("on-trigger.actions")) {
            if (raw == null || raw.trim().isEmpty()) continue;
            String line = raw.replace("{player}", p.getName())
                    .replace("{world}", loc.getWorld().getName())
                    .replace("{x}", Integer.toString(loc.getBlockX()))
                    .replace("{y}", Integer.toString(loc.getBlockY()))
                    .replace("{z}", Integer.toString(loc.getBlockZ()));
            if (line.regionMatches(true, 0, "console:", 0, 8)) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), line.substring(8).trim());
            } else if (line.regionMatches(true, 0, "player:", 0, 7)) {
                p.performCommand(line.substring(7).trim());
            } else if (line.regionMatches(true, 0, "op:", 0, 3)) {
                boolean was = p.isOp();
                try {
                    p.setOp(true);
                    p.performCommand(line.substring(3).trim());
                } finally { p.setOp(was); }
            } else if (line.regionMatches(true, 0, "title:", 0, 6)) {
                p.sendTitle(color(line.substring(6).trim()), "", 10, 60, 10);
            } else if (line.regionMatches(true, 0, "subtitle:", 0, 9)) {
                p.sendTitle("", color(line.substring(9).trim()), 10, 60, 10);
            } else if (line.regionMatches(true, 0, "sound:", 0, 6)) {
                try {
                    p.playSound(p.getLocation(), Sound.valueOf(line.substring(6).trim().toUpperCase()), 1f, 1f);
                } catch (IllegalArgumentException ignored) {}
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), line.trim());
            }
        }
    }

    private String color(String s) { return s.replace("&", "§"); }
}
