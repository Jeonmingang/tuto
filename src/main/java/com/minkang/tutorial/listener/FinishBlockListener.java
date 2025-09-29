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
    private final Map<UUID, Long> cooldown = new HashMap<>();

    public FinishBlockListener(TutorialPlugin plugin) { this.plugin = plugin; }

    // Block pick
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        if (!plugin.isSelecting(p.getUniqueId())) return;
        plugin.endSelecting(p.getUniqueId());
        plugin.addFinishBlock(e.getClickedBlock().getLocation());
        Location l = e.getClickedBlock().getLocation();
        p.sendMessage(plugin.msg("prefix","&6[Tutorial]&r ")
                + plugin.msg("set-finish-done","&a종료블럭 추가: &f%world% %x%,%y%,%z%")
                    .replace("%world%", l.getWorld().getName())
                    .replace("%x%", String.valueOf(l.getBlockX()))
                    .replace("%y%", String.valueOf(l.getBlockY()))
                    .replace("%z%", String.valueOf(l.getBlockZ())));
    }

    // Trigger
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!plugin.finishEnabled()) return;
        Player p = e.getPlayer();
        if (p.hasPermission(plugin.bypassPerm())) return;

        if (plugin.requireSneak() && !p.isSneaking()) return;

        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;

        long now = System.currentTimeMillis();
        long until = cooldown.getOrDefault(p.getUniqueId(), 0L);
        if (until > now) return;

        String mode = plugin.triggerMode();
        Location playerLoc = p.getLocation();
        boolean hit = false;
        Location hitBlock = null;

        if ("radius".equalsIgnoreCase(mode)) {
            double r2 = plugin.triggerRadius() * plugin.triggerRadius();
            for (Location l : plugin.getFinishBlocks()) {
                if (l.getWorld() == null || !l.getWorld().equals(playerLoc.getWorld())) continue;
                if (playerLoc.distanceSquared(l) <= r2) {
                    hit = true; hitBlock = l; break;
                }
            }
        } else { // block
            Location feet = playerLoc.clone().subtract(0, 1, 0);
            for (Location l : plugin.getFinishBlocks()) {
                if (l.getWorld() == null || !l.getWorld().equals(feet.getWorld())) continue;
                if (l.getBlockX() == feet.getBlockX()
                        && l.getBlockY() == feet.getBlockY()
                        && l.getBlockZ() == feet.getBlockZ()) {
                    hit = true; hitBlock = l; break;
                }
            }
        }

        if (!hit) return;

        if (plugin.oneTime() && plugin.store().isFinished(p.getUniqueId())) return;

        // cooldown mark
        cooldown.put(p.getUniqueId(), now + plugin.cooldownSeconds()*1000L);

        // Do actions
        runActions(p, hitBlock);
        plugin.store().setFinished(p.getUniqueId());
        plugin.store().save();
    }

    private void runActions(Player p, Location origin) {
        List<String> actions = plugin.actions();
        if (actions == null || actions.isEmpty()) {
            // fallback: warp spawn or world spawn
            String warp = "spawn";
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "warp " + warp + " " + p.getName());
            return;
        }
        for (String raw : actions) {
            String line = plugin.ph(raw, p.getUniqueId(), origin);
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
                } else if (lower.startsWith("warp:")) {
                    String name = line.substring("warp:".length()).trim();
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "warp " + name + " " + p.getName());
                } else if (lower.startsWith("teleport:")) {
                    String arg = line.substring("teleport:".length()).trim();
                    if ("world-spawn".equalsIgnoreCase(arg)) {
                        p.teleport(p.getWorld().getSpawnLocation());
                    } else {
                        // format: world:x,y,z
                        String[] sp = arg.split(":");
                        if (sp.length == 2) {
                            String world = sp[0];
                            String[] xyz = sp[1].split(",");
                            if (xyz.length == 3) {
                                org.bukkit.World w = Bukkit.getWorld(world);
                                if (w != null) {
                                    double x = Double.parseDouble(xyz[0]);
                                    double y = Double.parseDouble(xyz[1]);
                                    double z = Double.parseDouble(xyz[2]);
                                    p.teleport(new Location(w, x, y, z));
                                }
                            }
                        }
                    }
                } else if (lower.startsWith("message:")) {
                    p.sendMessage(line.substring("message:".length()).trim());
                } else if (lower.startsWith("title:")) {
                    String title = line.substring("title:".length()).trim();
                    p.sendTitle(title, "", 10, 60, 10);
                } else if (lower.startsWith("subtitle:")) {
                    String sub = line.substring("subtitle:".length()).trim();
                    p.sendTitle("", sub, 10, 60, 10);
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