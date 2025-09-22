package com.minkang.tuto.model;

import org.bukkit.Location;
import org.bukkit.World;

public class Trigger {
    public final World world;
    public final int x, y, z;
    public final double radius;
    public final java.util.List<String> commands;

    public Trigger(World world, int x, int y, int z, double radius, java.util.List<String> commands){
        this.world = world; this.x=x; this.y=y; this.z=z; this.radius=radius; this.commands=commands;
    }
    public boolean matches(Location loc){
        if (loc==null || world==null) return false;
        if (!world.equals(loc.getWorld())) return false;
        if (radius <= 0.0){
            return loc.getBlockX()==x && loc.getBlockY()==y && loc.getBlockZ()==z;
        } else {
            double cx = x + 0.5, cy = y + 0.0, cz = z + 0.5;
            return loc.distanceSquared(new Location(world, cx, cy, cz)) <= radius*radius;
        }
    }
}
