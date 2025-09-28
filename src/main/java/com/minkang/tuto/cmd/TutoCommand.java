package com.minkang.tuto.cmd;

import com.minkang.tuto.TutoPlugin;
import com.minkang.tuto.model.Trigger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TutoCommand implements CommandExecutor, TabCompleter {
    private final TutoPlugin plugin;
    public TutoCommand(TutoPlugin plugin){ this.plugin=plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("tuto.admin")) { sender.sendMessage(ChatColor.RED + "권한이 없습니다."); return true; }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
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
        if (sub.equals("reload")) {
            plugin.reloadAndApply();
            sender.sendMessage(color(plugin.getConfig().getString("messages.reloaded", "&aReloaded.")));
            return true;
        }

        if (sub.equals("reset") && args.length >= 2) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
            UUID u = op.getUniqueId();
            plugin.data().reset(u);
            String msg = plugin.getConfig().getString("messages.reset", "&e{player} reset.");
            sender.sendMessage(color(msg.replace("{player}", op.getName() == null ? args[1] : op.getName())));
            return true;
        }

        if (sub.equals("complete") && args.length >= 2) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
            UUID u = op.getUniqueId();
            plugin.data().setCompleted(u, true);
            String msg = plugin.getConfig().getString("messages.completed-for", "&a{player} completed.");
            sender.sendMessage(color(msg.replace("{player}", op.getName() == null ? args[1] : op.getName())));
            return true;
        }

        if (sub.equals("status") && args.length >= 2) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
            UUID u = op.getUniqueId();
            boolean done = plugin.data().isCompleted(u);
            sender.sendMessage(ChatColor.YELLOW + (op.getName() == null ? args[1] : op.getName()) + ChatColor.GRAY + " : " + (done ? ChatColor.GREEN + "완료" : ChatColor.RED + "미완료"));
            return true;
        }

        if (sub.equals("settrigger")) {
            if (!(sender instanceof Player)) { sender.sendMessage(ChatColor.RED + "플레이어만 사용 가능"); return true; }
            Player p = (Player) sender;
            if (!plugin.isLocationMode()) { sender.sendMessage(ChatColor.RED + "finish.mode가 location이 아닙니다."); return true; }
            double radius = 0.0;
            if (args.length >= 2) {
                try { radius = Double.parseDouble(args[1]); } catch (Exception ignore) {}
            }
            World w = p.getWorld();
            int x = p.getLocation().getBlockX();
            int y = p.getLocation().getBlockY() - 1; // 발 아래 블럭
            int z = p.getLocation().getBlockZ();
            Trigger t = new Trigger(w, x, y, z, radius, null);
            plugin.getTriggers().add(t);
            plugin.saveTriggersToConfig();
            sender.sendMessage(ChatColor.GREEN + "트리거 추가됨: " + w.getName() + " " + x + " " + y + " " + z + (radius>0 ? " r=" + radius : ""));
            return true;
        }

        if (sub.equals("cleartriggers")) {
            plugin.getTriggers().clear();
            plugin.saveTriggersToConfig();
            sender.sendMessage(ChatColor.YELLOW + "모든 트리거 삭제됨");
            return true;
        }

        if (sub.equals("listtriggers")) {
            List<Trigger> list = plugin.getTriggers();
            sender.sendMessage(ChatColor.AQUA + "등록된 트리거: " + list.size());
            int idx = 1;
            for (Trigger t : list) {
                String w = (t.world==null? "null" : t.world.getName());
                sender.sendMessage(ChatColor.GRAY + (idx++) + ") " + w + " " + t.x + " " + t.y + " " + t.z + (t.radius > 0 ? " r=" + t.radius : ""));
            }
            return true;
        }

        sender.sendMessage(ChatColor.RED + "알 수 없는 하위 명령입니다.");
        return true;
    }

    private String color(String s){ return ChatColor.translateAlternateColorCodes('&', s==null? "" : s); }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (!sender.hasPermission("tuto.admin")) return out;
        if (args.length==1){
            out.add("reload"); out.add("reset"); out.add("complete"); out.add("status");
            out.add("settrigger"); out.add("cleartriggers"); out.add("listtriggers");
        }
        return out;
    }
}
