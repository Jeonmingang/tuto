package com.minkang.tuto.cmd;

import com.minkang.tuto.TutoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import java.util.*;

public class TutoCommand implements CommandExecutor, TabCompleter {
    private final TutoPlugin plugin;
    public TutoCommand(TutoPlugin plugin){ this.plugin=plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("tuto.admin")){ sender.sendMessage(ChatColor.RED+"권한이 없습니다."); return true; }
        if (args.length==0){
            sender.sendMessage(ChatColor.GOLD + "/tuto reload");
            sender.sendMessage(ChatColor.GOLD + "/tuto reset <player>");
            sender.sendMessage(ChatColor.GOLD + "/tuto complete <player>");
            sender.sendMessage(ChatColor.GOLD + "/tuto status <player>");
            return true;
        }
        switch (args[0].toLowerCase()){
            case "reload" -> { plugin.reloadAll(); sender.sendMessage(color(plugin.getConfig().getString("messages.reloaded","&a리로드 완료"))); }
            case "reset" -> {
                if (args.length<2){ sender.sendMessage(ChatColor.YELLOW+"사용법: /tuto reset <player>"); return true; }
                OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
                plugin.data().reset(op.getUniqueId());
                sender.sendMessage(color(plugin.getConfig().getString("messages.reset","&e{player} 초기화 완료").replace("{player}", op.getName()==null?args[1]:op.getName())));
            }
            case "complete" -> {
                if (args.length<2){ sender.sendMessage(ChatColor.YELLOW+"사용법: /tuto complete <player>"); return true; }
                OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
                plugin.data().setCompleted(op.getUniqueId(), true);
                sender.sendMessage(color(plugin.getConfig().getString("messages.completed-for","&a{player} 완료 처리").replace("{player}", op.getName()==null?args[1]:op.getName())));
            }
            case "status" -> {
                if (args.length<2){ sender.sendMessage(ChatColor.YELLOW+"사용법: /tuto status <player>"); return true; }
                OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
                boolean done = plugin.data().isCompleted(op.getUniqueId());
                sender.sendMessage(ChatColor.AQUA + (op.getName()==null?args[1]:op.getName()) + ChatColor.GRAY + " : " + (done? ChatColor.GREEN+"완료":"미완료"));
            }
            default -> sender.sendMessage(ChatColor.RED + "알 수 없는 하위 명령입니다.");
        }
        return true;
    }
    private String color(String s){ return ChatColor.translateAlternateColorCodes('&', s==null? "": s); }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (!sender.hasPermission("tuto.admin")) return out;
        if (args.length==1){ out.add("reload"); out.add("reset"); out.add("complete"); out.add("status"); }
        return out;
    }
}
