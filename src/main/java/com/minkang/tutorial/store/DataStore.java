package com.minkang.tutorial.store;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Persistent storage for tutorial state.
 *
 * Data layout (tutorial-data.yml):
 * completed: [uuid, uuid, ...]
 * stages:
 *   <uuid>: <int>   # last reached stage number (1-based)
 * modes:
 *   <uuid>: <String>  # tutorial path id (full / quick / system ...)
 */
public class DataStore {

    private final File file;
    private FileConfiguration data;

    private final Set<UUID> completed = new HashSet<>();
    private final Map<UUID, Integer> stages = new HashMap<>();
    private final Map<UUID, String> modes = new HashMap<>();

    public DataStore(File file) {
        this.file = file;
        reload();
    }

    public void reload() {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException ignored) {
            }
        }
        data = YamlConfiguration.loadConfiguration(file);
        completed.clear();
        stages.clear();
        modes.clear();

        // completed
        for (String s : data.getStringList("completed")) {
            try {
                completed.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
            }
        }

        // stages
        if (data.isConfigurationSection("stages")) {
            for (String key : data.getConfigurationSection("stages").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    int stage = data.getInt("stages." + key, 0);
                    if (stage > 0) {
                        stages.put(uuid, stage);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        // modes
        if (data.isConfigurationSection("modes")) {
            for (String key : data.getConfigurationSection("modes").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    String mode = data.getString("modes." + key, null);
                    if (mode != null && !mode.isEmpty()) {
                        modes.put(uuid, mode);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public void save() {
        // completed
        List<String> list = new ArrayList<>();
        for (UUID u : completed) list.add(u.toString());
        data.set("completed", list);

        // stages
        Map<String, Integer> stagesOut = new HashMap<>();
        for (Map.Entry<UUID, Integer> e : stages.entrySet()) {
            stagesOut.put(e.getKey().toString(), e.getValue());
        }
        data.set("stages", stagesOut);

        // modes
        Map<String, String> modesOut = new HashMap<>();
        for (Map.Entry<UUID, String> e : modes.entrySet()) {
            modesOut.put(e.getKey().toString(), e.getValue());
        }
        data.set("modes", modesOut);

        try {
            data.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- completed ---

    public boolean isCompleted(UUID uuid) {
        return completed.contains(uuid);
    }

    public void setCompleted(UUID uuid, boolean value) {
        if (value) completed.add(uuid); else completed.remove(uuid);
        save();
    }

    public void clearAllCompleted() {
        completed.clear();
        save();
    }

    public Set<UUID> getAllCompleted() {
        return new HashSet<>(completed);
    }

    // --- stage ---

    /** Returns the last reached stage number (1-based). 0 if never started. */
    public int getStage(UUID uuid) {
        Integer i = stages.get(uuid);
        return i == null ? 0 : i;
    }

    public void setStage(UUID uuid, int stage) {
        if (stage <= 0) {
            stages.remove(uuid);
        } else {
            stages.put(uuid, stage);
        }
        save();
    }

    public void clearAllStages() {
        stages.clear();
        save();
    }

    // --- mode ---

    public String getMode(UUID uuid) {
        return modes.get(uuid);
    }

    public void setMode(UUID uuid, String mode) {
        if (mode == null || mode.isEmpty()) {
            modes.remove(uuid);
        } else {
            modes.put(uuid, mode.toLowerCase(Locale.ROOT));
        }
        save();
    }

    public void clearAllModes() {
        modes.clear();
        save();
    }

    // --- global ---

    public void clearAll() {
        completed.clear();
        stages.clear();
        modes.clear();
        save();
    }
}
