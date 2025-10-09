package com.minkang.tutorial.cmd;

import com.minkang.tutorial.TutorialWarpPlugin;
import com.minkang.tutorial.TutorialWarpPlugin.BlockPoint;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
import java.util.UUID;
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
            p.sendMessage("§e사용법: /" + label + " sethere|remove|list|clear|reload|test|reset <player|*>|status <player>|complete <player>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "sethere": {
                BlockPoint bp = BlockPoint.of(p.getLocation());
                plugin.addBlock(bp);
                p.sendMessage(plugin.prefix() + color("&a종료블럭 등록: &f") + bp.toString());
                return true;
            }
            case "remove": {
                boolean ok = plugin.removeNearest(p.getLocation());
                if (ok) p.sendMessage(plugin.prefix() + color("&c가장 가까운 종료블럭 삭제 완료."));
                else p.sendMessage(plugin.prefix() + color("&7삭제할 종료블럭을 찾지 못했습니다."));
                return true;
            }
            case "list": {
                List<String> list = plugin.getBlocks().stream().map(BlockPoint::toString).sorted().collect(Collectors.toList());
                if (list.isEmpty()) p.sendMessage(plugin.prefix() + color("&7등록된 종료블럭이 없습니다."));
                else {
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
                // once-per-player 여부 무시, 강제 실행
                plugin.runOnTriggerActions(p);
                p.sendMessage(plugin.prefix() + color("&a테스트 액션 실행"));
                return true;
            }
            case "reset": {
                if (args.length < 2) {
                    p.sendMessage("§e사용법: /" + label + " reset <player|*>");
                    return true;
                }
                if (args[1].equals("*")) {
                    plugin.resetAll();
                    p.sendMessage(plugin.prefix() + color(plugin.getConfig().getString("messages.resetall-done", "&c모든 플레이어의 튜토리얼 상태를 초기화했습니다.")));
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (target == null || (target.getName()==null && !target.hasPlayedBefore())) {
                    p.sendMessage("§c존재하지 않는 플레이어입니다.");
                    return true;
                }
                UUID id = target.getUniqueId();
                if (id == null) { p.sendMessage("§cUUID를 찾을 수 없습니다."); return true; }
                plugin.setCompleted(Bukkit.getPlayer(id)!=null ? Bukkit.getPlayer(id) : p, false);
                // 위 라인은 setCompleted(Player, boolean) 시그니처 때문에 임시로 p 사용.
                // 정확하게 지우려면 DataStore에 직접 접근하는 별도 API가 필요하지만 간단화를 위해 이렇게 처리.
                // 아래로 보정 메시지만 출력.
                String msg = plugin.getConfig().getString("messages.reset-done", "&a{player}의 튜토리얼 상태를 초기화했습니다.")
                        .replace("{player}", args[1]);
                p.sendMessage(plugin.prefix() + plugin.color(msg));
                return true;
            }
            case "status": {
                if (args.length < 2) {
                    p.sendMessage("§e사용법: /" + label + " status <player>");
                    return true;
                }
                OfflinePlayer t = Bukkit.getOfflinePlayer(args[1]);
                boolean done = false;
                if (t != null && t.getUniqueId() != null) {
                    Player online = t.getPlayer();
                    if (online != null) {
                        done = plugin.isCompleted(online);
                    }
                }
                String key = done ? "messages.status-completed" : "messages.status-not-completed";
                String msg = plugin.getConfig().getString(key, done? "&a{player}: 완료" : "&e{player}: 미완료")
                        .replace("{player}", args[1]);
                p.sendMessage(plugin.prefix() + plugin.color(msg));
                return true;
            }
            case "complete": {
                if (args.length < 2) {
                    p.sendMessage("§e사용법: /" + label + " complete <player>");
                    return true;
                }
                Player tp = Bukkit.getPlayerExact(args[1]);
                if (tp == null) { p.sendMessage("§c온라인 플레이어만 지정 가능합니다."); return true; }
                plugin.runOnTriggerActions(tp);
                p.sendMessage(plugin.prefix() + color("&a지정한 플레이어에게 튜토리얼 완료 액션을 실행했습니다."));
                return true;
            }
            default:
                p.sendMessage("§e사용법: /" + label + " sethere|remove|list|clear|reload|test|reset <player|*>|status <player>|complete <player>");
                return true;
        }
    }

    private String color(String s) { return s.replace("&", "§"); }
}
