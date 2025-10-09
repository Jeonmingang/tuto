package com.minkang.tutorial.store;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DataStore {
    private final File file;
    private FileConfiguration data;
    private final Set<UUID> completed = new HashSet<>();

    public DataStore(File folder) {
        if (!folder.exists()) folder.mkdirs();
        this.file = new File(folder, "data.yml");
        reload();
    }

    public void reload() {
        data = YamlConfiguration.loadConfiguration(file);
        completed.clear();
        for (String s : data.getStringList("completed")) {
            try { completed.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
    }

    public void save() {
        try {
            Set<String> out = new HashSet<>();
            for (UUID u : completed) out.add(u.toString());
            data.set("completed", out.toArray(new String[0]));
            data.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isCompleted(UUID uuid) { return completed.contains(uuid); }
    public void setCompleted(UUID uuid, boolean value) {
        if (value) completed.add(uuid); else completed.remove(uuid);
        save();
    }
    public void clearAll() {
        completed.clear();
        save();
    }
    public Set<UUID> getAllCompleted() { return new HashSet<>(completed); }
}
