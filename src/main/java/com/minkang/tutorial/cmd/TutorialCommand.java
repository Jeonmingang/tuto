package com.minkang.tutorial.cmd;

import com.minkang.tutorial.TutorialWarpPlugin;
import com.minkang.tutorial.TutorialWarpPlugin.BlockPoint;
import org.bukkit.Bukkit;
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
import java.util.stream.Collectors;

public class TutorialCommand implements CommandExecutor {
    private final TutorialWarpPlugin plugin;
    public TutorialCommand(TutorialWarpPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player p = (Player) sender;

        if (args.length == 0) {
            p.sendMessage("§e사용법: /" + label + " sethere|remove|list|clear|reload|test");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "sethere": {
                Location l = p.getLocation();
                BlockPoint bp = BlockPoint.of(l);
                plugin.addBlock(bp);
                p.sendMessage(plugin.prefix() + color("&a종료블럭 등록: &f") + bp.toString());
                return true;
            }
            case "remove": {
                boolean ok = plugin.removeNearest(p.getLocation());
                if (ok) {
                    p.sendMessage(plugin.prefix() + color("&c가장 가까운 종료블럭 삭제 완료."));
                } else {
                    p.sendMessage(plugin.prefix() + color("&7삭제할 종료블럭을 찾지 못했습니다."));
                }
                return true;
            }
            case "list": {
                List<String> list = plugin.getBlocks().stream().map(BlockPoint::toString).sorted().collect(Collectors.toList());
                if (list.isEmpty()) {
                    p.sendMessage(plugin.prefix() + color("&7등록된 종료블럭이 없습니다."));
                } else {
                    p.sendMessage(plugin.prefix() + color("&e[종료블럭 목록]"));
                    for (String s : list) p.sendMessage(" §7- §f" + s);
                }
                return true;
            }
            case "clear": {
                plugin.getConfig().set("trigger.blocks", new ArrayList<>());
                plugin.saveConfig();
                plugin.reloadLocal();
                p.sendMessage(plugin.prefix() + color("&c모든 종료블럭 삭제."));
                return true;
            }
            case "reload": {
                plugin.reloadLocal();
                p.sendMessage(plugin.prefix() + color("&a리로드 완료."));
                return true;
            }
            case "test": {
                plugin.runOnTriggerActions(p);
                p.sendMessage(plugin.prefix() + color("&a테스트 액션 실행"));
                return true;
            }
            default:
                p.sendMessage("§e사용법: /" + label + " sethere|remove|list|clear|reload|test");
                return true;
        }
    }

    private String color(String s) { return s.replace("&", "§"); }
}
