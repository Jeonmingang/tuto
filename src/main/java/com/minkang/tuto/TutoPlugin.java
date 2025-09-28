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
    public void loadTriggersFromConfig() {
        this.triggers.clear();
        if (!isLocationMode()) return;
        List<Map<String, Object>> list = (List<Map<String, Object>>) getConfig().getList("finish.triggers");
        if (list == null) return;
        for (Map<String, Object> m : list) {
            try {
                World w = null;
                if (m.containsKey("world-uid")) {
                    try {
                        UUID uid = UUID.fromString(String.valueOf(m.get("world-uid")));
                        w = Bukkit.getWorld(uid);
                    } catch (Exception ignored) {}
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
            List<Map<String,Object>> list = new ArrayList<Map<String,Object>>();
            for (Trigger t : this.triggers) {
                Map<String,Object> m = new LinkedHashMap<String,Object>();
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
            getConfig().set("finish.triggers", list);
            saveConfig();
        } catch (Throwable t) {
            getLogger().warning("[Tuto] Failed to save triggers: " + t.getMessage());
        }
    }
}
