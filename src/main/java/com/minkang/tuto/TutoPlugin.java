package com.minkang.tuto;

import com.minkang.tuto.cmd.TutoCommand;
import com.minkang.tuto.listener.FirstJoinListener;
import com.minkang.tuto.listener.FinishTriggerListener;
import com.minkang.tuto.model.Trigger;
import com.minkang.tuto.store.DataStore;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TutoPlugin extends JavaPlugin {

    private File triggersFile;
    private YamlConfiguration triggersCfg;
    private DataStore dataStore;
    private final List<Trigger> triggers = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureDataFiles();

        // Load persistent data
        this.dataStore = new DataStore(new File(getDataFolder(), "data.yml"));
        this.dataStore.load();
        loadTriggers();

        // Register listeners and command
        Bukkit.getPluginManager().registerEvents(new FirstJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new FinishTriggerListener(this), this);
        getCommand("tuto").setExecutor(new TutoCommand(this));

        // 1-tick delayed world-safe init
        Bukkit.getScheduler().runTask(this, () -> getLogger().info("[Tuto] Triggers loaded: " + triggers.size()));
    }

    @Override
    public void onDisable() {
        saveTriggers();
        if (dataStore != null) dataStore.save();
    }

    public FileConfiguration getTriggersConfig() { return triggersCfg; }

    public List<Trigger> getTriggers() { return triggers; }

    public DataStore getDataStore() { return dataStore; }

    public void reloadAll() {
        reloadConfig();
        loadTriggers();
        if (dataStore != null) dataStore.save();
    }

    private void ensureDataFiles() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        this.triggersFile = new File(getDataFolder(), "triggers.yml");
        if (!this.triggersFile.exists()) {
            try {
                this.triggersFile.createNewFile();
                YamlConfiguration y = new YamlConfiguration();
                y.set("triggers", new ArrayList<>());
                y.save(this.triggersFile);
            } catch (IOException e) {
                getLogger().warning("Failed to create triggers.yml: " + e.getMessage());
            }
        }
        this.triggersCfg = YamlConfiguration.loadConfiguration(this.triggersFile);
    }

    public void addTrigger(Trigger t) {
        this.triggers.add(t);
        saveTriggers();
    }

    public void clearTriggers() {
        this.triggers.clear();
        saveTriggers();
    }

    public void loadTriggers() {
        ensureDataFiles();
        this.triggers.clear();
        List<?> list = this.triggersCfg.getList("triggers");
        if (list != null) {
            for (Object o : list) {
                if (o instanceof java.util.Map) {
                    java.util.Map<?,?> m = (java.util.Map<?,?>) o;
                    String worldName = String.valueOf(m.get("world"));
                    World w = Bukkit.getWorld(worldName);
                    if (w == null) continue;
                    double x = toD(m.get("x")), y = toD(m.get("y")), z = toD(m.get("z"));
                    double radius = toD(m.getOrDefault("radius", 0.0));
                    List<String> cmds = new ArrayList<>();
                    Object c = m.get("commands");
                    if (c instanceof List) {
                        for (Object e : (List<?>) c) cmds.add(String.valueOf(e));
                    }
                    Trigger t = new Trigger(w.getName(), w.getUID(), x, y, z, radius, cmds);
                    this.triggers.add(t);
                }
            }
        }
    }

    public void saveTriggers() {
        ensureDataFiles();
        List<java.util.Map<String,Object>> list = new ArrayList<>();
        for (Trigger t : this.triggers) {
            java.util.Map<String,Object> m = new java.util.HashMap<>();
            m.put("world", t.worldName);
            m.put("world-uid", t.worldUid.toString());
            m.put("x", t.x);
            m.put("y", t.y);
            m.put("z", t.z);
            if (t.radius > 0.0) m.put("radius", t.radius);
            if (t.commands != null && !t.commands.isEmpty()) m.put("commands", t.commands);
            list.add(m);
        }
        this.triggersCfg.set("triggers", list);
        try {
            this.triggersCfg.save(this.triggersFile);
        } catch (IOException e) {
            getLogger().warning("Failed to save triggers.yml: " + e.getMessage());
        }
    }

    private double toD(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return 0.0; }
    }

    // Convenience getters for config
    public boolean isFinishEnabled() { return getConfig().getBoolean("finish.enabled", true); }
    public String getFinishMode() { return getConfig().getString("finish.mode", "location").toLowerCase(); }
    public List<String> getFinishCommands() { return getConfig().getStringList("finish.commands"); }
    public boolean isOneTime() { return getConfig().getBoolean("finish.one-time-per-player", true); }
    public int getCooldownSeconds() { return getConfig().getInt("finish.cooldown-seconds", 0); }
    public int getExecuteDelayTicks() { return getConfig().getInt("finish.execute-delay-ticks", 0); }
    public List<String> getBlockTypes() { return getConfig().getStringList("finish.block-types"); }
    public String prefix() { return getConfig().getString("messages.prefix", "&6[Tuto]&r "); }
    public String msgStart() { return getConfig().getString("messages.start", "&e튜토리얼 지역입니다."); }
    public String msgFinish() { return getConfig().getString("messages.finish", "&a튜토리얼 완료! 보상을 지급합니다."); }
}