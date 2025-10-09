package com.minkang.tutorial;

import com.minkang.tutorial.cmd.TutorialCommand;
import com.minkang.tutorial.listeners.FirstJoinListener;
import com.minkang.tutorial.listeners.MoveListener;
import com.minkang.tutorial.store.DataStore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class TutorialWarpPlugin extends JavaPlugin {

    private final Set<BlockPoint> blocks = new HashSet<>();
    private boolean debug;
    private double radius;
    private boolean requireSneak;
    private boolean oncePerPlayer;
    private DataStore store;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        store = new DataStore(getDataFolder());
        reloadLocal();

        getServer().getPluginManager().registerEvents(new MoveListener(this), this);
        getServer().getPluginManager().registerEvents(new FirstJoinListener(this), this);
        getCommand("tutorial").setExecutor(new TutorialCommand(this));
        info("Enabled v" + getDescription().getVersion() + " with " + blocks.size() + " trigger blocks.");
    }

    @Override
    public void onDisable() {
        if (store != null) store.save();
    }

    public void reloadLocal() {
        reloadConfig();
        FileConfiguration cfg = getConfig();
        debug = cfg.getBoolean("debug", false);
        radius = cfg.getDouble("trigger.radius", 1.2);
        requireSneak = cfg.getBoolean("trigger.require-sneak", false);
        oncePerPlayer = cfg.getBoolean("tutorial.once-per-player", true);
        blocks.clear();
        List<String> list = cfg.getStringList("trigger.blocks");
        for (String s : list) {
            BlockPoint bp = BlockPoint.parse(s);
            if (bp != null) blocks.add(bp);
        }
    }

    public boolean isOncePerPlayer() { return oncePerPlayer; }
    public boolean isCompleted(Player p) { return store != null && store.isCompleted(p.getUniqueId()); }
    public void setCompleted(Player p, boolean value) { if (store != null) store.setCompleted(p.getUniqueId(), value); }
    public void resetAll() { if (store != null) store.clearAll(); }

    public void addBlock(BlockPoint bp) {
        blocks.add(bp);
        persistBlocks();
    }
    public boolean removeNearest(Location from) {
        if (blocks.isEmpty()) return false;
        BlockPoint best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPoint bp : blocks) {
            Location l = bp.toLocation();
            if (l == null || !Objects.equals(l.getWorld(), from.getWorld())) continue;
            double d = l.distanceSquared(from);
            if (d < bestD) { bestD = d; best = bp; }
        }
        if (best != null) {
            blocks.remove(best);
            persistBlocks();
            return true;
        }
        return false;
    }

    public Set<BlockPoint> getBlocks() { return Collections.unmodifiableSet(blocks); }
    public boolean isDebug() { return debug; }
    public double getRadius() { return radius; }
    public boolean isRequireSneak() { return requireSneak; }

    public void runOnTriggerActions(Player p) {
        List<String> actions = getConfig().getStringList("on-trigger.actions");
        if (actions == null) return;
        for (String raw : actions) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            String replaced = replace(line, p);
            try {
                if (line.regionMatches(true, 0, "title:", 0, 6)) {
                    String msg = color(line.substring(6).trim());
                    p.sendTitle(msg, "", 10, 50, 10);
                } else if (line.regionMatches(true, 0, "subtitle:", 0, 9)) {
                    String msg = color(line.substring(9).trim());
                    p.sendTitle("", msg, 10, 50, 10);
                } else if (line.regionMatches(true, 0, "sound:", 0, 6)) {
                    String s = line.substring(6).trim().toUpperCase(Locale.ROOT);
                    try {
                        org.bukkit.Sound sound = org.bukkit.Sound.valueOf(s);
                        p.playSound(p.getLocation(), sound, 1f, 1f);
                    } catch (IllegalArgumentException ex) {
                        warn("Unknown sound: " + s);
                    }
                } else if (line.regionMatches(true, 0, "console:", 0, 8)) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replaced.substring(8).trim());
                } else if (line.regionMatches(true, 0, "player:", 0, 7)) {
                    p.performCommand(replaced.substring(7).trim());
                } else if (line.regionMatches(true, 0, "op:", 0, 3)) {
                    boolean was = p.isOp();
                    try {
                        p.setOp(true);
                        p.performCommand(replaced.substring(3).trim());
                    } finally {
                        p.setOp(was);
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        // Mark completion if enabled
        if (isOncePerPlayer()) {
            setCompleted(p, true);
            String msg = getConfig().getString("messages.completed-now", "&a튜토리얼을 완료했습니다!");
            if (msg != null && !msg.isEmpty()) p.sendMessage(prefix() + color(msg));
        }
    }

    public boolean isTriggered(Location to, boolean sneaking) {
        if (requireSneak && !sneaking) return false;
        String mode = getConfig().getString("trigger.mode", "block").toLowerCase(Locale.ROOT);
        if ("radius".equals(mode)) {
            for (BlockPoint bp : blocks) {
                Location l = bp.toLocation();
                if (l == null || !Objects.equals(l.getWorld(), to.getWorld())) continue;
                if (l.distanceSquared(to) <= (radius*radius)) return true;
            }
            return false;
        }
        // default block mode: match block under player's feet
        int bx = to.getBlockX(), by = to.getBlockY(), bz = to.getBlockZ();
        // In Minecraft, the player's location block coords are usually the AIR block they occupy;
        // the ground block under feet is Y-1. We check both for robustness.
        int fx = bx, fy = by - 1, fz = bz;
        String wname = to.getWorld()!=null ? to.getWorld().getName() : "world";
        for (BlockPoint bp : blocks) {
            if (Objects.equals(bp.world, wname)) {
                if ((bp.x == bx && bp.y == by && bp.z == bz) ||
                    (bp.x == fx && bp.y == fy && bp.z == fz)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void persistBlocks() {
        List<String> serialized = blocks.stream().sorted(Comparator
                .comparing((BlockPoint b) -> b.world)
                .thenComparingInt(b -> b.x).thenComparingInt(b -> b.y).thenComparingInt(b -> b.z))
                .map(BlockPoint::toString).collect(Collectors.toList());
        getConfig().set("trigger.blocks", serialized);
        saveConfig(); // persist immediately
    }

    public String prefix() { return color(getConfig().getString("messages.prefix", "&6[Tutorial]&r ")); }

    private String replace(String s, Player p) {
        Location l = p.getLocation();
        return s.replace("{player}", p.getName())
                .replace("{world}", l.getWorld()!=null?l.getWorld().getName():"world")
                .replace("{x}", Integer.toString(l.getBlockX()))
                .replace("{y}", Integer.toString(l.getBlockY()))
                .replace("{z}", Integer.toString(l.getBlockZ()));
    }

    private void info(String s) { getLogger().info(s); }
    public void warn(String s) { getLogger().warning(s); }
    public String color(String s) { return s.replace("&", "§"); }

    // ---------- Model ----------
    public static class BlockPoint {
        public final String world;
        public final int x, y, z;
        public BlockPoint(String world, int x, int y, int z) { this.world = world; this.x=x; this.y=y; this.z=z; }
        public static BlockPoint of(Location l) {
            World w = l.getWorld();
            return new BlockPoint(w!=null?w.getName():"world", l.getBlockX(), l.getBlockY(), l.getBlockZ());
        }
        public static BlockPoint parse(String s) {
            if (s==null) return null;
            String[] sp = s.split(",");
            if (sp.length != 4) return null;
            try {
                return new BlockPoint(sp[0], Integer.parseInt(sp[1]), Integer.parseInt(sp[2]), Integer.parseInt(sp[3]));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        public Location toLocation() {
            World w = Bukkit.getWorld(world);
            if (w == null) return null;
            return new Location(w, x+0.5, y, z+0.5);
        }
        @Override public String toString() { return world + "," + x + "," + y + "," + z; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BlockPoint)) return false;
            BlockPoint bp = (BlockPoint) o;
            return x==bp.x && y==bp.y && z==bp.z && Objects.equals(world, bp.world);
        }
        @Override public int hashCode() { return Objects.hash(world,x,y,z); }
    }
}
