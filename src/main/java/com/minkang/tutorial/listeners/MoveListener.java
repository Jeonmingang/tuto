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
        Player p = e.getPlayer();
        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;

        FileConfiguration cfg = plugin.getConfig();

        // Trigger mode check
        String mode = cfg.getString("trigger.mode", "block").toLowerCase();
        boolean shouldTrigger = false;

        if ("block".equals(mode)) {
            // If player's block matches any configured trigger block, trigger
            for (BlockPoint bp : plugin.getTriggerBlocks()) {
                if (bp.matches(e.getTo())) {
                    shouldTrigger = true;
                    break;
                }
            }
        } else { // radius
            double radius = cfg.getDouble("trigger.radius", 1.0D);
            for (BlockPoint bp : plugin.getTriggerBlocks()) {
                Location loc = bp.toLocation();
                if (loc != null && loc.getWorld() == e.getTo().getWorld() && loc.distance(e.getTo()) <= radius) {
                    shouldTrigger = true;
                    break;
                }
            }
        }

        if (!shouldTrigger) return;

        // Sneak requirement
        if (cfg.getBoolean("trigger.require-sneak", false) && !p.isSneaking()) {
            return;
        }

        // Perform actions
        for (String raw : cfg.getStringList("on-trigger.actions")) {
            if (raw == null || raw.trim().isEmpty()) continue;
            String line = replacePlaceholders(raw, p);

            // Recognize action prefixes similar to FirstJoinListener
            if (line.regionMatches(true, 0, "title:", 0, 6)) {
                p.sendTitle(color(line.substring(6).trim()), "", 10, 60, 10);
            } else if (line.regionMatches(true, 0, "subtitle:", 0, 9)) {
                p.sendTitle("", color(line.substring(9).trim()), 10, 60, 10);
            } else if (line.regionMatches(true, 0, "sound:", 0, 6)) {
                try {
                    p.playSound(p.getLocation(), Sound.valueOf(line.substring(6).trim().toUpperCase()), 1f, 1f);
                } catch (IllegalArgumentException ignored) {}
            } else if (line.regionMatches(true, 0, "console:", 0, 8)) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), line.substring(8).trim());
            } else if (line.regionMatches(true, 0, "player:", 0, 7)) {
                p.performCommand(line.substring(7).trim());
            } else if (line.regionMatches(true, 0, "op:", 0, 3)) {
                boolean was = p.isOp();
                try {
                    p.setOp(true);
                    p.performCommand(line.substring(3).trim());
                } finally {
                    p.setOp(was);
                }
            } else {
                // Backward compatibility: treat unprefixed lines as console commands.
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), line.trim());
            }
        }
    }

    private String replacePlaceholders(String s, Player p) {
        Location l = p.getLocation();
        return s.replace("{player}", p.getName())
                .replace("{world}", l.getWorld()!=null?l.getWorld().getName():"world")
                .replace("{x}", Integer.toString(l.getBlockX()))
                .replace("{y}", Integer.toString(l.getBlockY()))
                .replace("{z}", Integer.toString(l.getBlockZ()));
    }

    private String color(String s) { return s.replace("&", "§"); }
}
