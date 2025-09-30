package com.minkang.tutorial;

import com.minkang.tutorial.command.TutorialCommand;
import com.minkang.tutorial.listener.FirstJoinListener;
import com.minkang.tutorial.listener.FinishBlockListener;
import com.minkang.tutorial.store.DataStore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TutorialPlugin extends JavaPlugin {

    private final Set<Location> finishBlocks = ConcurrentHashMap.newKeySet();
    private final Set<UUID> awaitingFinishClick = ConcurrentHashMap.newKeySet();
    private DataStore dataStore;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadFinishBlocks();
        dataStore = new DataStore(getDataFolder());

        // listeners
        Bukkit.getPluginManager().registerEvents(new FirstJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new FinishBlockListener(this), this);

        // command
        getCommand("튜토리얼").setExecutor(new TutorialCommand(this));

        getLogger().info("TutorialFinishBlock enabled. Blocks=" + finishBlocks.size());
    }

    @Override
    public void onDisable() {
        saveFinishBlocks();
        if (dataStore != null) dataStore.save();
    }

    // ===== Finish blocks =====
    public void loadFinishBlocks() {
        finishBlocks.clear();
        FileConfiguration cfg = getConfig();
        List<String> raw = cfg.getStringList("finish.blocks");
        for (String s : raw) {
            String[] parts = s.split(",", 5);
            if (parts.length < 4) continue;
            World w = Bukkit.getWorld(parts[0]);
            if (w == null) continue;
            try {
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);
                finishBlocks.add(new Location(w, x, y, z));
            } catch (NumberFormatException ignore) {}
        }
    }

    public void saveFinishBlocks() {
        List<String> out = new ArrayList<>();
        for (Location l : finishBlocks) {
            out.add(l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ());
        }
        getConfig().set("finish.blocks", out);
        saveConfig();
    }

    public boolean addFinishBlock(Location l) {
        Location b = new Location(l.getWorld(), l.getBlockX(), l.getBlockY(), l.getBlockZ());
        boolean added = finishBlocks.add(b);
        if (added) saveFinishBlocks();
        return added;
    }

    public Location removeNearest(Location ref) {
        Location nearest = null;
        double best = Double.MAX_VALUE;
        for (Location l : finishBlocks) {
            if (!Objects.equals(l.getWorld(), ref.getWorld())) continue;
            double d = l.distanceSquared(ref);
            if (d < best) { best = d; nearest = l; }
        }
        if (nearest != null) {
            finishBlocks.remove(nearest);
            saveFinishBlocks();
        }
        return nearest;
    }

    public Set<Location> getFinishBlocks() { return Collections.unmodifiableSet(finishBlocks); }

    // ===== State =====
    public DataStore data() { return dataStore; }

    public Set<UUID> awaiting() { return awaitingFinishClick; }

    // ===== Config getters =====
    public boolean debug() { return getConfig().getBoolean("debug", false); }
    public String prefix() { return getConfig().getString("messages.prefix", ""); }
    public String msg(String path, String def) { return getConfig().getString("messages."+path, def); }

    public boolean firstJoinEnabled() { return getConfig().getBoolean("tutorial.enabled", true); }
    public String firstJoinWarp() { return getConfig().getString("tutorial.on-first-join.warp", ""); }
    public List<String> firstJoinCmds() { return getConfig().getStringList("tutorial.on-first-join.commands"); }

    public boolean finishEnabled() { return getConfig().getBoolean("finish.enabled", true); }
    public String triggerMode() { return getConfig().getString("finish.trigger.mode", "block"); }
    public double triggerRadius() { return getConfig().getDouble("finish.trigger.radius", 1.0D); }
    public boolean triggerRequireSneak() { return getConfig().getBoolean("finish.trigger.require-sneak", false); }
    public List<String> finishActions() { return getConfig().getStringList("finish.actions"); }

    // ===== Utils =====
    public void log(String s) { if (debug()) getLogger().info(s); }

    public static String color(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }

    public String ph(String s, UUID player, Location l) {
        if (s == null) return "";
        String out = s.replace("{player}", Bukkit.getOfflinePlayer(player) != null && Bukkit.getOfflinePlayer(player).getName()!=null ? Bukkit.getOfflinePlayer(player).getName() : "player");
        if (l != null && l.getWorld() != null) {
            out = out.replace("{world}", l.getWorld().getName())
                     .replace("{x}", String.valueOf(l.getBlockX()))
                     .replace("{y}", String.valueOf(l.getBlockY()))
                     .replace("{z}", String.valueOf(l.getBlockZ()));
        }
        return color(out);
    }
}
