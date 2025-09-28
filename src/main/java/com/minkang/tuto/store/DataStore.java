package com.minkang.tuto.store;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DataStore {
    private final Plugin plugin;
    private File file;
    private FileConfiguration conf;
    private final Set<UUID> completed = new HashSet<UUID>();

    public DataStore(Plugin plugin){ this.plugin = plugin; }

    public void load(){
        try{
            if (file == null) {
                file = new File(plugin.getDataFolder(), "data.yml");
            }
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            if (!file.exists()) {
                conf = new YamlConfiguration();
                conf.set("completed", new java.util.ArrayList<String>());
                conf.save(file);
            }
            conf = YamlConfiguration.loadConfiguration(file);
            completed.clear();
            for (String s : conf.getStringList("completed")){
                try { completed.add(UUID.fromString(s)); } catch (Exception ignore) {}
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    public void save(){
        if (conf == null) conf = new YamlConfiguration();
        java.util.List<String> list = new java.util.ArrayList<String>();
        for (UUID u : completed) list.add(u.toString());
        conf.set("completed", list);
        try { conf.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public boolean isCompleted(UUID u){ return completed.contains(u); }
    public void setCompleted(UUID u, boolean done){ if (done) completed.add(u); else completed.remove(u); save(); }
    public void reset(UUID u){ completed.remove(u); save(); }
}
