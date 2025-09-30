package com.minkang.tutorial.cmd;

import com.minkang.tutorial.TutorialWarpPlugin;
import com.minkang.tutorial.TutorialWarpPlugin.BlockPoint;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TutorialCommand implements CommandExecutor {
    private final TutorialWarpPlugin plugin;
    public TutorialCommand(TutorialWarpPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Players only."); return true; }
        Player p = (Player) sender;
        if (!p.hasPermission("tutorial.admin")) { p.sendMessage("§c권한이 없습니다."); return true; }

        if (args.length == 0) {
            p.sendMessage("§e사용법: /" + label + " sethere|remove|list|clear|reload|test");
            return true;
        }

        FileConfiguration c = plugin.getConfig();
        switch (args[0].toLowerCase()) {
            case "sethere": {
                Location under = p.getLocation().clone();
                under.setY(Math.floor(under.getY()) - 1);
                BlockPoint bp = BlockPoint.of(under);
                List<String> list = c.getStringList("trigger.blocks");
                list.add(bp.toString());
                c.set("trigger.blocks", list);
                plugin.saveConfig();
                plugin.reloadAll();
                p.sendMessage(plugin.prefix() + "§a등록 완료: §f" + bp.toString());
                return true;
            }
            case "remove": {
                // remove nearest
                if (plugin.getTriggerBlocks().isEmpty()) { p.sendMessage(plugin.prefix() + c.getString("messages.none")); return true; }
                BlockPoint nearest = null;
                double best = Double.MAX_VALUE;
                for (BlockPoint bp : plugin.getTriggerBlocks()) {
                    if (p.getWorld().getName().equals(bp.world)) {
                        double d = p.getLocation().distanceSquared(bp.toLocation());
                        if (d < best) { best = d; nearest = bp; }
                    }
                }
                if (nearest == null) { p.sendMessage(plugin.prefix() + "§7가까운 종료블럭이 없습니다."); return true; }
                List<String> list = new ArrayList<>(c.getStringList("trigger.blocks"));
                list.remove(nearest.toString());
                c.set("trigger.blocks", list);
                plugin.saveConfig();
                plugin.reloadAll();
                p.sendMessage(plugin.prefix() + c.getString("messages.removed-nearest")
                        .replace("%world%", nearest.world).replace("%x%", ""+nearest.x).replace("%y%", ""+nearest.y).replace("%z%", ""+nearest.z));
                return true;
            }
            case "list": {
                List<String> list = c.getStringList("trigger.blocks");
                if (list.isEmpty()) { p.sendMessage(plugin.prefix() + c.getString("messages.none")); return true; }
                p.sendMessage(plugin.prefix() + c.getString("messages.list-header"));
                for (String s : list) {
                    BlockPoint bp = BlockPoint.parse(s);
                    if (bp != null) {
                        p.sendMessage(plugin.prefix() + c.getString("messages.list-entry")
                                .replace("%world%", bp.world).replace("%x%", ""+bp.x).replace("%y%", ""+bp.y).replace("%z%", ""+bp.z));
                    }
                }
                return true;
            }
            case "clear": {
                c.set("trigger.blocks", new ArrayList<String>());
                plugin.saveConfig();
                plugin.reloadAll();
                p.sendMessage(plugin.prefix() + c.getString("messages.cleared"));
                return true;
            }
            case "reload": {
                plugin.reloadAll();
                p.sendMessage(plugin.prefix() + "§a리로드 완료");
                return true;
            }
            case "test": {
                // run actions on current location
                try {
                    Class<?> moveListener = Class.forName("com.minkang.tutorial.listeners.MoveListener");
                    // simple reflective call is overkill; just duplicate tiny logic:
                    for (String raw : plugin.getConfig().getStringList("on-trigger.actions")) {
                        String line = raw.replace("{player}", p.getName());
                        if (line.toLowerCase().startsWith("console:")) {
                            p.getServer().dispatchCommand(p.getServer().getConsoleSender(), line.substring(8).trim());
                        } else if (line.toLowerCase().startsWith("player:")) {
                            p.performCommand(line.substring(7).trim());
                        } else if (line.toLowerCase().startsWith("op:")) {
                            boolean was = p.isOp();
                            try { p.setOp(true); p.performCommand(line.substring(3).trim()); }
                            finally { p.setOp(was); }
                        } else if (line.toLowerCase().startsWith("title:")) {
                            p.sendTitle(color(line.substring(6).trim()), "", 10, 60, 10);
                        } else if (line.toLowerCase().startsWith("subtitle:")) {
                            p.sendTitle("", color(line.substring(9).trim()), 10, 60, 10);
                        } else {
                            p.getServer().dispatchCommand(p.getServer().getConsoleSender(), line.trim());
                        }
                    }
                } catch (Exception ignored) {}
                p.sendMessage(plugin.prefix() + "§a테스트 액션 실행");
                return true;
            }
            default:
                p.sendMessage("§e사용법: /" + label + " sethere|remove|list|clear|reload|test");
                return true;
        }
    }

    private String color(String s) { return s.replace("&", "§"); }
}
