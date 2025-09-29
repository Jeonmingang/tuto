package com.minkang.tutorial.store;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataStore {

    private final File file;
    private YamlConfiguration yml;
    private final Set<UUID> finished = new HashSet<>();

    public DataStore(File dataFolder) {
        this.file = new File(dataFolder, "data.yml");
    }

    public void load() {
        try {
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            if (!file.exists()) file.createNewFile();
            yml = YamlConfiguration.loadConfiguration(file);
            finished.clear();
            for (String s : yml.getStringList("finished")) {
                try { finished.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
            }
        } catch (IOException ignored) {}
    }

    public void save() {
        if (yml == null) yml = new YamlConfiguration();
        List<String> list = new ArrayList<>();
        for (UUID u : finished) list.add(u.toString());
        yml.set("finished", list);
        try { yml.save(file); } catch (IOException ignored) {}
    }

    public boolean isFinished(UUID u) { return finished.contains(u); }
    public void setFinished(UUID u) { finished.add(u); }
    public void unsetFinished(UUID u) { finished.remove(u); }
}