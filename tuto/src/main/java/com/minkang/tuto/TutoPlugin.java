package com.minkang.tuto;

import com.minkang.tuto.cmd.TutoCommand;
import com.minkang.tuto.listener.FirstJoinListener;
import com.minkang.tuto.listener.FinishTriggerListener;
import com.minkang.tuto.model.Trigger;
import com.minkang.tuto.store.DataStore;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TutoPlugin extends JavaPlugin {
    private static TutoPlugin inst;
    private DataStore data;
    private List<Trigger> triggers = new ArrayList<Trigger>();

    public static TutoPlugin get(){ return inst; }
    public DataStore data(){ return data; }
    public List<Trigger> triggers(){ return triggers; }

    @Override public void onEnable(){
        inst = this;
        saveDefaultConfig();
        data = new DataStore(this);
        data.load();
        loadTriggersFromConfig();
        Bukkit.getPluginManager().registerEvents(new FirstJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new FinishTriggerListener(this), this);
        TutoCommand handler = new TutoCommand(this);
        getCommand("tuto").setExecutor(handler);
        getCommand("tuto").setTabCompleter(handler);
        getLogger().info("[Tuto] enabled (Java 8, 1.16.5)");
    }
    @Override public void onDisable(){ if (data!=null) data.save(); }

    public void reloadAll(){
        reloadConfig();
        data.load();
        loadTriggersFromConfig();
    }

    public boolean isTutorialEnabled(){ return getConfig().getBoolean("tutorial.enabled", true); }
    public String tutorialWarp(){ return getConfig().getString("tutorial.warp","tutorial"); }

    public boolean isLocationMode(){
        String mode = getConfig().getString("finish.mode","block");
        return "location".equalsIgnoreCase(mode);
    }

    @SuppressWarnings("unchecked")
    private void loadTriggersFromConfig(){
        this.triggers.clear();
        if (!isLocationMode()) return;
        java.util.List<Map<?,?>> list = getConfig().getMapList("finish.triggers");
        for (Map<?,?> m : list){
            try {
                String worldName = String.valueOf(m.get("world"));
                UUID wid = null;
                try {
                    Object wu = m.get("world-uid");
                    if (wu != null) wid = UUID.fromString(String.valueOf(wu));
                } catch (Exception ignored){}
                World w = (wid != null ? Bukkit.getWorld(wid) : Bukkit.getWorld(worldName));
                int x = Integer.parseInt(String.valueOf(m.get("x")));
                int y = Integer.parseInt(String.valueOf(m.get("y")));
                int z = Integer.parseInt(String.valueOf(m.get("z")));
                double radius = 0.0;
                Object r = m.get("radius");
                if (r instanceof Number) radius = ((Number) r).doubleValue();
                java.util.List<String> commands = null;
                Object c = m.get("commands");
                if (c instanceof java.util.List){
                    commands = new java.util.ArrayList<String>();
                    for (Object o : (java.util.List<Object>) c) commands.add(String.valueOf(o));
                }
                this.triggers.add(new Trigger(w, x, y, z, radius, commands));
            } catch (Exception ignored){}
        }
    }
}
