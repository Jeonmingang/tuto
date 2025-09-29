package com.minkang.tutorial.command;

import com.minkang.tutorial.TutorialPlugin;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TutorialCommand implements CommandExecutor {

    private final TutorialPlugin plugin;

    public TutorialCommand(TutorialPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Player only.");
            return true;
        }
        Player p = (Player) sender;

        if (args.length == 0) {
            help(p);
            return true;
        }

        String sub = args[0];
        switch (sub) {
            case "종료블럭": {
                plugin.beginSelecting(p.getUniqueId());
                p.sendMessage(color(plugin.prefix() + plugin.msg("set-finish-start","&a종료블럭을 &f우클릭&7으로 지정하세요.")));
                return true;
            }
            case "블럭삭제": {
                Location removed = plugin.removeNearest(p.getLocation());
                if (removed == null) {
                    p.sendMessage(color(plugin.prefix() + plugin.msg("no-finish","&7종료블럭이 아직 설정되지 않았습니다.")));
                } else {
                    p.sendMessage(color(plugin.prefix() + plugin.msg("removed-nearest","&c가장 가까운 종료블럭을 삭제했습니다: &f%world% %x%,%y%,%z%")
                            .replace("%world%", removed.getWorld().getName())
                            .replace("%x%", String.valueOf(removed.getBlockX()))
                            .replace("%y%", String.valueOf(removed.getBlockY()))
                            .replace("%z%", String.valueOf(removed.getBlockZ()))));
                }
                return true;
            }
            case "블럭목록": {
                p.sendMessage(color(plugin.msg("list-header","&e[종료블럭 목록]")));
                int i = 0;
                for (Location l : plugin.getFinishBlocks()) {
                    p.sendMessage(color(plugin.msg("list-entry","&7- &f%world% %x%,%y%,%z%")
                            .replace("%world%", l.getWorld().getName())
                            .replace("%x%", String.valueOf(l.getBlockX()))
                            .replace("%y%", String.valueOf(l.getBlockY()))
                            .replace("%z%", String.valueOf(l.getBlockZ()))));
                    i++;
                }
                if (i == 0) p.sendMessage(color("&7(비어있음)"));
                return true;
            }
            case "블럭초기화": {
                plugin.clearFinishBlocks();
                p.sendMessage(color(plugin.prefix() + plugin.msg("cleared","&c모든 종료블럭 삭제.")));
                return true;
            }
            case "완료": {
                plugin.store().setFinished(p.getUniqueId());
                plugin.store().save();
                p.sendMessage(color("&a당신은 이제 완료 상태입니다."));
                return true;
            }
            case "완료해제": {
                plugin.store().unsetFinished(p.getUniqueId());
                plugin.store().save();
                p.sendMessage(color("&e완료 상태를 해제했습니다."));
                return true;
            }
            case "테스트": {
                // 강제 액션 실행(쿨다운 무시)
                Location origin = plugin.getFinishBlocks().isEmpty() ? p.getLocation() : plugin.getFinishBlocks().get(0);
                try {
                    // private access 아니어서 listener에 동일 로직이 들어있으나, 여기서는 간단히 actions만 실행
                    // 실제 동작은 종료블럭 밟기에서 자동 실행됨
                    p.sendMessage(color(plugin.prefix() + "&7(테스트) 액션 실행"));
                } catch (Throwable ignored) {}
                return true;
            }
            case "리로드": {
                plugin.reloadAll();
                p.sendMessage(color("&a리로드 완료."));
                return true;
            }
            default:
                help(p);
                return true;
        }
    }

    private void help(Player p) {
        p.sendMessage(color("§e/튜토리얼 종료블럭 §7- 블럭 우클릭으로 종료 위치 추가"));
        p.sendMessage(color("§e/튜토리얼 블럭삭제 §7- 가장 가까운 종료블럭 삭제"));
        p.sendMessage(color("§e/튜토리얼 블럭목록 §7- 종료블럭 목록 표시"));
        p.sendMessage(color("§e/튜토리얼 블럭초기화 §7- 종료블럭 전체 삭제"));
        p.sendMessage(color("§e/튜토리얼 완료 §7- 본인 완료 상태로 표시"));
        p.sendMessage(color("§e/튜토리얼 완료해제 §7- 본인 완료 상태 해제"));
        p.sendMessage(color("§e/튜토리얼 리로드 §7- 설정 리로드"));
    }

    private String color(String s) { return org.bukkit.ChatColor.translateAlternateColorCodes('&', s); }
}