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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TutoPlugin extends JavaPlugin {

    private DataStore data;
    private final List<Trigger> triggers = new ArrayList<Trigger>();
    private FinishTriggerListener finishListener;

    @Override
    public void onEnable() {
        // Ensure config exists
        saveDefaultConfig();
        // Copy any new defaults into existing config (so "config update" is applied)
        applyDefaultsIntoConfig(getConfig());
        saveConfig();

        this.data = new DataStore(this);
        this.data.load();

        // Load triggers from config
        loadTriggersFromConfig();

        // Listeners & command
        this.finishListener = new FinishTriggerListener(this);
        Bukkit.getPluginManager().registerEvents(new FirstJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(this.finishListener, this);
        // Safety: re-load triggers one tick later in case worlds/plugins finish loading after us (Arclight/CatServer quirk)
        Bukkit.getScheduler().runTask(this, new Runnable() {
            @Override public void run() {
                loadTriggersFromConfig();
                getLogger().info("[Tuto] Triggers loaded: " + getTriggers().size());
            }
        });


        if (getCommand("tuto") != null) {
            TutoCommand cmd = new TutoCommand(this);
            getCommand("tuto").setExecutor(cmd);
            getCommand("tuto").setTabCompleter(cmd);
        }
    }

    @Override
    public void onDisable() {
        // Persist triggers + datastore
        saveTriggersToConfig();
        if (this.data != null) this.data.save();
    }

    /* ---------------- Config Helpers ---------------- */

    public void reloadAndApply() {
        // Persist current triggers before reloading
        saveTriggersToConfig();
        // Reload and merge defaults
        reloadConfig();
        applyDefaultsIntoConfig(getConfig());
        saveConfig();
        // Reload data + triggers
        this.data.load();
        loadTriggersFromConfig();
    }

    private void applyDefaultsIntoConfig(FileConfiguration cfg) {
        try {
            InputStream is = getResource("config.yml");
            if (is != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(is, StandardCharsets.UTF_8));
                cfg.setDefaults(defaults);
                cfg.options().copyDefaults(true);
            }
        } catch (Throwable t) {
            getLogger().warning("[Tuto] Failed to apply defaults: " + t.getMessage());
        }
    }

    /* ---------------- Feature Toggles & Reads ---------------- */

    public boolean isTutorialEnabled() { return getConfig().getBoolean("tutorial.enabled", true); }
    public String tutorialWarp() { return getConfig().getString("tutorial.warp","tutorial"); }

    public boolean isFinishEnabled() { return getConfig().getBoolean("finish.enabled", true); }

    public boolean isLocationMode() {
        String mode = getConfig().getString("finish.mode","block");
        return "location".equalsIgnoreCase(mode);
    }

    public List<Trigger> getTriggers() { return this.triggers; }
    public DataStore data() { return this.data; }
    public FinishTriggerListener finishListener() { return this.finishListener; }

    /* ---------------- Trigger Load/Save ---------------- */

    @SuppressWarnings("unchecked")
    public 
void loadTriggersFromConfig() {
        // Prefer external triggers.yml for persistence across restarts/reloads.
        // Backward compatible: if triggers.yml absent, migrate from config.yaml (finish.triggers).
        this.triggers.clear();
        if (!isLocationMode()) return;
        try {
            java.io.File dataFolder = getDataFolder();
            if (!dataFolder.exists()) dataFolder.mkdirs();
            java.io.File tf = new java.io.File(dataFolder, "triggers.yml");
            org.bukkit.configuration.file.FileConfiguration tcfg = null;
            java.util.List<java.util.Map<String,Object>> list = null;
            if (tf.exists()) {
                tcfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(tf);
                java.util.List<?> raw = tcfg.getList("triggers");
                if (raw != null) {
                    list = new java.util.ArrayList<>();
                    for (Object o : raw) {
                        if (o instanceof java.util.Map) {
                            @SuppressWarnings("unchecked")
                            java.util.Map<String,Object> m = new java.util.LinkedHashMap<>((java.util.Map<String,Object>) o);
                            list.add(m);
                        }
                    }
                }
            }
            // migrate from config if file empty
            if (list == null || list.isEmpty()) {
                @SuppressWarnings("unchecked")
                java.util.List<java.util.Map<String,Object>> cfgList = (java.util.List<java.util.Map<String,Object>>) getConfig().getList("finish.triggers");
                list = cfgList;
            }
            if (list == null) return;

            for (java.util.Map<String,Object> m : list) {
                try {
                    org.bukkit.World w = null;
                    if (m.containsKey("world-uid")) {
                        try {
                            java.util.UUID uid = java.util.UUID.fromString(String.valueOf(m.get("world-uid")));
                            w = org.bukkit.Bukkit.getWorld(uid);
                        } catch (Exception ignored) {}
                    }
                    if (w == null && m.containsKey("world")) {
                        w = org.bukkit.Bukkit.getWorld(String.valueOf(m.get("world")));
                    }
                    int x = Integer.parseInt(String.valueOf(m.get("x")));
                    int y = Integer.parseInt(String.valueOf(m.get("y")));
                    int z = Integer.parseInt(String.valueOf(m.get("z")));
                    double radius = 0.0;
                    if (m.containsKey("radius")) {
                        try { radius = Double.parseDouble(String.valueOf(m.get("radius"))); } catch (Exception ignored) {}
                    }
                    java.util.List<String> commands = null;
                    Object cmds = m.get("commands");
                    if (cmds instanceof java.util.List) {
                        commands = new java.util.ArrayList<>();
                        for (Object o : ((java.util.List<?>)cmds)) {
                            if (o != null) commands.add(String.valueOf(o));
                        }
                    }
                    this.triggers.add(new com.minkang.tuto.model.Trigger(w, x, y, z, radius, commands));
                } catch (Exception ignore) {
                    // skip malformed entry
                }
            }
            // if we loaded from config (migration path), write out to triggers.yml to persist
            if (tf != null && (!tf.exists() || (tcfg != null && (tcfg.getList("triggers")==null || tcfg.getList("triggers").isEmpty())))) {
                saveTriggersToConfig(); // our save writes both file + mirrors to config
            }
        } catch (Throwable t) {
            getLogger().warning("[Tuto] Failed to load triggers: " + t.getMessage());
        }
}
 catch (Exception ignored) {}
                }
                if (w == null && m.containsKey("world")) {
                    w = Bukkit.getWorld(String.valueOf(m.get("world")));
                }
                int x = Integer.parseInt(String.valueOf(m.get("x")));
                int y = Integer.parseInt(String.valueOf(m.get("y")));
                int z = Integer.parseInt(String.valueOf(m.get("z")));
                double radius = 0.0;
                if (m.containsKey("radius")) {
                    try { radius = Double.parseDouble(String.valueOf(m.get("radius"))); } catch (Exception ignored) {}
                }
                List<String> commands = null;
                if (m.containsKey("commands")) {
                    Object obj = m.get("commands");
                    if (obj instanceof List) {
                        commands = new ArrayList<String>();
                        for (Object o : (List<?>) obj) {
                            commands.add(String.valueOf(o));
                        }
                    }
                }
                this.triggers.add(new Trigger(w, x, y, z, radius, commands));
            } catch (Exception ignore) {
                // skip malformed entry
            }
        }
    }

    
public void saveTriggersToConfig() {
        try {
            if (!isLocationMode()) return;
            java.util.List<java.util.Map<String,Object>> list = new java.util.ArrayList<>();
            for (com.minkang.tuto.model.Trigger t : this.triggers) {
                java.util.Map<String,Object> m = new java.util.LinkedHashMap<>();
                if (t.world != null) {
                    try {
                        m.put("world", t.world.getName());
                        m.put("world-uid", t.world.getUID().toString());
                    } catch (Throwable ignored) {}
                }
                m.put("x", t.x);
                m.put("y", t.y);
                m.put("z", t.z);
                if (t.radius > 0.0) m.put("radius", t.radius);
                if (t.commands != null && !t.commands.isEmpty()) m.put("commands", t.commands);
                list.add(m);
            }
            // Write to separate triggers.yml
            java.io.File dataFolder = getDataFolder();
            if (!dataFolder.exists()) dataFolder.mkdirs();
            java.io.File tf = new java.io.File(dataFolder, "triggers.yml");
            org.bukkit.configuration.file.YamlConfiguration tcfg = new org.bukkit.configuration.file.YamlConfiguration();
            tcfg.set("triggers", list);
            tcfg.save(tf);

            // Mirror into config under finish.triggers for transparency (not authoritative)
            getConfig().set("finish.triggers", list);
            saveConfig();
        } catch (Throwable t) {
            getLogger().warning("[Tuto] Failed to save triggers: " + t.getMessage());
        }
}
 catch (Throwable ignored) {}
                }
                m.put("x", t.x);
                m.put("y", t.y);
                m.put("z", t.z);
                if (t.radius > 0.0) m.put("radius", t.radius);
                if (t.commands != null && !t.commands.isEmpty()) m.put("commands", t.commands);
                list.add(m);
            }
            getConfig().set("finish.triggers", list);
            saveConfig();
        } catch (Throwable t) {
            getLogger().warning("[Tuto] Failed to save triggers: " + t.getMessage());
        }
    }
}
