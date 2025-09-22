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
            sender.sendMessage(ChatColor.GOLD + "/tuto settrigger [radius]");
            sender.sendMessage(ChatColor.GOLD + "/tuto cleartriggers");
            sender.sendMessage(ChatColor.GOLD + "/tuto listtriggers");
            return true;
        }
        String sub = args[0].toLowerCase();
        if ("reload".equals(sub)){
            plugin.reloadAll();
            sender.sendMessage(color(plugin.getConfig().getString("messages.reloaded","&a리로드 완료")));
            return true;
        } else if ("reset".equals(sub)){
            if (args.length<2){ sender.sendMessage(ChatColor.YELLOW+"사용법: /tuto reset <player>"); return true; }
            OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
            plugin.data().reset(op.getUniqueId());
            String name = (op.getName()==null? args[1]: op.getName());
            sender.sendMessage(color(plugin.getConfig().getString("messages.reset","&e{player} 초기화 완료").replace("{player}", name)));
            return true;
        } else if ("complete".equals(sub)){
            if (args.length<2){ sender.sendMessage(ChatColor.YELLOW+"사용법: /tuto complete <player>"); return true; }
            OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
            plugin.data().setCompleted(op.getUniqueId(), true);
            String name = (op.getName()==null? args[1]: op.getName());
            sender.sendMessage(color(plugin.getConfig().getString("messages.completed-for","&a{player} 완료 처리").replace("{player}", name)));
            return true;
        } else if ("status".equals(sub)){
            if (args.length<2){ sender.sendMessage(ChatColor.YELLOW+"사용법: /tuto status <player>"); return true; }
            OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
            boolean done = plugin.data().isCompleted(op.getUniqueId());
            String name = (op.getName()==null? args[1]: op.getName());
            sender.sendMessage(ChatColor.AQUA + name + ChatColor.GRAY + " : " + (done? ChatColor.GREEN+"완료":"미완료"));
            return true;
        } else if ("settrigger".equals(sub)){
            if (!(sender instanceof org.bukkit.entity.Player)) {
                sender.sendMessage(ChatColor.RED + "플레이어만 사용할 수 있습니다.");
                return true;
            }
            org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
            org.bukkit.Location loc = p.getLocation();
            String worldName = p.getWorld().getName();
            double radius = 0.0;
            if (args.length >= 2) {
                try { radius = Double.parseDouble(args[1]); } catch (Exception ignored){}
            }
            if (!"location".equalsIgnoreCase(plugin.getConfig().getString("finish.mode","block"))) {
                plugin.getConfig().set("finish.mode", "location");
            }
            java.util.List<java.util.Map<?,?>> rawList = plugin.getConfig().getMapList("finish.triggers");
            java.util.List<java.util.Map<String,Object>> list = new java.util.ArrayList<java.util.Map<String,Object>>();
            for (java.util.Map<?,?> m : rawList) {
                java.util.Map<String,Object> copy = new java.util.LinkedHashMap<String,Object>();
                for (java.util.Map.Entry<?,?> e : m.entrySet()) {
                    copy.put(String.valueOf(e.getKey()), e.getValue());
                }
                list.add(copy);
            }
            java.util.Map<String,Object> entry = new java.util.LinkedHashMap<String,Object>();
            entry.put("world", worldName);
            entry.put("x", loc.getBlockX());
            entry.put("y", loc.getBlockY());
            entry.put("z", loc.getBlockZ());
            entry.put("radius", radius);
            list.add(entry);
            plugin.getConfig().set("finish.triggers", list);
            plugin.saveConfig();
            plugin.reloadAll();
            sender.sendMessage(ChatColor.GREEN + String.format("트리거 추가: %s (%d,%d,%d) r=%.2f", worldName, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), radius));
            return true;
        } else if ("cleartriggers".equals(sub)){
            plugin.getConfig().set("finish.triggers", new java.util.ArrayList<java.util.Map<String,Object>>());
            plugin.saveConfig();
            plugin.reloadAll();
            sender.sendMessage(ChatColor.YELLOW + "모든 트리거를 삭제했습니다.");
            return true;
        } else if ("listtriggers".equals(sub)){
            java.util.List<com.minkang.tuto.model.Trigger> list = plugin.triggers();
            sender.sendMessage(ChatColor.AQUA + "등록된 트리거: " + list.size() + "개");
            for (int i=0;i<list.size();i++){
                com.minkang.tuto.model.Trigger t = list.get(i);
                String wn = (t.world==null? "null" : t.world.getName());
                sender.sendMessage(ChatColor.GRAY + String.format(" #%d: %s (%d,%d,%d) r=%.2f", i+1, wn, t.x, t.y, t.z, t.radius));
            }
            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "알 수 없는 하위 명령입니다.");
            return true;
        }
    }
    private String color(String s){ return ChatColor.translateAlternateColorCodes('&', s==null? "": s); }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        if (!sender.hasPermission("tuto.admin")) return out;
        if (args.length==1){ out.add("reload"); out.add("reset"); out.add("complete"); out.add("status"); out.add("settrigger"); out.add("cleartriggers"); out.add("listtriggers"); }
        return out;
    }
}
