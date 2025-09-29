package com.minkang.tuto.store;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DataStore {
    private final File file;
    private YamlConfiguration yml;
    private final Set<UUID> finished = new HashSet<>();
    private final java.util.Map<UUID, Long> cooldowns = new java.util.HashMap<>();

    public DataStore(File file) {
        this.file = file;
    }

    public void load() {
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        this.yml = YamlConfiguration.loadConfiguration(file);
        this.finished.clear();
        for (String s : this.yml.getStringList("finished")) {
            try { finished.add(UUID.fromString(s)); } catch (Exception ignored) {}
        }
    }

    public void save() {
        if (this.yml == null) this.yml = new YamlConfiguration();
        java.util.List<String> list = new java.util.ArrayList<>();
        for (UUID u : finished) list.add(u.toString());
        this.yml.set("finished", list);
        try { this.yml.save(file); } catch (IOException ignored) {}
    }

    public boolean isFinished(UUID uuid) { return finished.contains(uuid); }
    public void setFinished(UUID uuid) { finished.add(uuid); }

    public boolean onCooldown(UUID uuid, int seconds) {
        if (seconds <= 0) return false;
        long now = System.currentTimeMillis();
        Long until = cooldowns.get(uuid);
        return until != null && until > now;
    }

    public void setCooldown(UUID uuid, int seconds) {
        if (seconds <= 0) { cooldowns.remove(uuid); return; }
        cooldowns.put(uuid, System.currentTimeMillis() + seconds * 1000L);
    }
}