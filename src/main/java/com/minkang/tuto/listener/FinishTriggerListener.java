package com.minkang.tuto.listener;

import com.minkang.tuto.TutoPlugin;
import com.minkang.tuto.model.Trigger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class FinishTriggerListener implements Listener {

    private final TutoPlugin plugin;
    private final Set<UUID> inRegion = new HashSet<>();

    public FinishTriggerListener(TutoPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!plugin.isFinishEnabled()) return;
        Player p = e.getPlayer();

        // Only react when block coordinates change (reduces spam)
        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) {
            return;
        }

        String mode = plugin.getFinishMode();
        if ("block".equalsIgnoreCase(mode)) {
            handleBlockMode(p);
        } else {
            handleLocationMode(p);
        }
    }

    private void handleBlockMode(Player p) {
        List<String> blocks = plugin.getBlockTypes();
        Block b = p.getLocation().getBlock();
        if (b == null) return;
        Material m = b.getType();
        for (String s : blocks) {
            try {
                Material want = Material.valueOf(s.toUpperCase(Locale.ROOT));
                if (m == want) {
                    triggerFinish(p, null);
                    break;
                }
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void handleLocationMode(Player p) {
        Location loc = p.getLocation();
        boolean inside = false;
        for (Trigger t : plugin.getTriggers()) {
            if (!t.worldName.equals(loc.getWorld().getName())) continue;
            if (t.radius <= 0) {
                if (loc.getBlockX() == (int)t.x && loc.getBlockY() == (int)t.y && loc.getBlockZ() == (int)t.z) {
                    inside = true; break;
                }
            } else {
                if (loc.getWorld().getUID().equals(t.worldUid) &&
                        loc.distanceSquared(new Location(loc.getWorld(), t.x, t.y, t.z)) <= t.radius * t.radius) {
                    inside = true; break;
                }
            }
        }
        if (inside) {
            if (inRegion.add(p.getUniqueId())) {
                triggerFinish(p, null);
            }
        } else {
            inRegion.remove(p.getUniqueId());
        }
    }

    private void triggerFinish(Player p, List<String> overrideCmds) {
        if (plugin.isOneTime() && plugin.getDataStore().isFinished(p.getUniqueId())) return;
        if (plugin.getDataStore().onCooldown(p.getUniqueId(), plugin.getCooldownSeconds())) return;

        List<String> cmds = (overrideCmds != null && !overrideCmds.isEmpty()) ? overrideCmds : plugin.getFinishCommands();

        Runnable run = () -> {
            for (String raw : cmds) {
                String line = raw.replace("{player}", p.getName());
                String lower = line.toLowerCase(Locale.ROOT);
                if (lower.startsWith("console:")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), line.substring("console:".length()).trim());
                } else if (lower.startsWith("player:")) {
                    p.performCommand(line.substring("player:".length()).trim());
                } else if (lower.startsWith("op:")) { // safe op wrapper
                    boolean wasOp = p.isOp();
                    try {
                        p.setOp(true);
                        p.performCommand(line.substring("op:".length()).trim());
                    } finally {
                        p.setOp(wasOp);
                    }
                } else {
                    // default console
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), line);
                }
            }
            p.sendMessage(col(plugin.prefix() + plugin.msgFinish()));
            plugin.getDataStore().setFinished(p.getUniqueId());
            plugin.getDataStore().setCooldown(p.getUniqueId(), plugin.getCooldownSeconds());
        };

        int delay = plugin.getExecuteDelayTicks();
        if (delay > 0) Bukkit.getScheduler().runTaskLater(plugin, run, delay);
        else Bukkit.getScheduler().runTask(plugin, run);
    }

    private String col(String s) { return org.bukkit.ChatColor.translateAlternateColorCodes('&', s); }
}