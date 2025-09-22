package com.minkang.tuto.model;

import org.bukkit.World;

import java.util.List;

public class Trigger {
    public final World world;
    public final int x, y, z;
    public final double radius;
    public final java.util.List<String> commands; // nullable -> use global

    public Trigger(World world, int x, int y, int z, double radius, java.util.List<String> commands){
        this.world = world;
        this.x = x; this.y = y; this.z = z;
        this.radius = radius;
        this.commands = commands;
    }

    public boolean matches(org.bukkit.Location loc){
        if (loc==null || world==null) return false;
        if (!world.equals(loc.getWorld())) return false;
        if (radius <= 0.0){
            // exact feet-block match
            return loc.getBlockX()==x && loc.getBlockY()==y && loc.getBlockZ()==z;
        } else {
            // spherical distance, center to block center
            double cx = x + 0.5;
            double cy = y + 0.0;
            double cz = z + 0.5;
            return loc.distanceSquared(new org.bukkit.Location(world, cx, cy, cz)) <= radius*radius;
        }
    }
}
