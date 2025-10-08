package com.minkang.tutorial;

import com.minkang.tutorial.cmd.TutorialCommand;
import com.minkang.tutorial.listeners.FirstJoinListener;
import com.minkang.tutorial.listeners.MoveListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class TutorialWarpPlugin extends JavaPlugin {

    private final Set<BlockPoint> triggerBlocks = new HashSet<>();
    private boolean debug;
    private String prefix;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadAll();
        // listeners
        Bukkit.getPluginManager().registerEvents(new FirstJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MoveListener(this), this);
        // command
        Objects.requireNonNull(getCommand("tutorial")).setExecutor(new TutorialCommand(this));
        log("&aEnabled &7(v" + getDescription().getVersion() + ")");
    }

    @Override
    public void onDisable() {
        saveConfig();
        log("&cDisabled");
    }

    public void reloadAll() {
        reloadConfig();
        FileConfiguration c = getConfig();
        this.debug = c.getBoolean("debug", false);
        this.prefix = color(c.getString("messages.prefix", "&6[Tutorial]&r "));

        // load trigger blocks
        this.triggerBlocks.clear();
        List<String> raw = c.getStringList("trigger.blocks");
        for (String s : raw) {
            BlockPoint bp = BlockPoint.parse(s);
            if (bp != null) triggerBlocks.add(bp);
        }
        if (debug) {
            log("&7Loaded blocks: " + triggerBlocks.stream().map(BlockPoint::toString).collect(Collectors.joining(", ")));
        }
    }

    public boolean isDebug() { return debug; }
    public String prefix() { return prefix != null ? prefix : ""; }
    public Set<BlockPoint> getTriggerBlocks() { return triggerBlocks; }

    public String color(String s) {
        return s == null ? "" : s.replace("&", "§");
    }
    public void msg(Player p, String key, Map<String,String> vars) {
        String raw = getConfig().getString(key, "");
        if (raw == null || raw.isEmpty()) return;
        for (Map.Entry<String,String> e : vars.entrySet()) {
            raw = raw.replace(e.getKey(), e.getValue());
        }
        p.sendMessage(prefix() + color(raw));
    }
    public void log(String s) { getServer().getConsoleSender().sendMessage(color("&7[Tutorial] " + s)); }

    // Helpers
    public static class BlockPoint {
        public final String world;
        public final int x,y,z;
        public BlockPoint(String world, int x, int y, int z) {
            this.world = world; this.x = x; this.y = y; this.z = z;
        }
        public static BlockPoint of(org.bukkit.Location loc) {
            return new BlockPoint(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        }
        public static BlockPoint parse(String s) {
            try {
                String[] t = s.split(",");
                if (t.length != 4) return null;
                return new BlockPoint(t[0], Integer.parseInt(t[1]), Integer.parseInt(t[2]), Integer.parseInt(t[3]));
            } catch (Exception e) { return null; }
        }
        
        public boolean matches(org.bukkit.Location loc) {
            if (loc == null || loc.getWorld() == null) return false;
            if (loc.getWorld().getName().equalsIgnoreCase(world)) {
                return loc.getBlockX() == x && loc.getBlockY() == y && loc.getBlockZ() == z;
            }
            return false;
        }
    
        public org.bukkit.Location toLocation() {
            org.bukkit.World w = Bukkit.getWorld(world);
            return (w == null) ? null : new org.bukkit.Location(w, x + 0.5, y, z + 0.5);
        }
        @Override public String toString() { return world + "," + x + "," + y + "," + z; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BlockPoint)) return false;
            BlockPoint bp = (BlockPoint)o;
            return x==bp.x && y==bp.y && z==bp.z && java.util.Objects.equals(world, bp.world);
        }
        @Override public int hashCode() { return java.util.Objects.hash(world,x,y,z); }
    }
}
