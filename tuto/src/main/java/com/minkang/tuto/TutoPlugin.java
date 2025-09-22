package com.minkang.tuto;

import com.minkang.tuto.cmd.TutoCommand;
import com.minkang.tuto.listener.FirstJoinListener;
import com.minkang.tuto.listener.FinishTriggerListener;
import com.minkang.tuto.store.DataStore;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class TutoPlugin extends JavaPlugin {
    private static TutoPlugin inst;
    private DataStore data;
    public static TutoPlugin get(){ return inst; }
    public DataStore data(){ return data; }

    @Override public void onEnable(){
        inst = this;
        saveDefaultConfig();
        data = new DataStore(this);
        data.load();
        Bukkit.getPluginManager().registerEvents(new FirstJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new FinishTriggerListener(this), this);
        getCommand("tuto").setExecutor(new TutoCommand(this));
        getCommand("tuto").setTabCompleter(new TutoCommand(this));
        getLogger().info("[Tuto] enabled");
    }
    @Override public void onDisable(){ if (data!=null) data.save(); }
    public void reloadAll(){ reloadConfig(); data.load(); }
    public boolean isTutorialEnabled(){ return getConfig().getBoolean("tutorial.enabled", true); }
    public String tutorialWarp(){ return getConfig().getString("tutorial.warp","tutorial"); }
}
