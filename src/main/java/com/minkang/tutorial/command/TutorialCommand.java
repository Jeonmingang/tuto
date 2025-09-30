package com.minkang.tutorial.command;

import com.minkang.tutorial.TutorialPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
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
            sender.sendMessage("플레이어만 사용 가능.");
            return true;
        }
        Player p = (Player) sender;

        String sub = args.length == 0 ? "help" : args[0];
        switch (sub.toLowerCase()) {
            case "종료블럭": {
                plugin.awaiting().add(p.getUniqueId());
                p.sendMessage(TutorialPlugin.color(plugin.prefix() + plugin.msg("set-finish-start","&a종료블럭을 &f우클릭&7으로 지정하세요.")));
                return true;
            }
            case "블럭삭제": {
                Location removed = plugin.removeNearest(p.getLocation());
                if (removed == null) {
                    p.sendMessage(TutorialPlugin.color(plugin.prefix() + plugin.msg("no-finish","&7종료블럭이 아직 설정되지 않았습니다.")));
                } else {
                    p.sendMessage(TutorialPlugin.color(plugin.prefix() + plugin.msg("removed-nearest","&c가장 가까운 종료블럭을 삭제했습니다: &f%world% %x%,%y%,%z%")
                            .replace("%world%", removed.getWorld().getName())
                            .replace("%x%", String.valueOf(removed.getBlockX()))
                            .replace("%y%", String.valueOf(removed.getBlockY()))
                            .replace("%z%", String.valueOf(removed.getBlockZ()))));
                }
                return true;
            }
            case "블럭목록": {
                p.sendMessage(TutorialPlugin.color(plugin.msg("list-header","&e[종료블럭 목록]")));
                for (Location l : plugin.getFinishBlocks()) {
                    p.sendMessage(TutorialPlugin.color(plugin.msg("list-entry","&7- &f%world% %x%,%y%,%z%")
                            .replace("%world%", l.getWorld().getName())
                            .replace("%x%", String.valueOf(l.getBlockX()))
                            .replace("%y%", String.valueOf(l.getBlockY()))
                            .replace("%z%", String.valueOf(l.getBlockZ()))));
                }
                return true;
            }
            case "블럭초기화": {
                for (Location l : plugin.getFinishBlocks().toArray(new Location[0])) {
                    plugin.removeNearest(l);
                }
                p.sendMessage(TutorialPlugin.color(plugin.prefix() + plugin.msg("cleared","&c모든 종료블럭 삭제.")));
                return true;
            }
            case "완료": {
                plugin.data().setFinished(p.getUniqueId());
                plugin.data().save();
                p.sendMessage("완료 상태 저장됨.");
                return true;
            }
            case "완료해제": {
                plugin.data().unsetFinished(p.getUniqueId());
                plugin.data().save();
                p.sendMessage("완료 상태 해제됨.");
                return true;
            }
            case "테스트": {
                Bukkit.getPluginManager().callEvent(new org.bukkit.event.player.PlayerMoveEvent(p, p.getLocation(), p.getLocation().add(0, 0.1, 0)));
                p.sendMessage("트리거 테스트 신호 전송.");
                return true;
            }
            case "리로드":
            case "reload": {
                plugin.reloadConfig();
                plugin.loadFinishBlocks();
                plugin.data().reload();
                p.sendMessage("리로드 완료.");
                return true;
            }
            default: {
                p.sendMessage(TutorialPlugin.color("&e/튜토리얼 종료블럭 &7- 우클릭으로 종료블럭 지정"));
                p.sendMessage(TutorialPlugin.color("&e/튜토리얼 블럭삭제 &7- 가장 가까운 종료블럭 삭제"));
                p.sendMessage(TutorialPlugin.color("&e/튜토리얼 블럭목록 &7- 모든 종료블럭 보기"));
                p.sendMessage(TutorialPlugin.color("&e/튜토리얼 블럭초기화 &7- 종료블럭 전체 삭제"));
                p.sendMessage(TutorialPlugin.color("&e/튜토리얼 완료 &7- 완료 상태 수동 저장"));
                p.sendMessage(TutorialPlugin.color("&e/튜토리얼 완료해제 &7- 완료 상태 해제"));
                p.sendMessage(TutorialPlugin.color("&e/튜토리얼 테스트 &7- 트리거 테스트"));
                p.sendMessage(TutorialPlugin.color("&e/튜토리얼 리로드 &7- 설정/데이터 리로드"));
                return true;
            }
        }
    }
}
