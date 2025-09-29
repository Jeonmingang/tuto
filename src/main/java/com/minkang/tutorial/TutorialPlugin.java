package com.minkang.tutorial;

import com.minkang.tutorial.command.TutorialCommand;
import com.minkang.tutorial.listener.FirstJoinListener;
import com.minkang.tutorial.listener.FinishBlockListener;
import com.minkang.tutorial.store.DataStore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TutorialPlugin extends JavaPlugin {

    private final List<Location> finishBlocks = new ArrayList<>();
    private final Set<UUID> selecting = ConcurrentHashMap.newKeySet();
    private DataStore dataStore;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.dataStore = new DataStore(getDataFolder());
        this.dataStore.load();
        loadFinishBlocks();

        Bukkit.getPluginManager().registerEvents(new FirstJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new FinishBlockListener(this), this);

        if (getCommand("튜토리얼") != null) {
            getCommand("튜토리얼").setExecutor(new TutorialCommand(this));
        }

        log("&aEnabled. Blocks=" + finishBlocks.size());
    }

    @Override
    public void onDisable() {
        saveFinishBlocks();
        if (this.dataStore != null) this.dataStore.save();
    }

    // --- Config getters ---
    public boolean debug() { return getConfig().getBoolean("debug", false); }
    public boolean tutorialEnabled() { return getConfig().getBoolean("tutorial.enabled", true); }
    public String firstWarp() { return getConfig().getString("tutorial.on-first-join.warp", ""); }
    public List<String> firstCommands() { return getConfig().getStringList("tutorial.on-first-join.commands"); }

    public boolean finishEnabled() { return getConfig().getBoolean("finish.enabled", true); }
    public String triggerMode() { return getConfig().getString("finish.trigger.mode", "block").toLowerCase(Locale.ROOT); }
    public double triggerRadius() { return getConfig().getDouble("finish.trigger.radius", 1.0D); }
    public boolean requireSneak() { return getConfig().getBoolean("finish.trigger.require-sneak", false); }
    public boolean oneTime() { return getConfig().getBoolean("finish.trigger.one-time-per-player", true); }
    public int cooldownSeconds() { return getConfig().getInt("finish.trigger.cooldown-seconds", 2); }
    public String bypassPerm() { return getConfig().getString("finish.trigger.bypass-permission", "tutorial.bypass"); }

    public List<String> actions() { return getConfig().getStringList("finish.actions"); }

    public String msg(String k, String def) {
        String s = getConfig().getString("messages."+k, def);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s);
    }
    public String prefix() { return getConfig().getString("messages.prefix", "&6[Tutorial]&r "); }

    // --- Finish blocks storage ---
    public List<Location> getFinishBlocks() { return Collections.unmodifiableList(finishBlocks); }

    public void addFinishBlock(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        Location l = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        this.finishBlocks.add(l);
        saveFinishBlocks();
    }

    public Location removeNearest(Location from) {
        if (finishBlocks.isEmpty()) return null;
        Location best = null;
        double bestDist = Double.MAX_VALUE;
        for (Location l : finishBlocks) {
            if (l.getWorld() == null || !l.getWorld().equals(from.getWorld())) continue;
            double d = l.distanceSquared(from);
            if (d < bestDist) { bestDist = d; best = l; }
        }
        if (best != null) { finishBlocks.remove(best); saveFinishBlocks(); }
        return best;
    }

    public void clearFinishBlocks() {
        this.finishBlocks.clear();
        saveFinishBlocks();
    }

    public void loadFinishBlocks() {
        this.finishBlocks.clear();
        List<?> list = getConfig().getList("finish.blocks");
        if (list == null) return;
        for (Object o : list) {
            if (!(o instanceof Map)) continue;
            Map<?,?> m = (Map<?,?>) o;
            String worldName = String.valueOf(m.get("world"));
            String uid = String.valueOf(m.get("world-uid"));
            World w = null;
            try { if (uid != null && !"".equals(uid) && !"null".equalsIgnoreCase(uid)) w = Bukkit.getWorld(UUID.fromString(uid)); } catch (Exception ignored) {}
            if (w == null) w = Bukkit.getWorld(worldName);
            if (w == null) continue;
            int x = toInt(m.get("x")), y = toInt(m.get("y")), z = toInt(m.get("z"));
            this.finishBlocks.add(new Location(w, x, y, z));
        }
    }

    public void saveFinishBlocks() {
        List<Map<String,Object>> out = new ArrayList<>();
        for (Location l : finishBlocks) {
            if (l.getWorld() == null) continue;
            Map<String,Object> m = new HashMap<>();
            m.put("world", l.getWorld().getName());
            m.put("world-uid", l.getWorld().getUID().toString());
            m.put("x", l.getBlockX());
            m.put("y", l.getBlockY());
            m.put("z", l.getBlockZ());
            out.add(m);
        }
        getConfig().set("finish.blocks", out);
        saveConfig();
    }

    // --- Selection state ---
    public void beginSelecting(UUID u) { selecting.add(u); }
    public boolean isSelecting(UUID u) { return selecting.contains(u); }
    public void endSelecting(UUID u) { selecting.remove(u); }

    // --- Data store ---
    public DataStore store() { return dataStore; }

    // --- Reload ---
    public void reloadAll() {
        reloadConfig();
        loadFinishBlocks();
    }

    // --- Utils ---
    private int toInt(Object o) {
        try { return (o instanceof Number) ? ((Number)o).intValue() : Integer.parseInt(String.valueOf(o)); }
        catch (Exception e) { return 0; }
    }

    public void log(String s) { if (debug()) getLogger().info(s); }

    public String ph(String s, UUID player, Location l) {
        if (s == null) return "";
        String out = s.replace("{player}", Bukkit.getOfflinePlayer(player).getName() == null ? "player" : Bukkit.getOfflinePlayer(player).getName());
        if (l != null && l.getWorld() != null) {
            out = out.replace("{world}", l.getWorld().getName())
                     .replace("{x}", String.valueOf(l.getBlockX()))
                     .replace("{y}", String.valueOf(l.getBlockY()))
                     .replace("{z}", String.valueOf(l.getBlockZ()));
        }
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', out);
    }
}