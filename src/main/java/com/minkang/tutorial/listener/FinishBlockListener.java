package com.minkang.tutorial.listener;

import com.minkang.tutorial.TutorialPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.*;

public class FinishBlockListener implements Listener {

    private final TutorialPlugin plugin;
    public FinishBlockListener(TutorialPlugin plugin) { this.plugin = plugin; }

    // Admin sets finish block by right clicking after command
    @EventHandler
    public void onRightClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        if (!plugin.awaiting().contains(p.getUniqueId())) return;
        Location l = e.getClickedBlock().getLocation();
        boolean added = plugin.addFinishBlock(l);
        if (added) {
            p.sendMessage(TutorialPlugin.color(plugin.prefix() + plugin.msg("set-finish-done","&a종료블럭 추가: &f%world% %x%,%y%,%z%")
                    .replace("%world%", l.getWorld().getName())
                    .replace("%x%", String.valueOf(l.getBlockX()))
                    .replace("%y%", String.valueOf(l.getBlockY()))
                    .replace("%z%", String.valueOf(l.getBlockZ()))));
        } else {
            p.sendMessage(TutorialPlugin.color("&c이미 등록된 블럭입니다."));
        }
        plugin.awaiting().remove(p.getUniqueId());
        e.setCancelled(true);
    }

    // Detect stepping on finish block (only when block position actually changes)
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!plugin.finishEnabled()) return;
        Player p = e.getPlayer();
        if (p.hasPermission("tutorial.bypass")) return;

        // Only react when block position changed to reduce spam
        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) {
            return;
        }

        if (plugin.triggerRequireSneak() && !p.isSneaking()) return;

        Location feet = p.getLocation();
        World w = feet.getWorld();
        if (w == null) return;

        boolean triggered = false;
        String mode = plugin.triggerMode();
        if ("radius".equalsIgnoreCase(mode)) {
            double r = plugin.triggerRadius();
            for (Location l : plugin.getFinishBlocks()) {
                if (!Objects.equals(l.getWorld(), w)) continue;
                if (l.clone().add(0.5, 0.5, 0.5).distance(feet) <= r) { triggered = true; break; }
            }
        } else {
            // block mode: block below player's feet equals a finish block
            Location under = new Location(w, feet.getBlockX(), feet.getBlockY()-1, feet.getBlockZ());
            for (Location l : plugin.getFinishBlocks()) {
                if (l.equals(under)) { triggered = true; break; }
            }
        }

        if (triggered) {
            runActions(p, feet);
            plugin.data().setFinished(p.getUniqueId());
            plugin.data().save();
        }
    }

    private void runActions(Player p, Location where) {
        List<String> actions = plugin.finishActions();
        for (String raw : actions) {
            if (raw == null || raw.trim().isEmpty()) continue;
            String line = plugin.ph(raw.trim(), p.getUniqueId(), where);
            String lower = line.toLowerCase(Locale.ROOT);

            try {
                if (lower.startsWith("console:")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), line.substring("console:".length()).trim());
                } else if (lower.startsWith("player:")) {
                    p.performCommand(line.substring("player:".length()).trim());
                } else if (lower.startsWith("op:")) {
                    boolean was = p.isOp();
                    try { p.setOp(true); p.performCommand(line.substring("op:".length()).trim()); }
                    finally { p.setOp(was); }
                } else if (lower.startsWith("tp:")) {
                    String[] parts = line.substring("tp:".length()).trim().split(",", 4);
                    if (parts.length >= 4) {
                        World w = Bukkit.getWorld(parts[0].trim());
                        double x = Double.parseDouble(parts[1].trim());
                        double y = Double.parseDouble(parts[2].trim());
                        double z = Double.parseDouble(parts[3].trim());
                        if (w != null) p.teleport(new Location(w, x, y, z));
                    }
                } else if (lower.startsWith("warp:")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "warp " + line.substring("warp:".length()).trim() + " " + p.getName());
                } else if (lower.startsWith("title:")) {
                    String title = line.substring("title:".length()).trim();
                    p.sendTitle(TutorialPlugin.color(title), "", 10, 60, 10);
                } else if (lower.startsWith("subtitle:")) {
                    String sub = line.substring("subtitle:".length()).trim();
                    p.sendTitle("", TutorialPlugin.color(sub), 10, 60, 10);
                } else if (lower.startsWith("sound:")) {
                    String snd = line.substring("sound:".length()).trim().toUpperCase(Locale.ROOT);
                    try {
                        p.playSound(p.getLocation(), Sound.valueOf(snd), 1.0f, 1.0f);
                    } catch (IllegalArgumentException ignore) {}
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), line);
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Action failed: " + raw + " -> " + t.getMessage());
            }
        }
    }
}
