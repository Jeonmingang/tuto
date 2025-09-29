package com.minkang.tuto.cmd;

import com.minkang.tuto.TutoPlugin;
import com.minkang.tuto.model.Trigger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TutoCommand implements CommandExecutor {

    private final TutoPlugin plugin;

    public TutoCommand(TutoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Player only.");
            return true;
        }
        Player p = (Player) sender;
        if (args.length == 0) {
            p.sendMessage("§e/tuto settrigger [radius] §7- 현재 위치로 트리거 추가");
            p.sendMessage("§e/tuto listtriggers §7- 트리거 목록");
            p.sendMessage("§e/tuto cleartriggers §7- 트리거 초기화");
            p.sendMessage("§e/tuto reload §7- 설정/트리거 리로드");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "settrigger": {
                double radius = 0.0;
                if (args.length >= 2) try { radius = Double.parseDouble(args[1]); } catch (Exception ignored) {}
                Location l = p.getLocation();
                List<String> cmds = new ArrayList<>(plugin.getFinishCommands());
                Trigger t = new Trigger(l.getWorld().getName(), l.getWorld().getUID(), l.getX(), l.getY(), l.getZ(), radius, cmds);
                plugin.addTrigger(t);
                p.sendMessage("§a트리거 추가: §f" + l.getWorld().getName() + " " + l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ() + " r=" + radius);
                return true;
            }
            case "listtriggers": {
                int i = 0;
                for (Trigger t : plugin.getTriggers()) {
                    p.sendMessage(String.format("§7[%d] §f%s §7%d,%d,%d §7r=%.1f", ++i, t.worldName, (int)t.x,(int)t.y,(int)t.z, t.radius));
                }
                if (i == 0) p.sendMessage("§7(등록된 트리거가 없습니다)");
                return true;
            }
            case "cleartriggers": {
                plugin.clearTriggers();
                p.sendMessage("§c모든 트리거 삭제됨.");
                return true;
            }
            case "reload": {
                plugin.reloadAll();
                p.sendMessage("§a리로드 완료.");
                return true;
            }
            default:
                p.sendMessage("Unknown subcommand.");
                return true;
        }
    }
}